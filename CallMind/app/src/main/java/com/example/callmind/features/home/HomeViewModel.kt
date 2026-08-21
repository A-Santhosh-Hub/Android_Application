package com.example.callmind.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.callmind.data.local.entities.CallLogDisplayItem
import com.example.callmind.data.local.entities.CallRecordEntity
import com.example.callmind.data.local.entities.GroupedCallLog
import com.example.callmind.data.local.entities.ReminderEntity
import com.example.callmind.data.repository.NoteRepository
import com.example.callmind.data.repository.ReminderRepository
import com.example.callmind.data.system.CallLogProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val callLogProvider: CallLogProvider,
    private val noteRepository: NoteRepository,
    private val reminderRepository: ReminderRepository
) : ViewModel() {

    private val _homeState = MutableStateFlow(HomeState())
    val homeState: StateFlow<HomeState> = _homeState

    // Exposed flow for notification badges
    val activeReminderCount: Flow<Int> = reminderRepository.getAllReminders()
        .map { reminders -> reminders.count { !it.isCompleted && it.scheduledTime > System.currentTimeMillis() } }

    fun loadHomeData() {
        viewModelScope.launch {
            // Combine data from repositories and system providers
            val calls = callLogProvider.getCallLogs()
            val todayCalls = calls.filter { isToday(it.timestamp) }
            
            noteRepository.getAllNotes().collectLatest { notes ->
                val todayNotes = notes.filter { isToday(it.createdAt) }
                
                reminderRepository.getAllReminders().collectLatest { reminders ->
                    val upcoming = reminders.filter { 
                        !it.isCompleted && it.scheduledTime > System.currentTimeMillis() 
                    }.sortedBy { it.scheduledTime }

                    _homeState.value = HomeState(
                        recentCalls = groupCalls(calls).take(5),
                        upcomingReminders = upcoming.take(2),
                        callCount = todayCalls.size,
                        noteCount = todayNotes.size,
                        scheduledCount = upcoming.size,
                        totalTalkTime = calculateTalkTime(todayCalls)
                    )
                }
            }
        }
    }

    private fun groupCalls(logs: List<CallRecordEntity>): List<CallLogDisplayItem> {
        if (logs.isEmpty()) return emptyList()

        val result = mutableListOf<CallLogDisplayItem>()
        val calendar = Calendar.getInstance()
        val today = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = calendar.timeInMillis

        val sdfHeader = java.text.SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())

        fun getHeader(timestamp: Long): String {
            return when {
                isSameDay(timestamp, today) -> "Today"
                isSameDay(timestamp, yesterday) -> "Yesterday"
                else -> sdfHeader.format(Date(timestamp))
            }
        }

        var currentHeader = ""
        var currentGroup = mutableListOf<CallRecordEntity>()

        for (log in logs) {
            val logHeader = getHeader(log.timestamp)
            
            if (currentHeader != logHeader) {
                if (currentGroup.isNotEmpty()) {
                    result.add(CallLogDisplayItem.CallGroup(GroupedCallLog(currentGroup.first(), currentGroup.toList())))
                    currentGroup.clear()
                }
                result.add(CallLogDisplayItem.Header(logHeader))
                currentHeader = logHeader
            }

            if (currentGroup.isEmpty()) {
                currentGroup.add(log)
            } else {
                val lastInGroup = currentGroup.last()
                if (lastInGroup.phoneNumber == log.phoneNumber) {
                    currentGroup.add(log)
                } else {
                    result.add(CallLogDisplayItem.CallGroup(GroupedCallLog(currentGroup.first(), currentGroup.toList())))
                    currentGroup = mutableListOf(log)
                }
            }
        }

        if (currentGroup.isNotEmpty()) {
            result.add(CallLogDisplayItem.CallGroup(GroupedCallLog(currentGroup.first(), currentGroup.toList())))
        }

        return result
    }

    private fun isToday(timestamp: Long): Boolean {
        val cal1 = Calendar.getInstance()
        val cal2 = Calendar.getInstance().apply { timeInMillis = timestamp }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isSameDay(t1: Long, t2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = t1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = t2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun calculateTalkTime(calls: List<CallRecordEntity>): String {
        val totalSeconds = calls.sumOf { it.durationSeconds }
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        
        return buildString {
            if (hours > 0) append("${hours}h ")
            if (minutes > 0 || hours > 0) append("${minutes}m ")
            append("${seconds}s")
        }.trim()
    }
}

data class HomeState(
    val recentCalls: List<CallLogDisplayItem> = emptyList(),
    val upcomingReminders: List<ReminderEntity> = emptyList(),
    val callCount: Int = 0,
    val noteCount: Int = 0,
    val scheduledCount: Int = 0,
    val totalTalkTime: String = "0s"
)
