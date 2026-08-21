package com.example.callmind.features.postcall

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.callmind.features.notes.NoteViewModel
import com.example.callmind.features.reminders.AddReminderSheet
import com.example.callmind.features.reminders.ReminderViewModel
import com.example.callmind.ui.components.CallMindCard
import com.example.callmind.ui.theme.CallMindTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PostCallActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val phoneNumber = intent.getStringExtra("EXTRA_PHONE_NUMBER") ?: "Unknown"
        setContent {
            val postCallViewModel: PostCallViewModel = hiltViewModel()
            val noteViewModel: NoteViewModel = hiltViewModel()
            val reminderViewModel: ReminderViewModel = hiltViewModel()
            
            PostCallContent(
                phoneNumber = phoneNumber,
                onDismiss = { finish() },
                onCallAgain = { postCallViewModel.callAgain(phoneNumber) },
                onWhatsApp = { postCallViewModel.openWhatsApp(phoneNumber) },
                onTelegram = { postCallViewModel.openTelegram(phoneNumber) },
                onSMS = { postCallViewModel.openSMS(phoneNumber) },
                onGPay = { postCallViewModel.openGPay(phoneNumber) },
                onViewContact = { postCallViewModel.openContact(phoneNumber) },
                onSaveNote = { phone, content -> noteViewModel.saveNote(phone, content) },
                onSaveReminder = { reminderViewModel }
            )
        }
    }
}

@Composable
fun PostCallContent(
    phoneNumber: String,
    onDismiss: () -> Unit,
    onCallAgain: () -> Unit,
    onWhatsApp: () -> Unit,
    onTelegram: () -> Unit,
    onSMS: () -> Unit,
    onGPay: () -> Unit,
    onViewContact: () -> Unit,
    onSaveNote: (String, String) -> Unit,
    onSaveReminder: () -> ReminderViewModel
) {
    var showAddNote by remember { mutableStateOf(false) }
    var showAddReminder by remember { mutableStateOf(false) }

    CallMindTheme {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "CALL ENDED",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 2.sp
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Surface(
                    modifier = Modifier.size(100.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = phoneNumber.take(1),
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = phoneNumber,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = "08:42",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Text(
                    text = "Actions",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.Start),
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                PostCallActionsGrid(
                    onCallAgain = onCallAgain,
                    onWhatsApp = onWhatsApp,
                    onTelegram = onTelegram,
                    onSMS = onSMS,
                    onGPay = onGPay,
                    onViewContact = onViewContact,
                    onAddNoteClick = { showAddNote = true },
                    onScheduleClick = { showAddReminder = true }
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                TextButton(onClick = onDismiss) {
                    Text("Dismiss")
                }
            }
        }

        if (showAddNote) {
            AddNoteSheetWrapper(
                phoneNumber = phoneNumber,
                onNoteSaved = {
                    showAddNote = false
                    onDismiss()
                },
                onDismiss = { showAddNote = false },
                onSave = onSaveNote
            )
        }

        if (showAddReminder) {
            AddReminderSheet(
                phoneNumber = phoneNumber,
                onReminderSaved = {
                    showAddReminder = false
                    onDismiss()
                },
                onDismiss = { showAddReminder = false },
                viewModel = onSaveReminder()
            )
        }
    }
}

@Composable
fun PostCallActionsGrid(
    onCallAgain: () -> Unit,
    onWhatsApp: () -> Unit,
    onTelegram: () -> Unit,
    onSMS: () -> Unit,
    onGPay: () -> Unit,
    onViewContact: () -> Unit,
    onAddNoteClick: () -> Unit,
    onScheduleClick: () -> Unit
) {
    Column {
        Row(modifier = Modifier.fillMaxWidth()) {
            PostCallActionButton(label = "WhatsApp", icon = Icons.Default.Phone, modifier = Modifier.weight(1f), onClick = onWhatsApp)
            PostCallActionButton(label = "Call", icon = Icons.Default.Call, modifier = Modifier.weight(1f), onClick = onCallAgain)
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            PostCallActionButton(label = "Telegram", icon = Icons.AutoMirrored.Filled.Send, modifier = Modifier.weight(1f), onClick = onTelegram)
            PostCallActionButton(label = "Message", icon = Icons.Default.Email, modifier = Modifier.weight(1f), onClick = onSMS)
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            PostCallActionButton(label = "Add Note", icon = Icons.Default.Edit, modifier = Modifier.weight(1f), onClick = onAddNoteClick)
            PostCallActionButton(label = "Schedule", icon = Icons.Default.DateRange, modifier = Modifier.weight(1f), onClick = onScheduleClick)
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            PostCallActionButton(label = "Set Reminder", icon = Icons.Default.Notifications, modifier = Modifier.weight(1f), onClick = onScheduleClick)
            PostCallActionButton(label = "GPay", icon = Icons.Default.ShoppingCart, modifier = Modifier.weight(1f), onClick = onGPay)
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            PostCallActionButton(label = "View Contact", icon = Icons.Default.Person, modifier = Modifier.weight(1f), onClick = onViewContact)
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun PostCallActionButton(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    CallMindCard(
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 4.dp),
                maxLines = 1
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNoteSheetWrapper(
    phoneNumber: String,
    onNoteSaved: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var noteContent by remember { mutableStateOf("") }
    
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
                text = "Add Note for $phoneNumber",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = noteContent,
                onValueChange = { noteContent = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("What do you want to remember?") },
                minLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                )
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = {
                    if (noteContent.isNotBlank()) {
                        onSave(phoneNumber, noteContent)
                        onNoteSaved()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Note")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PostCallPreview() {
    PostCallContent(
        phoneNumber = "+91 98765 43210",
        onDismiss = {},
        onCallAgain = {},
        onWhatsApp = {},
        onTelegram = {},
        onSMS = {},
        onGPay = {},
        onViewContact = {},
        onSaveNote = { _, _ -> },
        onSaveReminder = { throw Exception("Mock") }
    )
}
