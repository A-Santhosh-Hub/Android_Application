package com.example.callmind.features.contacts

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.callmind.core.permissions.rememberPermissionState
import com.example.callmind.data.local.entities.ContactEntity
import com.example.callmind.ui.components.CallMindCard
import com.example.callmind.ui.theme.CallMindTheme
import android.util.Log

@Composable
fun ContactsScreen(viewModel: ContactViewModel = hiltViewModel()) {
    val contacts by viewModel.contacts.collectAsState()
    val permissionState = rememberPermissionState(Manifest.permission.READ_CONTACTS)

    Log.d("ContactsScreen", "isGranted: ${permissionState.isGranted}, contacts count: ${contacts.size}")

    LaunchedEffect(permissionState.isGranted) {
        if (permissionState.isGranted) {
            Log.d("ContactsScreen", "Permission granted, loading contacts...")
            viewModel.loadContacts()
        }
    }

    ContactsContent(
        contacts = contacts,
        isPermissionGranted = permissionState.isGranted,
        onRequestPermission = { permissionState.requestPermission() },
        onRefresh = { viewModel.loadContacts() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsContent(
    contacts: List<ContactEntity>,
    isPermissionGranted: Boolean,
    onRequestPermission: () -> Unit,
    onRefresh: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Contacts") },
                actions = {
                    if (isPermissionGranted) {
                        IconButton(onClick = onRefresh) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            if (isPermissionGranted) {
                if (contacts.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("No contacts found.")
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onRefresh) {
                            Text("Retry")
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(contacts) { contact ->
                            ContactItem(contact)
                        }
                    }
                }
            } else {
                PermissionRequiredView(
                    message = "Contacts permission is required to show your contact list.",
                    onRequestPermission = onRequestPermission
                )
            }
        }
    }
}

@Composable
fun ContactItem(contact: ContactEntity) {
    CallMindCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = (contact.name ?: "U").take(1),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = contact.name ?: "Unknown",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = contact.phoneNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun PermissionRequiredView(message: String, onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = message, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRequestPermission) {
            Text("Grant Permission")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ContactsScreenPreview() {
    CallMindTheme {
        ContactsContent(
            contacts = listOf(
                ContactEntity(name = "Arun Kumar", phoneNumber = "+91 98765 43210"),
                ContactEntity(name = "Priya", phoneNumber = "+91 87654 32109")
            ),
            isPermissionGranted = true,
            onRequestPermission = {},
            onRefresh = {}
        )
    }
}
