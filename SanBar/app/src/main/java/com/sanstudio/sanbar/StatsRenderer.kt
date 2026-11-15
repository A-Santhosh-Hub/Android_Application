package com.sanstudio.sanbar

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.PowerManager
import android.view.View
import android.widget.RemoteViews
import com.sanstudio.sanbar.WidgetRenderer.tint

/** Draws the SanStats widget: CPU / RAM / TEMP rings. */
object StatsRenderer {

    private var lastSignature: String? = null

    private const val GREEN = 0xFF4ADE80.toInt()
    private const val YELLOW = 0xFFFFD43B.toInt()
    private const val ORANGE = 0xFFFF922B.toInt()
    private const val RED = 0xFFFF3B3B.toInt()

    fun widgetIds(ctx: Context): IntArray =
        AppWidgetManager.getInstance(ctx).getAppWidgetIds(ComponentName(ctx, StatsWidget::class.java))

    private fun layoutFor(awm: AppWidgetManager, id: Int): Int {
        val maxHeight = awm.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)
        return if (maxHeight >= 130) R.layout.stats_tall else R.layout.stats_slim
    }

    @Synchronized
    fun render(ctx: Context, force: Boolean = false) {
        val ids = widgetIds(ctx)
        if (ids.isEmpty()) return
        val awm = AppWidgetManager.getInstance(ctx)
        val s = StatsRepo.read(ctx)
        val accent = WidgetRenderer.currentAccent(ctx)
        val layouts = ids.map { layoutFor(awm, it) }

        val signature = listOf(
            s.cpuPct, s.ramPct, (s.tempC * 10).toInt(), s.thermalStatus, s.heatPct,
            "%.1f".format(s.cpuGhz), Prefs.signature(ctx), accent, layouts,
        ).joinToString("|")
        if (!force && signature == lastSignature) return
        lastSignature = signature

        ids.forEachIndexed { i, id -> awm.updateAppWidget(id, build(ctx, layouts[i], s, accent)) }
    }

    private fun build(ctx: Context, layout: Int, s: StatsRepo.Stats, accent: Int): RemoteViews {
        val rv = RemoteViews(ctx.packageName, layout)
        WidgetRenderer.applyLook(ctx, rv, accent)
        if (Prefs.statsTransparent(ctx)) {
            // No box and no RGB border: only the rings and text float on the wallpaper.
            rv.tint(R.id.content, "setBackgroundTintList", Color.TRANSPARENT)
            WidgetRenderer.FX_VIEW_IDS.forEach { rv.setViewVisibility(it, View.GONE) }
        }

        // CPU
        val cpu = s.cpuPct.coerceAtLeast(0)
        rv.setProgressBar(R.id.ring_cpu, 100, cpu, false)
        rv.tint(R.id.ring_cpu, "setProgressTintList", if (cpu >= 85) ORANGE else accent)
        rv.setTextViewText(R.id.val_cpu, if (s.cpuPct < 0) "--" else "$cpu%")
        rv.setTextViewText(R.id.det_cpu, if (s.cpuGhz > 0) "%.1f GHz".format(s.cpuGhz) else "")

        // RAM
        rv.setProgressBar(R.id.ring_ram, 100, s.ramPct, false)
        rv.tint(R.id.ring_ram, "setProgressTintList", if (s.ramPct >= 90) ORANGE else accent)
        rv.setTextViewText(R.id.val_ram, "${s.ramPct}%")
        rv.setTextViewText(R.id.det_ram, "%.1f/%.1f GB".format(s.ramUsedGb, s.ramTotalGb))

        // TEMP: battery °C; ring fill = thermal headroom (how close to slowing down from heat)
        val heatColor = when (s.thermalStatus) {
            PowerManager.THERMAL_STATUS_NONE, PowerManager.THERMAL_STATUS_LIGHT -> GREEN
            PowerManager.THERMAL_STATUS_MODERATE -> YELLOW
            PowerManager.THERMAL_STATUS_SEVERE -> ORANGE
            else -> RED
        }
        val fill = if (s.heatPct >= 0) s.heatPct else ((s.tempC - 20f) / 30f * 100f).toInt().coerceIn(0, 100)
        rv.setProgressBar(R.id.ring_temp, 100, fill, false)
        rv.tint(R.id.ring_temp, "setProgressTintList", heatColor)
        rv.setTextViewText(R.id.val_temp, "%.0f°".format(s.tempC))
        rv.setTextColor(R.id.val_temp, if (s.thermalStatus >= PowerManager.THERMAL_STATUS_SEVERE) heatColor else 0xFFFFFFFF.toInt())
        rv.setTextViewText(R.id.det_temp, statusName(s.thermalStatus))

        val open = PendingIntent.getActivity(
            ctx, 20, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        rv.setOnClickPendingIntent(R.id.stats_area, open)
        return rv
    }

    private fun statusName(status: Int) = when (status) {
        PowerManager.THERMAL_STATUS_NONE -> "Cool"
        PowerManager.THERMAL_STATUS_LIGHT -> "Warm"
        PowerManager.THERMAL_STATUS_MODERATE -> "Hot"
        PowerManager.THERMAL_STATUS_SEVERE -> "Very hot"
        else -> "Critical"
    }
}
