package com.example.callmind.features.home

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
import androidx.compose.ui.graphics.Brush
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
import com.example.callmind.data.local.entities.CallLogDisplayItem
import com.example.callmind.data.local.entities.CallRecordEntity
import com.example.callmind.data.local.entities.CallType
import com.example.callmind.data.local.entities.GroupedCallLog
import com.example.callmind.data.local.entities.ReminderEntity
import com.example.callmind.features.postcall.PostCallViewModel
import com.example.callmind.ui.components.*
import com.example.callmind.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    postCallViewModel: PostCallViewModel = hiltViewModel(),
    onSearchClick: () -> Unit = {},
    onCallsClick: () -> Unit = {},
    onNotesClick: () -> Unit = {},
    onRemindersClick: () -> Unit = {}
) {
    val state by viewModel.homeState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadHomeData()
    }

    HomeScreenContent(
        state = state,
        onSearchClick = onSearchClick,
        onCallsClick = onCallsClick,
        onNotesClick = onNotesClick,
        onRemindersClick = onRemindersClick,
        onCallClick = { postCallViewModel.callAgain("") },
        onWhatsAppClick = { postCallViewModel.openWhatsApp("") },
        onTelegramClick = { postCallViewModel.openTelegram("") },
        postCallViewModel = postCallViewModel
    )
}

@Composable
fun HomeScreenContent(
    state: HomeState,
    onSearchClick: () -> Unit,
    onCallsClick: () -> Unit,
    onNotesClick: () -> Unit,
    onRemindersClick: () -> Unit,
    onCallClick: () -> Unit,
    onWhatsAppClick: () -> Unit,
    onTelegramClick: () -> Unit,
    postCallViewModel: PostCallViewModel? = null
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                HomeHeader()
            }

            item {
                TodayCommunicationCard(state)
            }

            item {
                QuickActionsCard(
                    onSearch = onSearchClick,
                    onNotes = onNotesClick,
                    onCall = onCallClick,
                    onWhatsApp = onWhatsAppClick,
                    onTelegram = onTelegramClick
                )
            }

            item {
                SectionHeader(title = "Recent calls", onActionClick = onCallsClick)
            }
            
            item {
                Surface(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        state.recentCalls.filterIsInstance<CallLogDisplayItem.CallGroup>().take(3).forEach { item ->
                            RecentActivityItem(item.group, postCallViewModel)
                        }
                    }
                }
            }

            item {
                SectionHeader(title = "Upcoming", onActionClick = onRemindersClick)
            }
            
            item {
                Surface(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(24.dp)
                ) {
                    if (state.upcomingReminders.isEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "No upcoming items",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    } else {
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            state.upcomingReminders.forEach { reminder ->
                                UpcomingCard(reminder)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Home",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Surface(
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.padding(12.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun TodayCommunicationCard(state: HomeState) {
    Surface(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "Today's communication",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(28.dp))
            
            Row(modifier = Modifier.fillMaxWidth()) {
                StatItemVertical(Icons.Default.Call, state.callCount.toString(), "Calls", modifier = Modifier.weight(1f))
                Box(modifier = Modifier.width(1.dp).height(40.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)).align(Alignment.CenterVertically))
                StatItemVertical(Icons.Default.DateRange, state.totalTalkTime, "Talk time", modifier = Modifier.weight(1.2f))
                Box(modifier = Modifier.width(1.dp).height(40.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)).align(Alignment.CenterVertically))
                StatItemVertical(Icons.Default.Edit, state.noteCount.toString(), "Notes", modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun StatItemVertical(icon: ImageVector, value: String, label: String, modifier: Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun QuickActionsCard(
    onSearch: () -> Unit,
    onNotes: () -> Unit,
    onCall: () -> Unit,
    onWhatsApp: () -> Unit,
    onTelegram: () -> Unit
) {
    Surface(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = "Quick actions", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ActionCircleButton(Icons.Default.Call, "Call", AccentGreen, onClick = onCall)
                ActionCircleButton(Icons.Default.Phone, "WhatsApp", AccentWhatsApp, onClick = onWhatsApp)
                ActionCircleButton(Icons.AutoMirrored.Filled.Send, "Telegram", AccentTelegram, onClick = onTelegram)
                ActionCircleButton(Icons.Default.Search, "Search", MaterialTheme.colorScheme.primary, onClick = onSearch)
                ActionCircleButton(Icons.Default.Edit, "Note", AccentNotes, onClick = onNotes)
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, icon: ImageVector? = null, onActionClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.width(10.dp))
            }
            Text(text = title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
        }
        Text(
            text = "View all",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable { onActionClick() }
        )
    }
}

@Composable
fun UpcomingCard(reminder: ReminderEntity) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when(reminder.type) {
                    com.example.callmind.data.local.entities.ReminderType.CALL -> Icons.Default.Call
                    com.example.callmind.data.local.entities.ReminderType.POPUP_NOTE -> Icons.Default.Edit
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = reminder.title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            Text(text = "Today • 8:30 PM", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = CircleShape
        ) {
            Text(
                text = "In 2h 4m",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun RecentActivityItem(
    group: GroupedCallLog,
    postCallViewModel: PostCallViewModel? = null
) {
    val call = group.lastCall
    val avatarColor = remember(call.phoneNumber) {
        val colors = listOf(Color(0xFF4361EE), Color(0xFFF72585), Color(0xFF4CC9F0), Color(0xFF7209B7), Color(0xFFFB8500))
        colors[Math.abs(call.phoneNumber.hashCode()) % colors.size]
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(avatarColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (call.contactName != null) {
                Text(
                    text = call.contactName.take(1).uppercase(),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = buildAnnotatedString {
                    append(call.contactName ?: call.phoneNumber)
                    if (group.calls.size > 1) {
                        withStyle(style = androidx.compose.ui.text.SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                            append(" (${group.calls.size})")
                        }
                    }
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when(call.type) {
                        CallType.INCOMING -> Icons.Default.KeyboardArrowDown
                        CallType.OUTGOING -> Icons.Default.KeyboardArrowUp
                        CallType.MISSED -> Icons.Default.Warning
                    },
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = if (call.type == CallType.MISSED) AccentRed else AccentGreen
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${if(call.type == CallType.INCOMING) "Incoming" else if(call.type == CallType.OUTGOING) "Outgoing" else "Missed"} • 8 min ago",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        IconButton(
            onClick = { postCallViewModel?.callAgain(call.phoneNumber) },
            modifier = Modifier.size(44.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
        ) {
            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(20.dp), tint = AccentGreen)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    val mockLog1 = CallRecordEntity(phoneNumber = "+919514815656", durationSeconds = 120, timestamp = System.currentTimeMillis(), type = CallType.INCOMING)
    val mockLog2 = CallRecordEntity(phoneNumber = "+919962315656", durationSeconds = 60, timestamp = System.currentTimeMillis(), type = CallType.INCOMING)
    
    CallMindTheme {
        HomeScreenContent(
            state = HomeState(
                recentCalls = listOf(
                    CallLogDisplayItem.Header("Today"),
                    CallLogDisplayItem.CallGroup(GroupedCallLog(mockLog1, listOf(mockLog1, mockLog1))),
                    CallLogDisplayItem.CallGroup(GroupedCallLog(mockLog2, listOf(mockLog2)))
                ),
                upcomingReminders = listOf(ReminderEntity(title = "Call Arun Kumar", scheduledTime = 0)),
                callCount = 6660,
                noteCount = 0,
                scheduledCount = 2,
                totalTalkTime = "42m"
            ),
            onSearchClick = {},
            onCallsClick = {},
            onNotesClick = {},
            onRemindersClick = {},
            onCallClick = {},
            onWhatsAppClick = {},
            onTelegramClick = {}
        )
    }
}
