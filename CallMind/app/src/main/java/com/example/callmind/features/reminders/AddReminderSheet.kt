package com.example.callmind.features.reminders

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.callmind.data.local.entities.ReminderEntity
import com.example.callmind.data.local.entities.ReminderType
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReminderSheet(
    phoneNumber: String,
    contactName: String? = null,
    onReminderSaved: () -> Unit,
    onDismiss: () -> Unit,
    viewModel: ReminderViewModel,
    initialType: ReminderType = ReminderType.CALL
) {
    val displayName = contactName ?: phoneNumber
    var type by remember { mutableStateOf(initialType) }
    var title by remember { 
        mutableStateOf(if (type == ReminderType.CALL) "Call $displayName" else "Note for $displayName") 
    }
    var description by remember { mutableStateOf("") }
    var hoursDelay by remember { mutableStateOf(1) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Text(
                text = if (type == ReminderType.CALL) "Schedule Call" else "Schedule Pop-up Note",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // Type Selector
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = type == ReminderType.CALL,
                    onClick = { 
                        type = ReminderType.CALL
                        title = "Call $displayName"
                    },
                    label = { Text("Call") },
                    leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White,
                        selectedLeadingIconColor = Color.White
                    )
                )
                FilterChip(
                    selected = type == ReminderType.POPUP_NOTE,
                    onClick = { 
                        type = ReminderType.POPUP_NOTE
                        title = "Note for $displayName"
                    },
                    label = { Text("Pop-up Note") },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White,
                        selectedLeadingIconColor = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Reminder Title") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )
            
            if (type == ReminderType.POPUP_NOTE) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Note Content") },
                    placeholder = { Text("What do you want to be reminded about?") },
                    minLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "Remind me in:", 
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf(1, 2, 4, 24).forEach { delay ->
                    FilterChip(
                        selected = hoursDelay == delay,
                        onClick = { hoursDelay = delay },
                        label = { Text(if (delay == 24) "Tomorrow" else "$delay hrs") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = {
                    val calendar = Calendar.getInstance()
                    calendar.add(Calendar.HOUR, hoursDelay)
                    
                    val reminder = ReminderEntity(
                        title = title,
                        description = description.ifBlank { null },
                        scheduledTime = calendar.timeInMillis,
                        phoneNumber = phoneNumber,
                        type = type
                    )
                    viewModel.insertReminder(reminder)
                    onReminderSaved()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Set Reminder")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
