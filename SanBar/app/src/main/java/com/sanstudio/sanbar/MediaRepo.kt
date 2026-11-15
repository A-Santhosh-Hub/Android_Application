package com.sanstudio.sanbar

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.SystemClock
import android.view.KeyEvent
import androidx.core.app.NotificationManagerCompat

/** Reads "what is playing" from any music app through Android's MediaSession system. */
object MediaRepo {

    data class NowPlaying(
        val title: String,
        val subtitle: String,
        val art: Bitmap?,
        val playing: Boolean,
        val positionMs: Long,
        val durationMs: Long,
        val packageName: String,
        /** Most vivid colour of the cover, for "Album colour" mode (null without a cover). */
        val artColor: Int?,
    )

    private const val ART_SIZE_PX = 192
    private var artCacheKey: String? = null
    private var artCache: Bitmap? = null
    private var artColorCache: Int? = null

    fun listenerComponent(ctx: Context) = ComponentName(ctx, MediaListenerService::class.java)

    fun hasAccess(ctx: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName)

    /** The session to show: the one playing now, otherwise the most recent one. */
    fun activeController(ctx: Context): MediaController? {
        if (!hasAccess(ctx)) return null
        val msm = ctx.getSystemService(MediaSessionManager::class.java) ?: return null
        val sessions = try {
            msm.getActiveSessions(listenerComponent(ctx))
        } catch (e: SecurityException) {
            return null
        }
        return sessions.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: sessions.firstOrNull()
    }

    fun now(ctx: Context): NowPlaying? {
        val c = activeController(ctx) ?: return null
        val md = c.metadata ?: return null
        val state = c.playbackState
        val title = md.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: md.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: return null
        val artist = md.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: md.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
            ?: md.getString(MediaMetadata.METADATA_KEY_ALBUM)
        val appName = appLabel(ctx, c.packageName)
        val subtitle = listOfNotNull(artist?.takeIf { it.isNotBlank() }, appName).joinToString(" · ")

        val playing = state?.state == PlaybackState.STATE_PLAYING
        val duration = md.getLong(MediaMetadata.METADATA_KEY_DURATION).coerceAtLeast(0)
        var position = state?.position ?: 0L
        if (playing && state != null && state.lastPositionUpdateTime > 0) {
            position += ((SystemClock.elapsedRealtime() - state.lastPositionUpdateTime) * state.playbackSpeed).toLong()
        }
        if (duration > 0) position = position.coerceIn(0, duration)

        val art = art(c.packageName + "|" + title, md)
        return NowPlaying(
            title = title,
            subtitle = subtitle,
            art = art,
            playing = playing,
            positionMs = position.coerceAtLeast(0),
            durationMs = duration,
            packageName = c.packageName,
            artColor = if (art != null) artColorCache else null,
        )
    }

    /** Previous / play-pause / next. Falls back to a media key if music access isn't granted. */
    fun control(ctx: Context, action: String) {
        val c = activeController(ctx)
        if (c != null) {
            val t = c.transportControls
            when (action) {
                SanBarWidget.ACTION_PREV -> t.skipToPrevious()
                SanBarWidget.ACTION_NEXT -> t.skipToNext()
                SanBarWidget.ACTION_TOGGLE ->
                    if (c.playbackState?.state == PlaybackState.STATE_PLAYING) t.pause() else t.play()
            }
            return
        }
        val key = when (action) {
            SanBarWidget.ACTION_PREV -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
            SanBarWidget.ACTION_NEXT -> KeyEvent.KEYCODE_MEDIA_NEXT
            else -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
        }
        val am = ctx.getSystemService(AudioManager::class.java) ?: return
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, key))
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, key))
    }

    private fun appLabel(ctx: Context, pkg: String): String? = try {
        val pm = ctx.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    } catch (e: Exception) {
        null
    }

    private fun art(key: String, md: MediaMetadata): Bitmap? {
        if (key == artCacheKey) return artCache
        val src = md.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: md.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: md.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        val out = src?.let { roundedCenterCrop(it, ART_SIZE_PX, ART_SIZE_PX * 0.18f) }
        // Only cache once a picture has arrived: many apps send the title first, the cover later.
        if (out != null) {
            artCacheKey = key
            artCache = out
            artColorCache = vividColor(out)
        }
        return out
    }

    /** Hue histogram weighted by saturation × brightness; returns a bright version of the winner. */
    private fun vividColor(bmp: Bitmap): Int? = try {
        val small = Bitmap.createScaledBitmap(bmp, 24, 24, true)
        val weight = FloatArray(24)
        val hsv = FloatArray(3)
        for (y in 0 until 24) for (x in 0 until 24) {
            android.graphics.Color.colorToHSV(small.getPixel(x, y), hsv)
            if (hsv[1] < 0.25f || hsv[2] < 0.25f) continue
            weight[(hsv[0] / 15f).toInt().coerceIn(0, 23)] += hsv[1] * hsv[2]
        }
        val best = weight.indices.maxByOrNull { weight[it] }
        if (best == null || weight[best] < 1f) null
        else android.graphics.Color.HSVToColor(floatArrayOf(best * 15f + 7.5f, 0.70f, 1f))
    } catch (e: Exception) {
        null
    }

    private fun roundedCenterCrop(source: Bitmap, size: Int, radius: Float): Bitmap? = try {
        val src = if (source.config == Bitmap.Config.HARDWARE) source.copy(Bitmap.Config.ARGB_8888, false) else source
        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val scale = maxOf(size.toFloat() / src.width, size.toFloat() / src.height)
        val matrix = Matrix().apply {
            setScale(scale, scale)
            postTranslate((size - src.width * scale) / 2f, (size - src.height * scale) / 2f)
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            shader = BitmapShader(src, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply { setLocalMatrix(matrix) }
        }
        Canvas(out).drawRoundRect(RectF(0f, 0f, size.toFloat(), size.toFloat()), radius, radius, paint)
        out
    } catch (e: Exception) {
        null
    }
}
