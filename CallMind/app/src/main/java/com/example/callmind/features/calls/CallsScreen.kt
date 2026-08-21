package com.example.callmind.features.calls

import android.Manifest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.callmind.core.permissions.rememberPermissionState
import com.example.callmind.data.local.entities.CallLogDisplayItem
import com.example.callmind.data.local.entities.CallRecordEntity
import com.example.callmind.data.local.entities.CallType
import com.example.callmind.data.local.entities.GroupedCallLog
import com.example.callmind.data.local.entities.ReminderType
import com.example.callmind.features.postcall.PostCallViewModel
import com.example.callmind.features.reminders.AddReminderSheet
import com.example.callmind.features.reminders.ReminderViewModel
import com.example.callmind.ui.components.*
import com.example.callmind.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CallsScreen(
    viewModel: CallViewModel = hiltViewModel(),
    postCallViewModel: PostCallViewModel = hiltViewModel(),
    reminderViewModel: ReminderViewModel = hiltViewModel()
) {
    val callLogs by viewModel.callLogs.collectAsState()
    val currentFilter by viewModel.currentFilter.collectAsState()
    val permissionState = rememberPermissionState(Manifest.permission.READ_CALL_LOG)

    var showAddReminder by remember { mutableStateOf(false) }
    var selectedLogForReminder by remember { mutableStateOf<CallRecordEntity?>(null) }
    var selectedReminderType by remember { mutableStateOf(ReminderType.CALL) }

    LaunchedEffect(permissionState.isGranted) {
        if (permissionState.isGranted) {
            viewModel.loadCallLogs()
        }
    }

    Box {
        CallsContent(
            callLogs = callLogs,
            currentFilter = currentFilter,
            isPermissionGranted = permissionState.isGranted,
            onFilterSelected = { viewModel.setFilter(it) },
            onRequestPermission = { permissionState.requestPermission() },
            onRefresh = { viewModel.loadCallLogs() },
            postCallViewModel = postCallViewModel,
            onScheduleReminder = { log, type ->
                selectedLogForReminder = log
                selectedReminderType = type
                showAddReminder = true
            }
        )

        if (showAddReminder && selectedLogForReminder != null) {
            AddReminderSheet(
                phoneNumber = selectedLogForReminder!!.phoneNumber,
                contactName = selectedLogForReminder!!.contactName,
                onReminderSaved = { showAddReminder = false },
                onDismiss = { showAddReminder = false },
                viewModel = reminderViewModel,
                initialType = selectedReminderType
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallsContent(
    callLogs: List<CallLogDisplayItem>,
    currentFilter: CallFilter,
    isPermissionGranted: Boolean,
    onFilterSelected: (CallFilter) -> Unit,
    onRequestPermission: () -> Unit,
    onRefresh: () -> Unit,
    postCallViewModel: PostCallViewModel? = null,
    onScheduleReminder: (CallRecordEntity, ReminderType) -> Unit = { _, _ -> }
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Recent Calls",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "All your calls in one place",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row {
                        IconButton(onClick = { /* Search */ }, modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        IconButton(onClick = onRefresh, modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)) {
                            Icon(Icons.Default.Menu, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                CallFilterRow(currentFilter, onFilterSelected)
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            if (isPermissionGranted) {
                if (callLogs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No call history found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        items(callLogs) { item ->
                            when (item) {
                                is CallLogDisplayItem.Header -> {
                                    Text(
                                        text = item.title,
                                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                is CallLogDisplayItem.CallGroup -> {
                                    CallLogItemWorkable(item.group, postCallViewModel, onScheduleReminder)
                                }
                            }
                        }
                    }
                }
            } else {
                com.example.callmind.features.contacts.PermissionRequiredView(
                    message = "Call Log permission is required to show your recent calls.",
                    onRequestPermission = onRequestPermission
                )
            }
        }
    }
}

@Composable
fun CallFilterRow(currentFilter: CallFilter, onFilterSelected: (CallFilter) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterTab(label = "All", isSelected = currentFilter is CallFilter.All, onClick = { onFilterSelected(CallFilter.All) }, modifier = Modifier.weight(1f))
        FilterTab(label = "Incoming", icon = Icons.Default.KeyboardArrowDown, isSelected = currentFilter is CallFilter.Incoming, onClick = { onFilterSelected(CallFilter.Incoming) }, modifier = Modifier.weight(1.2f), iconTint = AccentGreen)
        FilterTab(label = "Outgoing", icon = Icons.Default.KeyboardArrowUp, isSelected = currentFilter is CallFilter.Outgoing, onClick = { onFilterSelected(CallFilter.Outgoing) }, modifier = Modifier.weight(1.2f), iconTint = Color.Red)
        FilterTab(label = "Missed", icon = Icons.Default.Warning, isSelected = currentFilter is CallFilter.Missed, onClick = { onFilterSelected(CallFilter.Missed) }, modifier = Modifier.weight(1.1f), iconTint = Color.Red)
    }
}

@Composable
fun FilterTab(
    label: String,
    icon: ImageVector? = null,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color = Color.Unspecified
) {
    Surface(
        modifier = modifier
            .height(40.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = if (isSelected) Color.White else iconTint)
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
fun CallLogItemWorkable(
    group: GroupedCallLog,
    postCallViewModel: PostCallViewModel?,
    onScheduleReminder: (CallRecordEntity, ReminderType) -> Unit = { _, _ -> }
) {
    val log = group.lastCall
    var expanded by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    val sdf = remember { SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()) }
    val dateString = remember(log.timestamp) { sdf.format(Date(log.timestamp)) }

    val avatarColor = remember(log.phoneNumber) {
        val colors = listOf(Color(0xFF4361EE), Color(0xFFF72585), Color(0xFF4CC9F0), Color(0xFF7209B7), Color(0xFFFB8500))
        colors[Math.abs(log.phoneNumber.hashCode()) % colors.size]
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .clickable { expanded = !expanded }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        Surface(
                            modifier = Modifier.size(52.dp),
                            shape = CircleShape,
                            color = avatarColor
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (log.contactName != null) {
                                    Text(
                                        text = log.contactName.take(1).uppercase(),
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
                                }
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(3.dp)
                        ) {
                            Icon(
                                imageVector = when (log.type) {
                                    CallType.INCOMING -> Icons.Default.KeyboardArrowDown
                                    CallType.OUTGOING -> Icons.Default.KeyboardArrowUp
                                    CallType.MISSED -> Icons.Default.Warning
                                },
                                contentDescription = null,
                                tint = if (log.type == CallType.MISSED) Color.Red else AccentGreen
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = buildAnnotatedString {
                                append(log.contactName ?: log.phoneNumber)
                                if (group.calls.size > 1) {
                                    withStyle(style = androidx.compose.ui.text.SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                                        append(" (${group.calls.size})")
                                    }
                                }
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val subtext = buildString {
                            if (log.contactName != null) {
                                append(log.phoneNumber)
                                append(" • ")
                            }
                            append(dateString)
                            if (log.durationSeconds > 0) {
                                append(" • ")
                                append(log.durationSeconds)
                                append("s")
                            }
                        }
                        Text(
                            text = subtext,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            modifier = Modifier.padding(top = 4.dp),
                            color = if (log.type == CallType.MISSED) Color.Red.copy(alpha = 0.1f) else AccentGreen.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = log.type.name,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (log.type == CallType.MISSED) Color.Red else AccentGreen,
                                fontSize = 9.sp
                            )
                        }
                    }
                    
                    IconButton(onClick = { postCallViewModel?.copyNumber(log.phoneNumber) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                    
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                AnimatedVisibility(visible = expanded) {
                    Column(modifier = Modifier.padding(bottom = 16.dp)) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            CallActionCircle(Icons.Default.Call, "Call", AccentGreen) { postCallViewModel?.callAgain(log.phoneNumber) }
                            CallActionCircle(Icons.Default.Call, "Call again", MaterialTheme.colorScheme.primary) { postCallViewModel?.callAgain(log.phoneNumber) }
                            CallActionCircle(Icons.Default.Email, "Message", AccentNotes) { postCallViewModel?.openSMS(log.phoneNumber) }
                            CallActionCircle(Icons.Default.Phone, "WhatsApp", AccentWhatsApp) { postCallViewModel?.openWhatsApp(log.phoneNumber) }
                            CallActionCircle(Icons.AutoMirrored.Filled.Send, "Telegram", AccentTelegram) { postCallViewModel?.openTelegram(log.phoneNumber) }
                            Box {
                                CallActionCircle(Icons.Default.MoreVert, "More", MaterialTheme.colorScheme.surfaceVariant) { showMoreMenu = true }
                                DropdownMenu(
                                    expanded = showMoreMenu,
                                    onDismissRequest = { showMoreMenu = false },
                                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Schedule CALL", color = MaterialTheme.colorScheme.onSurface) },
                                        onClick = {
                                            showMoreMenu = false
                                            onScheduleReminder(log, ReminderType.CALL)
                                        },
                                        leadingIcon = { Icon(Icons.Default.Call, contentDescription = null, tint = AccentGreen) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Schedule Pop-up Notes", color = MaterialTheme.colorScheme.onSurface) },
                                        onClick = {
                                            showMoreMenu = false
                                            onScheduleReminder(log, ReminderType.POPUP_NOTE)
                                        },
                                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = AccentNotes) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CallActionCircle(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier
                .size(44.dp)
                .clickable { onClick() },
            shape = CircleShape,
            color = color
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
    }
}

@Preview(showBackground = true)
@Composable
fun CallsScreenPreview() {
    val mockLog1 = CallRecordEntity(phoneNumber = "+919514815656", durationSeconds = 0, timestamp = System.currentTimeMillis(), type = CallType.INCOMING)
    val mockLog2 = CallRecordEntity(phoneNumber = "+919962315656", durationSeconds = 88, timestamp = System.currentTimeMillis() - 3600000, type = CallType.INCOMING)
    
    CallMindTheme {
        CallsContent(
            callLogs = listOf(
                CallLogDisplayItem.Header("Today"),
                CallLogDisplayItem.CallGroup(GroupedCallLog(mockLog1, listOf(mockLog1))),
                CallLogDisplayItem.CallGroup(GroupedCallLog(mockLog2, listOf(mockLog2)))
            ),
            currentFilter = CallFilter.All,
            isPermissionGranted = true,
            onFilterSelected = {},
            onRequestPermission = {},
            onRefresh = {}
        )
    }
}
