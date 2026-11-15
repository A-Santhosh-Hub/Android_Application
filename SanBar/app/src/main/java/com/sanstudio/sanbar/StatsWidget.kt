package com.sanstudio.sanbar

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle

/** SanStats: CPU / RAM / TEMP. Kept live every few seconds by MediaListenerService while the screen is on. */
class StatsWidget : AppWidgetProvider() {

    override fun onUpdate(ctx: Context, awm: AppWidgetManager, ids: IntArray) {
        StatsRenderer.render(ctx, force = true)
        SanBarWidget.wakeListener(ctx)
    }

    override fun onAppWidgetOptionsChanged(ctx: Context, awm: AppWidgetManager, id: Int, options: Bundle) {
        StatsRenderer.render(ctx, force = true)
    }

    override fun onEnabled(ctx: Context) = SanBarWidget.wakeListener(ctx)
}
