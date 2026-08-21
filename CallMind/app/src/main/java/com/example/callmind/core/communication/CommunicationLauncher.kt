package com.example.callmind.core.communication

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast

class CommunicationLauncher(private val context: Context) {

    fun launchWhatsApp(phoneNumber: String) {
        try {
            val url = "https://api.whatsapp.com/send?phone=${normalizeNumber(phoneNumber)}"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchTelegram(phoneNumber: String) {
        try {
            val url = "tg://msg?number=${normalizeNumber(phoneNumber)}"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Telegram not installed", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchDialer(phoneNumber: String) {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$phoneNumber")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun launchSMS(phoneNumber: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$phoneNumber")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun launchGPay(phoneNumber: String) {
        try {
            val uri = Uri.Builder()
                .scheme("upi")
                .authority("pay")
                .appendQueryParameter("pa", "$phoneNumber@okaxis")
                .appendQueryParameter("pn", "Unknown")
                .appendQueryParameter("mc", "")
                .appendQueryParameter("tid", "")
                .appendQueryParameter("tr", "")
                .appendQueryParameter("tn", "Sent from CallMind")
                .appendQueryParameter("am", "")
                .appendQueryParameter("cu", "INR")
                .build()
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = uri
                `package` = "com.google.android.apps.nbu.paisa.user"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Google Pay not installed", Toast.LENGTH_SHORT).show()
        }
    }

    fun viewContact(phoneNumber: String) {
        try {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = uri
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not view contact", Toast.LENGTH_SHORT).show()
        }
    }

    fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Phone Number", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied to clipboard: $text", Toast.LENGTH_SHORT).show()
    }

    private fun normalizeNumber(number: String): String {
        return number.filter { it.isDigit() }
    }
}
