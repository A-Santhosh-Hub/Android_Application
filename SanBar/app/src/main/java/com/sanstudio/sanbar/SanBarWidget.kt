package com.sanstudio.sanbar

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService

class SanBarWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_PREV = "com.sanstudio.sanbar.PREV"
        const val ACTION_TOGGLE = "com.sanstudio.sanbar.TOGGLE"
        const val ACTION_NEXT = "com.sanstudio.sanbar.NEXT"

        /** Ask Android to (re)start the always-on listener, if music access is granted. */
        fun wakeListener(ctx: Context) {
            if (!MediaRepo.hasAccess(ctx)) return
            try {
                NotificationListenerService.requestRebind(MediaRepo.listenerComponent(ctx))
            } catch (_: Exception) {
            }
        }
    }

    override fun onUpdate(ctx: Context, awm: AppWidgetManager, ids: IntArray) {
        WidgetRenderer.render(ctx, force = true)
        wakeListener(ctx)
    }

    override fun onAppWidgetOptionsChanged(ctx: Context, awm: AppWidgetManager, id: Int, options: Bundle) {
        WidgetRenderer.render(ctx, force = true)
    }

    override fun onEnabled(ctx: Context) = wakeListener(ctx)

    override fun onReceive(ctx: Context, intent: Intent) {
        when (val action = intent.action) {
            ACTION_PREV, ACTION_TOGGLE, ACTION_NEXT -> {
                MediaRepo.control(ctx, action)
                // Give the music app a moment to react, then redraw (the listener also redraws).
                val pending = goAsync()
                Handler(Looper.getMainLooper()).postDelayed({
                    WidgetRenderer.render(ctx, force = true)
                    pending.finish()
                }, 350)
            }
            else -> super.onReceive(ctx, intent)
        }
    }
}
