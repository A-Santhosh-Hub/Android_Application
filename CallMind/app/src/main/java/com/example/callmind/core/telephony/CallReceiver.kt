package com.example.callmind.core.telephony

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log

class CallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            val phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
            
            Log.d("CallReceiver", "Phone state changed: $state, number: $phoneNumber")
            
            if (state == TelephonyManager.EXTRA_STATE_IDLE) {
                // Call ended, start the PostCallService
                val serviceIntent = Intent(context, PostCallService::class.java).apply {
                    putExtra("EXTRA_PHONE_NUMBER", phoneNumber)
                }
                context.startService(serviceIntent)
            }
        }
    }
}
