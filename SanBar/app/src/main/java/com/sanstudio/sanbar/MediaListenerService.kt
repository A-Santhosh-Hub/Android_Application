package com.sanstudio.sanbar

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.service.notification.NotificationListenerService

/**
 * Always-on engine behind the widget. Android keeps a granted notification listener alive,
 * so this watches music, phone battery and Bluetooth and redraws the widget when they change.
 */
class MediaListenerService : NotificationListenerService() {

    private val handler = Handler(Looper.getMainLooper())
    private var sessionManager: MediaSessionManager? = null
    private val watched = mutableListOf<Pair<MediaController, MediaController.Callback>>()
    private var receiverRegistered = false

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { list ->
        watch(list.orEmpty())
        refresh()
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            if (intent.action == BatteryRepo.ACTION_BT_BATTERY_LEVEL_CHANGED) {
                @Suppress("DEPRECATION")
                val dev = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                val level = intent.getIntExtra(BatteryRepo.EXTRA_BT_BATTERY_LEVEL, -1)
                if (dev != null) BatteryRepo.reportedLevels[dev.address] = level
            }
            refresh()
        }
    }

    private val render = Runnable {
        WidgetRenderer.render(this)
        StatsRenderer.render(this)
        updateSpectrum()
        scheduleTick()
        scheduleStatsTick()
    }

    /** SanStats refresh: every 4 s, only while the screen is on and a stats widget exists. */
    private val statsTick = Runnable {
        StatsRenderer.render(this)
        scheduleStatsTick()
    }

    private fun scheduleStatsTick() {
        if (handler.hasCallbacks(statsTick)) return
        val interactive = getSystemService(PowerManager::class.java)?.isInteractive == true
        if (interactive && StatsRenderer.widgetIds(this).isNotEmpty()) handler.postDelayed(statsTick, 4000)
    }

    /** Customize changes (colour, RGB effect, visualizer) redraw the widget right away. */
    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> refresh() }

    // ---- Real spectrum ----
    private var spectrum: SpectrumEngine? = null

    private fun updateSpectrum() {
        val want = Prefs.viz(this) == Prefs.VIZ_REAL &&
            WidgetRenderer.lastPlaying &&
            getSystemService(PowerManager::class.java)?.isInteractive == true &&
            checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED &&
            WidgetRenderer.widgetIds(this).isNotEmpty()
        if (want && spectrum == null) {
            val engine = SpectrumEngine { levels, live -> onSpectrum(levels, live) }
            if (engine.start()) spectrum = engine
        } else if (!want && spectrum != null) {
            stopSpectrum()
        }
    }

    private fun stopSpectrum() {
        spectrum?.stop()
        spectrum = null
        if (WidgetRenderer.spectrumLive) {
            WidgetRenderer.spectrumLive = false
            WidgetRenderer.render(this, force = true)
        }
    }

    private fun onSpectrum(levels: IntArray?, live: Boolean) {
        if (live != WidgetRenderer.spectrumLive) {
            WidgetRenderer.spectrumLive = live
            WidgetRenderer.render(this, force = true) // switch bars between real and animated
        }
        if (live && levels != null) WidgetRenderer.pushSpectrum(this, levels)
    }

    override fun onListenerConnected() {
        val msm = getSystemService(MediaSessionManager::class.java)
        sessionManager = msm
        val me = MediaRepo.listenerComponent(this)
        try {
            msm?.addOnActiveSessionsChangedListener(sessionsListener, me, handler)
            watch(msm?.getActiveSessions(me).orEmpty())
        } catch (_: SecurityException) {
        }

        if (!receiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_BATTERY_CHANGED)
                addAction(Intent.ACTION_POWER_CONNECTED)
                addAction(Intent.ACTION_POWER_DISCONNECTED)
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
                addAction(BatteryRepo.ACTION_BT_BATTERY_LEVEL_CHANGED)
                addAction("android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED")
                addAction("android.media.ACTION_SCO_AUDIO_STATE_UPDATED")
            }
            registerReceiver(receiver, filter, RECEIVER_EXPORTED)
            receiverRegistered = true
            Prefs.register(this, prefsListener)
        }
        refresh()
    }

    override fun onListenerDisconnected() {
        cleanup()
        // Ask Android to bring us back (e.g. after the app was updated).
        SanBarWidget.wakeListener(this)
    }

    override fun onDestroy() {
        cleanup()
        super.onDestroy()
    }

    private fun cleanup() {
        handler.removeCallbacksAndMessages(null)
        stopSpectrum()
        Prefs.unregister(this, prefsListener)
        try {
            sessionManager?.removeOnActiveSessionsChangedListener(sessionsListener)
        } catch (_: Exception) {
        }
        watch(emptyList())
        if (receiverRegistered) {
            try {
                unregisterReceiver(receiver)
            } catch (_: Exception) {
            }
            receiverRegistered = false
        }
    }

    private fun watch(controllers: List<MediaController>) {
        watched.forEach { (c, cb) -> c.unregisterCallback(cb) }
        watched.clear()
        controllers.forEach { c ->
            val cb = object : MediaController.Callback() {
                override fun onPlaybackStateChanged(state: PlaybackState?) = refresh()
                override fun onMetadataChanged(metadata: MediaMetadata?) = refresh()
                override fun onSessionDestroyed() = refresh()
            }
            c.registerCallback(cb, handler)
            watched += c to cb
        }
    }

    /** Coalesce bursts of events (battery broadcasts can arrive many times a second). */
    private fun refresh() {
        handler.removeCallbacks(render)
        handler.postDelayed(render, 150)
    }

    /** While music plays and the screen is on, move the progress bar every few seconds. */
    private fun scheduleTick() {
        handler.removeCallbacks(tick)
        val interactive = getSystemService(PowerManager::class.java)?.isInteractive == true
        if (WidgetRenderer.lastPlaying && interactive) handler.postDelayed(tick, 3000)
    }

    private val tick = Runnable { refresh() }
}
