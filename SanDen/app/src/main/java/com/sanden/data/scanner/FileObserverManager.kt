package com.sanden.data.scanner

import android.os.Build
import android.os.FileObserver
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Describes a file-system change event detected by [FileObserverManager].
 */
data class FileChangeEvent(
    val eventType: EventType,
    val path: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    enum class EventType {
        CREATED,
        DELETED,
        MODIFIED,
        MOVED_FROM,
        MOVED_TO
    }
}

/**
 * Manages recursive [FileObserver] instances to watch a directory tree for changes.
 *
 * On API 29+ a single [FileObserver] created with a [File] is used (supports recursive
 * observation). On older APIs, one observer is created per sub-directory.
 *
 * Events are debounced (300 ms) using coroutines and emitted as a [SharedFlow].
 */
@Singleton
class FileObserverManager @Inject constructor() {

    companion object {
        private const val TAG = "FileObserverManager"
        private const val DEBOUNCE_MS = 300L

        /** Mask for the events we care about. */
        private const val EVENT_MASK =
            FileObserver.CREATE or
            FileObserver.DELETE or
            FileObserver.MODIFY or
            FileObserver.MOVED_FROM or
            FileObserver.MOVED_TO or
            FileObserver.CLOSE_WRITE

        /** Soft limit to prevent excessive observer counts on pre-29 devices. */
        private const val MAX_OBSERVERS = 1_000
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _events = MutableSharedFlow<FileChangeEvent>(
        replay = 0,
        extraBufferCapacity = 64
    )

    /** Public stream of debounced file-change events. */
    val events: SharedFlow<FileChangeEvent> = _events.asSharedFlow()

    // Active observers
    private val observers = mutableListOf<FileObserver>()
    private var watching = false
    private var rootPath: String? = null

    // Debounce state: path → pending emit job
    private val pendingEvents = mutableMapOf<String, Job>()

    // ── Public API ──────────────────────────────────────────────────────

    /**
     * Starts watching the given [path] (and its subtree) for file system events.
     * If already watching, the previous session is stopped first.
     */
    fun startWatching(path: String) {
        if (watching && rootPath == path) return
        if (watching) stopWatching()

        rootPath = path
        watching = true

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startRecursiveObserver(path)
            } else {
                startLegacyObservers(File(path))
            }
            Log.i(TAG, "Started watching: $path (${observers.size} observer(s))")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start watching: $path", e)
        }
    }

    /**
     * Stops all active observers and clears state.
     */
    fun stopWatching() {
        watching = false
        for (observer in observers) {
            try {
                observer.stopWatching()
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping observer", e)
            }
        }
        observers.clear()
        pendingEvents.values.forEach { it.cancel() }
        pendingEvents.clear()
        rootPath = null
        Log.i(TAG, "Stopped watching")
    }

    /** Whether the manager is currently watching a directory. */
    fun isWatching(): Boolean = watching

    // ── API 29+ recursive observer ──────────────────────────────────────

    private fun startRecursiveObserver(path: String) {
        val observer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            object : FileObserver(File(path), EVENT_MASK) {
                override fun onEvent(event: Int, relativePath: String?) {
                    handleRawEvent(event, relativePath, path)
                }
            }
        } else {
            // Won't reach here, but keep the compiler happy
            return
        }
        observer.startWatching()
        observers.add(observer)
    }

    // ── Pre-API-29 per-directory observers ──────────────────────────────

    private fun startLegacyObservers(root: File) {
        registerLegacyObserver(root)
        walkDirectories(root)
    }

    private fun walkDirectories(dir: File) {
        if (observers.size >= MAX_OBSERVERS) {
            Log.w(TAG, "Max observer count ($MAX_OBSERVERS) reached; skipping deeper directories")
            return
        }

        val children = dir.listFiles() ?: return
        for (child in children) {
            if (child.isDirectory) {
                registerLegacyObserver(child)
                walkDirectories(child)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun registerLegacyObserver(dir: File) {
        val path = dir.absolutePath
        val observer = object : FileObserver(path, EVENT_MASK) {
            override fun onEvent(event: Int, relativePath: String?) {
                handleRawEvent(event, relativePath, path)
            }
        }
        observer.startWatching()
        observers.add(observer)
    }

    // ── Event handling & debouncing ─────────────────────────────────────

    private fun handleRawEvent(event: Int, relativePath: String?, basePath: String) {
        if (!watching) return
        val fileName = relativePath ?: return
        if (fileName == ".nomedia") return // Ignore .nomedia changes

        val fullPath = "$basePath/$fileName"
        val masked = event and 0xFFF // Strip IN_ISDIR etc.

        val eventType = when (masked) {
            FileObserver.CREATE -> FileChangeEvent.EventType.CREATED
            FileObserver.DELETE -> FileChangeEvent.EventType.DELETED
            FileObserver.MODIFY,
            FileObserver.CLOSE_WRITE -> FileChangeEvent.EventType.MODIFIED
            FileObserver.MOVED_FROM -> FileChangeEvent.EventType.MOVED_FROM
            FileObserver.MOVED_TO -> FileChangeEvent.EventType.MOVED_TO
            else -> return
        }

        debounceAndEmit(fullPath, eventType)

        // For legacy mode: if a new directory is created, add an observer for it
        if (masked == FileObserver.CREATE && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val file = File(fullPath)
            if (file.isDirectory && observers.size < MAX_OBSERVERS) {
                registerLegacyObserver(file)
            }
        }
    }

    /**
     * Debounces events on the same path: if a new event arrives within [DEBOUNCE_MS]
     * of a pending one for the same path, the previous job is cancelled and the
     * timer resets. Only the last event within the window is emitted.
     */
    private fun debounceAndEmit(path: String, eventType: FileChangeEvent.EventType) {
        synchronized(pendingEvents) {
            pendingEvents[path]?.cancel()
            pendingEvents[path] = scope.launch {
                delay(DEBOUNCE_MS)
                val event = FileChangeEvent(
                    eventType = eventType,
                    path = path
                )
                _events.emit(event)
                synchronized(pendingEvents) {
                    pendingEvents.remove(path)
                }
            }
        }
    }
}
