package com.sanden

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SanDenApp : Application() {

    companion object {
        const val CHANNEL_FILE_OPS = "sanden_file_ops"
        const val CHANNEL_FILE_OPS_NAME = "File Operations"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()

        // TODO: Initialize biometric / PIN lock manager here.
        //  When implemented, this should check if authentication is configured
        //  and prompt for biometric or PIN verification before granting access
        //  to vault contents. Consider using BiometricPrompt API for seamless
        //  integration with system-level biometrics (fingerprint, face, iris).
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val fileOpsChannel = NotificationChannel(
                CHANNEL_FILE_OPS,
                CHANNEL_FILE_OPS_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for file import, export, move, and delete operations"
                setShowBadge(false)
                enableVibration(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(fileOpsChannel)
        }
    }
}
