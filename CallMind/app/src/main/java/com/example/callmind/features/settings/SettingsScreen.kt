package com.example.callmind.features.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.callmind.ui.theme.CallMindTheme

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    SettingsContent(
        onClearData = { viewModel.clearAllData() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(onClearData: () -> Unit) {
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings") })
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                SettingsSection(title = "Appearance") {
                    SettingsItem(label = "Theme", icon = Icons.Default.Settings, value = "System default")
                    SettingsItem(label = "Dynamic Color", icon = Icons.Default.Check, value = "On")
                }
            }
            
            item {
                SettingsSection(title = "Privacy") {
                    SettingsItem(
                        label = "Clear All CallMind Data",
                        icon = Icons.Default.Delete,
                        onClick = { showDeleteConfirm = true },
                        textColor = MaterialTheme.colorScheme.error
                    )
                }
            }

            item {
                SettingsSection(title = "About") {
                    SettingsItem(
                        label = "Developed by SanStudio",
                        icon = Icons.Default.Info,
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://sanstudio-hub.github.io/in/"))
                            context.startActivity(intent)
                        }
                    )
                    SettingsItem(label = "Version", icon = Icons.Default.Info, value = "1.0.0")
                }
            }
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Clear All Data") },
                text = { Text("This will permanently delete all your CallMind notes and reminders. This action cannot be undone.") },
                confirmButton = {
                    TextButton(onClick = {
                        onClearData()
                        showDeleteConfirm = false
                    }) {
                        Text("Clear", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Column(modifier = Modifier.fillMaxWidth(), content = content)
        }
    }
}

@Composable
fun SettingsItem(
    label: String,
    icon: ImageVector,
    value: String? = null,
    onClick: () -> Unit = {},
    textColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = textColor.copy(alpha = 0.6f))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge, color = textColor, modifier = Modifier.weight(1f))
        if (value != null) {
            Text(text = value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    CallMindTheme {
        SettingsContent(onClearData = {})
    }
}
