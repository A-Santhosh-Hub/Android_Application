package com.example.data

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.*

class TaskReminderService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    
    private val triggeredKeys = mutableSetOf<String>()
    private var wakeLock: android.os.PowerManager.WakeLock? = null
    
    companion object {
        const val ONGOING_CHANNEL_ID = "task_ongoing_channel"
        const val ALERT_CHANNEL_ID = "task_alert_channel"
        const val ONGOING_NOTIFICATION_ID = 9182
        
        fun startService(context: Context) {
            val intent = Intent(context, TaskReminderService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        
        // Acquire partial WakeLock to keep the background polling active when screen is off
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            wakeLock = powerManager.newWakeLock(
                android.os.PowerManager.PARTIAL_WAKE_LOCK,
                "SanSyncTaskReminderService::WakeLock"
            ).apply {
                acquire()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // Setup initial foreground notification required on registration
        val initialNotif = createOngoingNotification(
            "San Sync",
            "Routine Monitor active. Scanning next task..."
        )
        startForeground(ONGOING_NOTIFICATION_ID, initialNotif)
        
        // Run persistent polling loop
        serviceScope.launch {
            while (isActive) {
                try {
                    checkAndNotifyTasks()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(5000) // Poll database every 5 seconds for tasks or alarms
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Run an instant polling pass if requested/triggered on task update
        serviceScope.launch {
            try {
                checkAndNotifyTasks()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return START_STICKY
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ongoingChannel = NotificationChannel(
                ONGOING_CHANNEL_ID,
                "San Sync task list drawer (Persistent)",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows your next upcoming task in the notification panel."
                setShowBadge(false)
            }
            
            val alertChannel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "San Sync alarms & reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Loud tones and alerts when a task reaches its scheduled time."
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                enableLights(true)
            }
            
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(ongoingChannel)
            manager.createNotificationChannel(alertChannel)
        }
    }

    private fun createOngoingNotification(title: String, content: String): Notification {
        val clickIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            clickIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        return NotificationCompat.Builder(this, ONGOING_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private suspend fun checkAndNotifyTasks() {
        val db = AppDatabase.getDatabase(this)
        val noteDao = db.noteDao()
        
        // Fetch current active notes lists
        val allNotes = noteDao.getActiveNotes().firstOrNull() ?: emptyList()
        val taskNote = allNotes.find { it.title == "Global Tasks List" && it.isChecklist }
        
        if (taskNote == null) {
            updateOngoingNotification("San Sync", "No active daily routines found.")
            return
        }
        
        val rawItems = taskNote.getChecklistItems()
        
        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val currentDateStr = sdfDate.format(Date())
        
        // Auto-reset routine tasks if the day has changed
        var listModified = false
        val items = rawItems.map { item ->
            if (item.isRoutine && item.isChecked && item.lastCheckedDate.isNotBlank() && item.lastCheckedDate != currentDateStr) {
                listModified = true
                item.copy(isChecked = false, lastCheckedDate = "")
            } else {
                item
            }
        }
        
        if (listModified) {
            noteDao.insertNote(
                taskNote.copy(
                    content = ChecklistItem.listToJson(items),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
        
        val dayOfWeekFormat = SimpleDateFormat("EEE", Locale.US) // e.g. "Mon"
        val currentDayStr = dayOfWeekFormat.format(Date())
        
        // Filter out routine tasks that do NOT include today
        val filteredItems = items.filter { item ->
            if (item.isRoutine && item.routineDays.isNotBlank()) {
                item.routineDays.contains(currentDayStr)
            } else {
                true
            }
        }
        
        val uncheckedItems = filteredItems.filter { !it.isChecked && it.time.isNotBlank() }
        
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        val currentMinutes = currentHour * 60 + currentMinute
        
        // 1. Process and trigger alarms if matching time
        for (item in uncheckedItems) {
            val itemMinutes = timeToMinutes(item.time)
            if (itemMinutes != null) {
                val reminderMinutes = itemMinutes - item.reminderOffsetMinutes
                val key = "${item.id}_${item.time}_${item.reminderOffsetMinutes}_${currentDateStr}"
                
                // Allow error range of +/- 1 minute to prevent missing triggers
                if (currentMinutes == reminderMinutes && !triggeredKeys.contains(key)) {
                    triggeredKeys.add(key)
                    triggerAlarmNotification(item)
                }
            }
        }
        
        // 2. Identify and display upcoming chronological task
        if (uncheckedItems.isEmpty()) {
            updateOngoingNotification("San Sync: All Tasks Done! 🎉", "Great job! All scheduled tasks are complete.")
            return
        }
        
        // Separate tasks into those later today vs those scheduled earlier today (wrap around to tomorrow)
        val laterToday = mutableListOf<Pair<ChecklistItem, Int>>()
        val earlierToday = mutableListOf<Pair<ChecklistItem, Int>>()
        
        for (item in uncheckedItems) {
            val itemMinutes = timeToMinutes(item.time) ?: continue
            if (itemMinutes >= currentMinutes) {
                laterToday.add(item to itemMinutes)
            } else {
                earlierToday.add(item to itemMinutes)
            }
        }
        
        laterToday.sortBy { it.second }
        earlierToday.sortBy { it.second }
        
        val nextTaskPair = if (laterToday.isNotEmpty()) {
            laterToday.first()
        } else {
            earlierToday.first() // Wrap around scheduled times for tomorrow
        }
        
        val nextItem = nextTaskPair.first
        val minDiff = nextTaskPair.second - currentMinutes
        
        val relativeTimeStr = if (laterToday.isNotEmpty()) {
            if (minDiff == 0) "Starting Now!" else "In $minDiff mins"
        } else {
            "Tomorrow"
        }
        
        val taskTitle = "[Upcoming] ${nextItem.text}"
        val taskBody = "Scheduled at ${nextItem.time} (${nextItem.category}) • $relativeTimeStr"
        
        updateOngoingNotification(taskTitle, taskBody)
    }

    private fun triggerAlarmNotification(item: ChecklistItem) {
        val clickIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            item.id.hashCode(),
            clickIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        // Wake the screen up if it's off so the user sees the alarm immediately
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            if (!pm.isInteractive) {
                @Suppress("DEPRECATION")
                val screenLock = pm.newWakeLock(
                    android.os.PowerManager.SCREEN_BRIGHT_WAKE_LOCK or android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "SanSync::TaskReminderScreenWake"
                )
                screenLock.acquire(10000) // Wakes the screen for 10 seconds
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Play custom alarm audio via the shared manager using saved user choices
        try {
            val prefs = getSharedPreferences("SanSyncPrefs", Context.MODE_PRIVATE)
            val soundType = prefs.getString("soundType", "System Default") ?: "System Default"
            val soundDuration = prefs.getInt("soundDurationSeconds", 5)
            com.example.ui.AlarmSoundManager.playSound(this, soundType, soundDuration, serviceScope)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val alertNotification = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
            .setContentTitle("Task Reminder!")
            .setContentText("It's time for: ${item.text} (${item.time})")
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingIntent, true)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 250, 500, 250, 500))
            .setContentIntent(pendingIntent)
            .build()
            
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(item.id.hashCode(), alertNotification)
    }

    private fun updateOngoingNotification(title: String, content: String) {
        val notification = createOngoingNotification(title, content)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(ONGOING_NOTIFICATION_ID, notification)
    }

    private fun timeToMinutes(timeStr: String): Int? {
        if (timeStr.isBlank()) return null
        return try {
            val clean = timeStr.trim().uppercase()
            val ampm = if (clean.contains("PM")) "PM" else "AM"
            val parts = clean.replace("AM", "").replace("PM", "").trim().split(":")
            var hr = parts[0].toInt()
            val min = parts[1].toInt()
            if (ampm == "PM" && hr < 12) hr += 12
            if (ampm == "AM" && hr == 12) hr = 0
            hr * 60 + min
        } catch (e: Exception) {
            null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
