package com.example.callmind.features.reminders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.callmind.data.local.entities.ReminderEntity
import com.example.callmind.data.local.entities.ReminderType
import com.example.callmind.ui.components.CallMindCard
import com.example.callmind.ui.theme.CallMindTheme
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RemindersScreen(viewModel: ReminderViewModel = hiltViewModel()) {
    val reminders by viewModel.allReminders.collectAsState(initial = emptyList())

    RemindersContent(
        reminders = reminders,
        onDeleteReminder = { viewModel.deleteReminder(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersContent(
    reminders: List<ReminderEntity>,
    onDeleteReminder: (ReminderEntity) -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Reminders", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            if (reminders.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No reminders scheduled.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(reminders) { reminder ->
                        ReminderItem(reminder, onDeleteReminder)
                    }
                }
            }
        }
    }
}

@Composable
fun ReminderItem(reminder: ReminderEntity, onDeleteReminder: (ReminderEntity) -> Unit) {
    val sdf = remember { SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()) }
    val dateString = remember(reminder.scheduledTime) { sdf.format(Date(reminder.scheduledTime)) }

    CallMindCard {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (reminder.type) {
                    ReminderType.CALL -> Icons.Default.Call
                    ReminderType.POPUP_NOTE -> Icons.Default.Edit
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.title,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (!reminder.description.isNullOrBlank()) {
                    Text(
                        text = reminder.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "$dateString ${if (reminder.phoneNumber != null) "• ${reminder.phoneNumber}" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            IconButton(onClick = { onDeleteReminder(reminder) }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RemindersScreenPreview() {
    CallMindTheme {
        RemindersContent(
            reminders = listOf(
                ReminderEntity(title = "Call Arun Kumar", scheduledTime = System.currentTimeMillis() + 3600000, phoneNumber = "+91 98765 43210"),
                ReminderEntity(title = "Project follow-up", scheduledTime = System.currentTimeMillis() + 86400000)
            ),
            onDeleteReminder = {}
        )
    }
}
