package com.example.callmind.features.notes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.callmind.data.local.entities.NoteEntity
import com.example.callmind.ui.components.CallMindCard
import com.example.callmind.ui.theme.CallMindTheme
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NotesScreen(viewModel: NoteViewModel = hiltViewModel()) {
    val notes by viewModel.allNotes.collectAsState(initial = emptyList())

    NotesContent(
        notes = notes,
        onDeleteNote = { viewModel.deleteNote(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesContent(
    notes: List<NoteEntity>,
    onDeleteNote: (NoteEntity) -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Notes", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            if (notes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No notes saved yet.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(notes) { note ->
                        NoteItem(note, onDeleteNote)
                    }
                }
            }
        }
    }
}

@Composable
fun NoteItem(note: NoteEntity, onDeleteNote: (NoteEntity) -> Unit) {
    val sdf = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val dateString = remember(note.createdAt) { sdf.format(Date(note.createdAt)) }

    CallMindCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "For ${note.phoneNumber} • $dateString",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            IconButton(onClick = { onDeleteNote(note) }) {
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
fun NotesScreenPreview() {
    CallMindTheme {
        NotesContent(
            notes = listOf(
                NoteEntity(content = "Follow up about project quotation", phoneNumber = "+91 98765 43210"),
                NoteEntity(content = "Arun mentioned a new client opportunity", phoneNumber = "9876543210")
            ),
            onDeleteNote = {}
        )
    }
}
