package com.example.callmind.features.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.callmind.data.local.entities.ContactEntity
import com.example.callmind.data.local.entities.NoteEntity
import com.example.callmind.ui.components.CallMindCard
import com.example.callmind.ui.theme.CallMindTheme

@Composable
fun SearchScreen(viewModel: SearchViewModel = hiltViewModel()) {
    val query by viewModel.searchQuery.collectAsState()
    val results by viewModel.searchResults.collectAsState()

    SearchContent(
        query = query,
        results = results,
        onQueryChanged = { viewModel.onQueryChanged(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchContent(
    query: String,
    results: SearchResults,
    onQueryChanged: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = query,
                        onValueChange = onQueryChanged,
                        modifier = Modifier.fillMaxWidth().padding(end = 16.dp),
                        placeholder = { Text("Search contacts, notes...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent
                        )
                    )
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            if (results.contacts.isNotEmpty()) {
                item {
                    Text("People", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp))
                }
                items(results.contacts) { contact ->
                    com.example.callmind.features.contacts.ContactItem(contact)
                }
            }

            if (results.notes.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Notes", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp))
                }
                items(results.notes) { note ->
                    NoteResultItem(note)
                }
            }
            
            if (query.length >= 2 && results.contacts.isEmpty() && results.notes.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Text("No results found.")
                    }
                }
            }
        }
    }
}

@Composable
fun NoteResultItem(note: NoteEntity) {
    CallMindCard {
        Column {
            Text(text = note.content, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "For ${note.phoneNumber}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SearchScreenPreview() {
    CallMindTheme {
        SearchContent(
            query = "Arun",
            results = SearchResults(
                contacts = listOf(ContactEntity(name = "Arun Kumar", phoneNumber = "+91 98765 43210")),
                notes = listOf(NoteEntity(content = "Arun said he will call back tomorrow", phoneNumber = "+91 98765 43210"))
            ),
            onQueryChanged = {}
        )
    }
}
