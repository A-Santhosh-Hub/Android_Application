package com.example.callmind.features.notes

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNoteSheet(
    phoneNumber: String,
    onNoteSaved: () -> Unit,
    onDismiss: () -> Unit,
    viewModel: NoteViewModel
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
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = {
                    if (noteContent.isNotBlank()) {
                        viewModel.saveNote(phoneNumber, noteContent)
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
