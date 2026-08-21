package com.example.callmind.data.repository

import android.content.Context
import androidx.work.*
import com.example.callmind.data.local.daos.ReminderDao
import com.example.callmind.data.local.entities.ReminderEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val reminderDao: ReminderDao
) {
    fun getAllReminders(): Flow<List<ReminderEntity>> = reminderDao.getAllReminders()

    suspend fun insertReminder(reminder: ReminderEntity) {
        reminderDao.insertReminder(reminder)
        scheduleReminder(reminder)
    }

    suspend fun deleteReminder(reminder: ReminderEntity) {
        reminderDao.deleteReminder(reminder)
        WorkManager.getInstance(context).cancelUniqueWork("reminder_${reminder.id}")
    }

    private fun scheduleReminder(reminder: ReminderEntity) {
        val delay = reminder.scheduledTime - System.currentTimeMillis()
        if (delay <= 0) return

        val data = Data.Builder()
            .putString("EXTRA_TITLE", reminder.title)
            .putString("EXTRA_PHONE_NUMBER", reminder.phoneNumber)
            .build()

        val reminderRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "reminder_${reminder.id}",
            ExistingWorkPolicy.REPLACE,
            reminderRequest
        )
    }
}

class ReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        // Implementation for showing notification when reminder triggers
        return Result.success()
    }
}
