package com.example.callmind.data.system

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CallLog
import androidx.core.content.ContextCompat
import com.example.callmind.data.local.entities.CallRecordEntity
import com.example.callmind.data.local.entities.CallType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

import android.util.Log

@Singleton
class CallLogProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun getCallLogs(): List<CallRecordEntity> = withContext(Dispatchers.IO) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            Log.w("CallLogProvider", "Permission READ_CALL_LOG not granted")
            return@withContext emptyList()
        }

        val callLogs = mutableListOf<CallRecordEntity>()
        try {
            val cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(
                    CallLog.Calls.NUMBER,
                    CallLog.Calls.CACHED_NAME,
                    CallLog.Calls.DURATION,
                    CallLog.Calls.DATE,
                    CallLog.Calls.TYPE
                ),
                null,
                null,
                CallLog.Calls.DATE + " DESC"
            )

            cursor?.use {
                val numberIndex = it.getColumnIndex(CallLog.Calls.NUMBER)
                val nameIndex = it.getColumnIndex(CallLog.Calls.CACHED_NAME)
                val durationIndex = it.getColumnIndex(CallLog.Calls.DURATION)
                val dateIndex = it.getColumnIndex(CallLog.Calls.DATE)
                val typeIndex = it.getColumnIndex(CallLog.Calls.TYPE)

                Log.d("CallLogProvider", "Found ${it.count} call log entries")

                while (it.moveToNext()) {
                    val number = it.getString(numberIndex)
                    val name = it.getString(nameIndex)
                    val duration = it.getLong(durationIndex)
                    val date = it.getLong(dateIndex)
                    val type = it.getInt(typeIndex)

                    val callType = when (type) {
                        CallLog.Calls.INCOMING_TYPE -> CallType.INCOMING
                        CallLog.Calls.OUTGOING_TYPE -> CallType.OUTGOING
                        CallLog.Calls.MISSED_TYPE -> CallType.MISSED
                        else -> CallType.INCOMING
                    }

                    callLogs.add(
                        CallRecordEntity(
                            phoneNumber = number ?: "Unknown",
                            contactName = name,
                            durationSeconds = duration,
                            timestamp = date,
                            type = callType
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("CallLogProvider", "Error querying call logs", e)
        }
        callLogs
    }
}
