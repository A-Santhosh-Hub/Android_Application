package com.example.callmind.features.calls

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.callmind.data.local.entities.CallLogDisplayItem
import com.example.callmind.data.local.entities.CallRecordEntity
import com.example.callmind.data.local.entities.CallType
import com.example.callmind.data.local.entities.GroupedCallLog
import com.example.callmind.data.system.CallLogProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class CallViewModel @Inject constructor(
    private val callLogProvider: CallLogProvider
) : ViewModel() {

    private val _allCallLogs = MutableStateFlow<List<CallRecordEntity>>(emptyList())
    
    private val _filteredCallLogs = MutableStateFlow<List<CallLogDisplayItem>>(emptyList())
    val callLogs: StateFlow<List<CallLogDisplayItem>> = _filteredCallLogs

    private val _currentFilter = MutableStateFlow<CallFilter>(CallFilter.All)
    val currentFilter: StateFlow<CallFilter> = _currentFilter

    fun loadCallLogs() {
        viewModelScope.launch {
            val logs = callLogProvider.getCallLogs()
            _allCallLogs.value = logs
            applyFilter(_currentFilter.value)
        }
    }

    fun setFilter(filter: CallFilter) {
        _currentFilter.value = filter
        applyFilter(filter)
    }

    private fun applyFilter(filter: CallFilter) {
        val baseLogs = when (filter) {
            CallFilter.All -> _allCallLogs.value
            CallFilter.Incoming -> _allCallLogs.value.filter { it.type == CallType.INCOMING }
            CallFilter.Outgoing -> _allCallLogs.value.filter { it.type == CallType.OUTGOING }
            CallFilter.Missed -> _allCallLogs.value.filter { it.type == CallType.MISSED }
        }
        _filteredCallLogs.value = groupCalls(baseLogs)
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

    private fun isSameDay(t1: Long, t2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = t1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = t2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}

sealed class CallFilter {
    object All : CallFilter()
    object Incoming : CallFilter()
    object Outgoing : CallFilter()
    object Missed : CallFilter()
}
