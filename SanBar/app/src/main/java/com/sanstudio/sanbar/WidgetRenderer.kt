package com.sanstudio.sanbar

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.SystemClock
import android.provider.Settings
import android.view.View
import android.widget.RemoteViews

/**
 * Builds and pushes the widget.
 *
 * Each widget gets ONE layout (slim or tall, by its current height) instead of Android 12's
 * size-mapped RemoteViews: REA showed RemoteViews.mergeRemoteViews() only merges plain action
 * lists, so the fast partial updates used by the real spectrum need a single layout per widget.
 *
 * All colour calls below (setProgressTintList, setIndeterminateTintList, setImageTintList,
 * setBackgroundTintList) were verified with REA to be @RemotableViewMethod on this phone.
 */
object WidgetRenderer {

    @Volatile
    var lastPlaying = false
        private set

    /** True while the real spectrum is delivering frames (set by MediaListenerService). */
    @Volatile
    var spectrumLive = false

    private var lastSignature: String? = null

    private const val TEXT = Color.WHITE
    private const val TEXT_DIM = 0xB3FFFFFF.toInt()
    private const val LOW = 0xFFFF6B6B.toInt()
    private const val CHARGE_NORMAL = 0xFFFFD43B.toInt() // yellow
    private const val CHARGE_FAST = 0xFFFF3B3B.toInt()   // red

    val VIZ_IDS = intArrayOf(
        R.id.viz_0, R.id.viz_1, R.id.viz_2, R.id.viz_3, R.id.viz_4, R.id.viz_5,
        R.id.viz_6, R.id.viz_7, R.id.viz_8, R.id.viz_9, R.id.viz_10, R.id.viz_11,
        R.id.viz_12, R.id.viz_13, R.id.viz_14, R.id.viz_15, R.id.viz_16, R.id.viz_17,
        R.id.viz_18, R.id.viz_19, R.id.viz_20, R.id.viz_21, R.id.viz_22, R.id.viz_23,
    )

    /** effect -> (normal speed view, fast view) */
    private val FX_VIEWS = mapOf(
        Prefs.FX_SPIN to (R.id.fx_spin to R.id.fx_spin_fast),
        Prefs.FX_CYCLE to (R.id.fx_cycle to R.id.fx_cycle_fast),
        Prefs.FX_BREATHE to (R.id.fx_breathe to R.id.fx_breathe_fast),
    )

    val FX_VIEW_IDS: List<Int> = FX_VIEWS.values.flatMap { listOf(it.first, it.second) }

    fun widgetIds(ctx: Context): IntArray =
        AppWidgetManager.getInstance(ctx).getAppWidgetIds(ComponentName(ctx, SanBarWidget::class.java))

    private fun layoutFor(awm: AppWidgetManager, id: Int): Int {
        val maxHeight = awm.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)
        return if (maxHeight >= 130) R.layout.widget_tall else R.layout.widget_slim
    }

    @Synchronized
    fun render(ctx: Context, force: Boolean = false) {
        val awm = AppWidgetManager.getInstance(ctx)
        val ids = widgetIds(ctx)
        if (ids.isEmpty()) return

        val app = ctx.applicationContext
        BatteryRepo.init(app) { render(app, force = true) }

        val media = MediaRepo.now(ctx)
        val phone = BatteryRepo.phone(ctx)
        val bt = BatteryRepo.headphones(ctx)
        lastPlaying = media?.playing == true
        val accent = if (Prefs.albumColor(ctx)) media?.artColor ?: Prefs.accent(ctx) else Prefs.accent(ctx)
        lastAccent = accent
        val layouts = ids.map { layoutFor(awm, it) }

        // Skip identical redraws (battery broadcasts repeat often with the same numbers).
        val signature = listOf(
            media?.title, media?.subtitle, media?.playing, media?.positionMs?.div(1000),
            media?.durationMs, media?.art?.generationId, phone.level, phone.charging, phone.fast, bt,
            MediaRepo.hasAccess(ctx), BatteryRepo.hasBluetoothPermission(ctx),
            Prefs.signature(ctx), accent, spectrumLive, layouts,
        ).joinToString("|")
        if (!force && signature == lastSignature) return
        lastSignature = signature

        ids.forEachIndexed { i, id ->
            awm.updateAppWidget(id, build(ctx, layouts[i], media, phone, bt, accent))
        }
    }

    /** Real spectrum frame: only the 24 bar heights travel to the home screen. */
    fun pushSpectrum(ctx: Context, levels: IntArray) {
        val awm = AppWidgetManager.getInstance(ctx)
        for (id in widgetIds(ctx)) {
            val rv = RemoteViews(ctx.packageName, layoutFor(awm, id))
            for (i in VIZ_IDS.indices) rv.setProgressBar(VIZ_IDS[i], 100, levels.getOrElse(i) { 0 }, false)
            awm.partiallyUpdateAppWidget(id, rv)
        }
    }

    fun RemoteViews.tint(id: Int, method: String, color: Int) =
        setColorStateList(id, method, ColorStateList.valueOf(color))

    /** Glass darkness + RGB border effect, shared by SanBar and SanStats. */
    fun applyLook(ctx: Context, rv: RemoteViews, accent: Int) {
        rv.tint(R.id.content, "setBackgroundTintList", Color.argb(Prefs.glassAlpha(ctx), 0x0C, 0x14, 0x10))
        FX_VIEWS.values.forEach { (normal, fast) ->
            rv.setViewVisibility(normal, View.GONE)
            rv.setViewVisibility(fast, View.GONE)
        }
        FX_VIEWS[Prefs.fx(ctx)]?.let { (normal, fast) ->
            rv.setViewVisibility(if (Prefs.fxFast(ctx)) fast else normal, View.VISIBLE)
        }
        // Breathing glows in the accent colour.
        rv.tint(R.id.fx_breathe, "setIndeterminateTintList", accent)
        rv.tint(R.id.fx_breathe_fast, "setIndeterminateTintList", accent)
    }

    /** Accent in use (album colour if that mode is on and a cover is known). */
    fun currentAccent(ctx: Context): Int = lastAccent ?: Prefs.accent(ctx)

    @Volatile
    private var lastAccent: Int? = null

    private fun build(
        ctx: Context,
        layout: Int,
        media: MediaRepo.NowPlaying?,
        phone: BatteryRepo.Phone,
        bt: BatteryRepo.Headphones?,
        accent: Int,
    ): RemoteViews {
        val rv = RemoteViews(ctx.packageName, layout)

        // ---- Look: glass, accent, RGB effect ----
        applyLook(ctx, rv, accent)
        if (Prefs.barTransparent(ctx)) {
            // Clean mode: no box and no RGB border, only cover, text, buttons and rings.
            rv.tint(R.id.content, "setBackgroundTintList", Color.TRANSPARENT)
            FX_VIEW_IDS.forEach { rv.setViewVisibility(it, View.GONE) }
        }
        rv.tint(R.id.btn_play, "setBackgroundTintList", accent)
        rv.tint(R.id.progress, "setProgressTintList", accent)
        rv.setTextColor(R.id.bt_name, accent)

        // ---- Music ----
        if (media == null) {
            rv.setTextViewText(R.id.title, "Nothing playing")
            rv.setTextViewText(
                R.id.subtitle,
                if (MediaRepo.hasAccess(ctx)) "Tap ▶ to resume your music" else "Open SanBar → allow music access",
            )
            rv.setImageViewResource(R.id.cover, R.drawable.cover_placeholder)
            rv.setProgressBar(R.id.progress, 1000, 0, false)
            rv.setViewVisibility(R.id.elapsed, View.INVISIBLE)
            rv.setTextViewText(R.id.duration, "")
        } else {
            rv.setTextViewText(R.id.title, media.title)
            rv.setTextViewText(R.id.subtitle, media.subtitle)
            if (media.art != null) rv.setImageViewBitmap(R.id.cover, media.art)
            else rv.setImageViewResource(R.id.cover, R.drawable.cover_placeholder)

            if (media.durationMs > 0) {
                val p = (media.positionMs * 1000 / media.durationMs).toInt()
                rv.setProgressBar(R.id.progress, 1000, p, false)
                rv.setTextViewText(R.id.duration, formatTime(media.durationMs))
            } else {
                rv.setProgressBar(R.id.progress, 1000, 0, false)
                rv.setTextViewText(R.id.duration, "")
            }
            // The chronometer ticks by itself on the home screen: no redraw needed every second.
            rv.setViewVisibility(R.id.elapsed, View.VISIBLE)
            rv.setChronometer(R.id.elapsed, SystemClock.elapsedRealtime() - media.positionMs, null, media.playing)
        }
        rv.setImageViewResource(R.id.btn_play, if (media?.playing == true) R.drawable.ic_pause else R.drawable.ic_play)

        rv.setOnClickPendingIntent(R.id.btn_prev, controlIntent(ctx, SanBarWidget.ACTION_PREV, 1))
        rv.setOnClickPendingIntent(R.id.btn_play, controlIntent(ctx, SanBarWidget.ACTION_TOGGLE, 2))
        rv.setOnClickPendingIntent(R.id.btn_next, controlIntent(ctx, SanBarWidget.ACTION_NEXT, 3))
        rv.setOnClickPendingIntent(R.id.music_area, openPlayingApp(ctx, media?.packageName))

        // ---- Visualizer ----
        val vizMode = Prefs.viz(ctx)
        val showViz = vizMode != Prefs.VIZ_OFF && media?.playing == true
        rv.setViewVisibility(R.id.viz_row, if (showViz) View.VISIBLE else View.GONE)
        if (showViz) {
            // Real bars when the spectrum is live, otherwise the self-animating dance bars.
            val animated = !(vizMode == Prefs.VIZ_REAL && spectrumLive)
            for (id in VIZ_IDS) {
                rv.tint(id, "setProgressTintList", accent)
                rv.tint(id, "setIndeterminateTintList", accent)
                rv.setProgressBar(id, 100, 0, animated)
            }
        }

        // ---- Phone battery + charging animation ----
        val chargeColor = if (phone.fast) CHARGE_FAST else CHARGE_NORMAL
        val ringColor = when {
            phone.charging -> chargeColor
            phone.level <= 15 -> LOW
            else -> accent
        }
        rv.setProgressBar(R.id.ring_phone, 100, phone.level, false)
        rv.tint(R.id.ring_phone, "setProgressTintList", ringColor)
        rv.setTextViewText(R.id.pct_phone, "${phone.level}%")
        rv.setTextColor(R.id.pct_phone, if (phone.charging) chargeColor else if (phone.level <= 15) LOW else TEXT)
        rv.setImageViewResource(R.id.icon_phone, if (phone.charging) R.drawable.ic_bolt else R.drawable.ic_phone)
        rv.tint(R.id.icon_phone, "setImageTintList", if (phone.charging) chargeColor else TEXT)
        // Spinning comet around the ring: slow + yellow for normal charge, fast + red for fast charge.
        rv.setViewVisibility(R.id.ring_charge, if (phone.charging && !phone.fast) View.VISIBLE else View.GONE)
        rv.setViewVisibility(R.id.ring_charge_fast, if (phone.charging && phone.fast) View.VISIBLE else View.GONE)
        rv.tint(R.id.ring_charge, "setIndeterminateTintList", CHARGE_NORMAL)
        rv.tint(R.id.ring_charge_fast, "setIndeterminateTintList", CHARGE_FAST)

        // ---- Headphones battery ----
        if (bt == null) {
            rv.setViewVisibility(R.id.bt_group, View.GONE)
            rv.setTextViewText(R.id.bt_name, "")
            rv.setViewVisibility(R.id.bt_name, View.GONE)
        } else {
            rv.setViewVisibility(R.id.bt_group, View.VISIBLE)
            rv.setViewVisibility(R.id.bt_name, View.VISIBLE)
            rv.setTextViewText(R.id.bt_name, bt.name)
            val known = bt.level in 0..100
            rv.setProgressBar(R.id.ring_bt, 100, if (known) bt.level else 0, false)
            rv.tint(R.id.ring_bt, "setProgressTintList", if (known && bt.level <= 15) LOW else accent)
            rv.setTextViewText(R.id.pct_bt, if (known) "${bt.level}%" else "--")
            rv.setTextColor(R.id.pct_bt, if (known && bt.level <= 15) LOW else if (known) TEXT else TEXT_DIM)
        }
        rv.setOnClickPendingIntent(R.id.battery_area, openBluetoothSettings(ctx))
        return rv
    }

    private fun controlIntent(ctx: Context, action: String, code: Int): PendingIntent {
        val i = Intent(ctx, SanBarWidget::class.java).setAction(action)
        return PendingIntent.getBroadcast(ctx, code, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun openPlayingApp(ctx: Context, pkg: String?): PendingIntent {
        val launch = pkg?.let { ctx.packageManager.getLaunchIntentForPackage(it) }
            ?: Intent(ctx, MainActivity::class.java)
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(ctx, 10, launch, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun openBluetoothSettings(ctx: Context): PendingIntent {
        val i = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(ctx, 11, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun formatTime(ms: Long): String {
        val s = ms / 1000
        return if (s >= 3600) "%d:%02d:%02d".format(s / 3600, (s % 3600) / 60, s % 60)
        else "%d:%02d".format(s / 60, s % 60)
    }
}
