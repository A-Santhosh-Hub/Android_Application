package com.sanden.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.sanden.R
import com.sanden.SanDenApp
import com.sanden.domain.model.FileOperation
import com.sanden.domain.repository.VaultRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class FileOperationService : Service() {

    companion object {
        private const val TAG = "FileOperationService"

        const val ACTION_IMPORT = "com.sanden.action.IMPORT"
        const val ACTION_EXPORT = "com.sanden.action.EXPORT"
        const val ACTION_DELETE = "com.sanden.action.DELETE"
        const val ACTION_MOVE = "com.sanden.action.MOVE"
        const val ACTION_CANCEL = "com.sanden.action.CANCEL"

        const val EXTRA_FILE_URIS = "extra_file_uris"
        const val EXTRA_DESTINATION_PATH = "extra_destination_path"
        const val EXTRA_FILE_IDS = "extra_file_ids"

        private const val NOTIFICATION_ID_PROGRESS = 1001
        private const val NOTIFICATION_ID_COMPLETE = 1002
        private const val NOTIFICATION_ID_ERROR = 1003

        private const val REQUEST_CODE_CANCEL = 2001

        fun createImportIntent(
            context: Context,
            fileUris: ArrayList<String>
        ): Intent {
            return Intent(context, FileOperationService::class.java).apply {
                action = ACTION_IMPORT
                putStringArrayListExtra(EXTRA_FILE_URIS, fileUris)
            }
        }

        fun createExportIntent(
            context: Context,
            fileIds: LongArray,
            destinationPath: String
        ): Intent {
            return Intent(context, FileOperationService::class.java).apply {
                action = ACTION_EXPORT
                putExtra(EXTRA_FILE_IDS, fileIds)
                putExtra(EXTRA_DESTINATION_PATH, destinationPath)
            }
        }

        fun createDeleteIntent(
            context: Context,
            fileIds: LongArray
        ): Intent {
            return Intent(context, FileOperationService::class.java).apply {
                action = ACTION_DELETE
                putExtra(EXTRA_FILE_IDS, fileIds)
            }
        }

        fun createMoveIntent(
            context: Context,
            fileIds: LongArray,
            destinationPath: String
        ): Intent {
            return Intent(context, FileOperationService::class.java).apply {
                action = ACTION_MOVE
                putExtra(EXTRA_FILE_IDS, fileIds)
                putExtra(EXTRA_DESTINATION_PATH, destinationPath)
            }
        }
    }

    @Inject
    lateinit var vaultRepository: VaultRepository

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(serviceJob + Dispatchers.IO)

    private var currentJob: Job? = null
    private var isCancelled = false

    private val notificationManager: NotificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "FileOperationService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CANCEL -> {
                Log.d(TAG, "Cancel requested")
                cancelCurrentOperation()
                return START_NOT_STICKY
            }
            ACTION_IMPORT -> {
                val uris = intent.getStringArrayListExtra(EXTRA_FILE_URIS) ?: emptyList()
                startForegroundWithNotification("Importing files...")
                handleImport(uris)
            }
            ACTION_EXPORT -> {
                val fileIds = intent.getLongArrayExtra(EXTRA_FILE_IDS) ?: longArrayOf()
                val destination = intent.getStringExtra(EXTRA_DESTINATION_PATH) ?: ""
                startForegroundWithNotification("Exporting files...")
                handleExport(fileIds, destination)
            }
            ACTION_DELETE -> {
                val fileIds = intent.getLongArrayExtra(EXTRA_FILE_IDS) ?: longArrayOf()
                startForegroundWithNotification("Deleting files...")
                handleDelete(fileIds)
            }
            ACTION_MOVE -> {
                val fileIds = intent.getLongArrayExtra(EXTRA_FILE_IDS) ?: longArrayOf()
                val destination = intent.getStringExtra(EXTRA_DESTINATION_PATH) ?: ""
                startForegroundWithNotification("Moving files...")
                handleMove(fileIds, destination)
            }
            else -> {
                Log.w(TAG, "Unknown action: ${intent?.action}")
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        Log.d(TAG, "FileOperationService destroyed")
    }

    // ── Foreground Notification ─────────────────────────────────────────

    private fun startForegroundWithNotification(title: String) {
        val notification = buildProgressNotification(title, 0, indeterminate = true)
        startForeground(NOTIFICATION_ID_PROGRESS, notification)
    }

    private fun buildProgressNotification(
        title: String,
        progress: Int,
        indeterminate: Boolean = false
    ): Notification {
        val cancelIntent = Intent(this, FileOperationService::class.java).apply {
            action = ACTION_CANCEL
        }
        val cancelPendingIntent = PendingIntent.getService(
            this,
            REQUEST_CODE_CANCEL,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, SanDenApp.CHANNEL_FILE_OPS)
            .setContentTitle(title)
            .setContentText(if (indeterminate) "Preparing..." else "$progress% complete")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setProgress(100, progress, indeterminate)
            .addAction(
                R.drawable.ic_launcher_foreground,
                getString(R.string.cancel),
                cancelPendingIntent
            )
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun updateProgress(title: String, progress: Int) {
        val notification = buildProgressNotification(title, progress)
        notificationManager.notify(NOTIFICATION_ID_PROGRESS, notification)
    }

    private fun showCompletionNotification(title: String, message: String) {
        val notification = NotificationCompat.Builder(this, SanDenApp.CHANNEL_FILE_OPS)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID_COMPLETE, notification)
    }

    private fun showErrorNotification(title: String, message: String) {
        val notification = NotificationCompat.Builder(this, SanDenApp.CHANNEL_FILE_OPS)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .build()

        notificationManager.notify(NOTIFICATION_ID_ERROR, notification)
    }

    // ── Operation Handlers ──────────────────────────────────────────────

    private fun handleImport(uris: List<String>) {
        currentJob = serviceScope.launch {
            isCancelled = false
            val total = uris.size
            var processed = 0
            var failed = 0

            try {
                for ((index, uriString) in uris.withIndex()) {
                    if (isCancelled) {
                        showCompletionNotification(
                            getString(R.string.import_cancelled),
                            getString(R.string.import_cancelled_detail, processed, total)
                        )
                        break
                    }

                    try {
                        ensureSufficientStorage()
                        vaultRepository.importFile(uriString)
                        processed++
                    } catch (e: LowStorageException) {
                        failed++
                        Log.e(TAG, "Low storage while importing: $uriString", e)
                        showErrorNotification(
                            getString(R.string.error_low_storage_title),
                            getString(R.string.error_low_storage_message)
                        )
                        break
                    } catch (e: SecurityException) {
                        failed++
                        Log.e(TAG, "Permission revoked for: $uriString", e)
                    } catch (e: FileInUseException) {
                        failed++
                        Log.e(TAG, "File in use: $uriString", e)
                    } catch (e: Exception) {
                        failed++
                        Log.e(TAG, "Failed to import: $uriString", e)
                    }

                    val progress = ((index + 1) * 100) / total
                    updateProgress(getString(R.string.importing_files), progress)
                }

                if (!isCancelled) {
                    showCompletionNotification(
                        getString(R.string.import_complete),
                        if (failed > 0) {
                            getString(R.string.import_complete_with_errors, processed, total, failed)
                        } else {
                            getString(R.string.import_complete_detail, processed)
                        }
                    )
                }
            } catch (e: CancellationException) {
                Log.d(TAG, "Import cancelled via coroutine cancellation")
            } finally {
                stopForegroundAndSelf()
            }
        }
    }

    private fun handleExport(fileIds: LongArray, destination: String) {
        currentJob = serviceScope.launch {
            isCancelled = false
            val total = fileIds.size
            var processed = 0
            var failed = 0

            try {
                for ((index, fileId) in fileIds.withIndex()) {
                    if (isCancelled) {
                        showCompletionNotification(
                            getString(R.string.export_cancelled),
                            getString(R.string.export_cancelled_detail, processed, total)
                        )
                        break
                    }

                    try {
                        ensureSufficientStorage()
                        vaultRepository.exportFile(fileId, destination)
                        processed++
                    } catch (e: LowStorageException) {
                        failed++
                        Log.e(TAG, "Low storage while exporting fileId=$fileId", e)
                        showErrorNotification(
                            getString(R.string.error_low_storage_title),
                            getString(R.string.error_low_storage_message)
                        )
                        break
                    } catch (e: SecurityException) {
                        failed++
                        Log.e(TAG, "Permission revoked during export fileId=$fileId", e)
                    } catch (e: Exception) {
                        failed++
                        Log.e(TAG, "Failed to export fileId=$fileId", e)
                    }

                    val progress = ((index + 1) * 100) / total
                    updateProgress(getString(R.string.exporting_files), progress)
                }

                if (!isCancelled) {
                    showCompletionNotification(
                        getString(R.string.export_complete),
                        if (failed > 0) {
                            getString(R.string.export_complete_with_errors, processed, total, failed)
                        } else {
                            getString(R.string.export_complete_detail, processed)
                        }
                    )
                }
            } catch (e: CancellationException) {
                Log.d(TAG, "Export cancelled via coroutine cancellation")
            } finally {
                stopForegroundAndSelf()
            }
        }
    }

    private fun handleDelete(fileIds: LongArray) {
        currentJob = serviceScope.launch {
            isCancelled = false
            val total = fileIds.size
            var processed = 0
            var failed = 0

            try {
                for ((index, fileId) in fileIds.withIndex()) {
                    if (isCancelled) {
                        showCompletionNotification(
                            getString(R.string.delete_cancelled),
                            getString(R.string.delete_cancelled_detail, processed, total)
                        )
                        break
                    }

                    try {
                        vaultRepository.deleteFile(fileId)
                        processed++
                    } catch (e: FileInUseException) {
                        failed++
                        Log.e(TAG, "File in use, cannot delete fileId=$fileId", e)
                    } catch (e: Exception) {
                        failed++
                        Log.e(TAG, "Failed to delete fileId=$fileId", e)
                    }

                    val progress = ((index + 1) * 100) / total
                    updateProgress(getString(R.string.deleting_files), progress)
                }

                if (!isCancelled) {
                    showCompletionNotification(
                        getString(R.string.delete_complete),
                        if (failed > 0) {
                            getString(R.string.delete_complete_with_errors, processed, total, failed)
                        } else {
                            getString(R.string.delete_complete_detail, processed)
                        }
                    )
                }
            } catch (e: CancellationException) {
                Log.d(TAG, "Delete cancelled via coroutine cancellation")
            } finally {
                stopForegroundAndSelf()
            }
        }
    }

    private fun handleMove(fileIds: LongArray, destination: String) {
        currentJob = serviceScope.launch {
            isCancelled = false
            val total = fileIds.size
            var processed = 0
            var failed = 0

            try {
                for ((index, fileId) in fileIds.withIndex()) {
                    if (isCancelled) {
                        showCompletionNotification(
                            getString(R.string.move_cancelled),
                            getString(R.string.move_cancelled_detail, processed, total)
                        )
                        break
                    }

                    try {
                        ensureSufficientStorage()
                        vaultRepository.moveFile(fileId, destination)
                        processed++
                    } catch (e: LowStorageException) {
                        failed++
                        Log.e(TAG, "Low storage while moving fileId=$fileId", e)
                        showErrorNotification(
                            getString(R.string.error_low_storage_title),
                            getString(R.string.error_low_storage_message)
                        )
                        break
                    } catch (e: FileInUseException) {
                        failed++
                        Log.e(TAG, "File in use, cannot move fileId=$fileId", e)
                    } catch (e: Exception) {
                        failed++
                        Log.e(TAG, "Failed to move fileId=$fileId", e)
                    }

                    val progress = ((index + 1) * 100) / total
                    updateProgress(getString(R.string.moving_files), progress)
                }

                if (!isCancelled) {
                    showCompletionNotification(
                        getString(R.string.move_complete),
                        if (failed > 0) {
                            getString(R.string.move_complete_with_errors, processed, total, failed)
                        } else {
                            getString(R.string.move_complete_detail, processed)
                        }
                    )
                }
            } catch (e: CancellationException) {
                Log.d(TAG, "Move cancelled via coroutine cancellation")
            } finally {
                stopForegroundAndSelf()
            }
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private fun cancelCurrentOperation() {
        isCancelled = true
        currentJob?.cancel()
    }

    private fun stopForegroundAndSelf() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    @Throws(LowStorageException::class)
    private fun ensureSufficientStorage() {
        val stat = android.os.StatFs(filesDir.absolutePath)
        val availableBytes = stat.availableBytes
        val minimumRequired = 50L * 1024 * 1024 // 50 MB minimum

        if (availableBytes < minimumRequired) {
            throw LowStorageException(
                "Insufficient storage: ${availableBytes / (1024 * 1024)} MB available, " +
                        "minimum ${minimumRequired / (1024 * 1024)} MB required"
            )
        }
    }

    // ── Custom Exceptions ───────────────────────────────────────────────

    class LowStorageException(message: String) : Exception(message)
    class FileInUseException(message: String) : Exception(message)
}
