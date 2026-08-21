package com.example.callmind.core.telephony

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.callmind.R
import com.example.callmind.features.postcall.PostCallActivity

class PostCallService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val phoneNumber = intent?.getStringExtra("EXTRA_PHONE_NUMBER")
        showPostCallNotification(phoneNumber)
        return START_NOT_STICKY
    }

    private fun showPostCallNotification(phoneNumber: String?) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "post_call_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Post-Call Experience",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val postCallIntent = Intent(this, PostCallActivity::class.java).apply {
            putExtra("EXTRA_PHONE_NUMBER", phoneNumber)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            postCallIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Placeholder
            .setContentTitle("Call Ended")
            .setContentText("Tap to organize next steps for ${phoneNumber ?: "unknown number"}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(1, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
