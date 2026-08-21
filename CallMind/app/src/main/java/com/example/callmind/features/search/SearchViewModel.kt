package com.example.callmind.features.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.callmind.data.local.entities.ContactEntity
import com.example.callmind.data.local.entities.NoteEntity
import com.example.callmind.data.repository.NoteRepository
import com.example.callmind.data.system.ContactProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val contactProvider: ContactProvider,
    private val noteRepository: NoteRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _searchResults = MutableStateFlow<SearchResults>(SearchResults())
    val searchResults: StateFlow<SearchResults> = _searchResults

    fun onQueryChanged(query: String) {
        _searchQuery.value = query
        if (query.length >= 2) {
            performSearch(query)
        } else {
            _searchResults.value = SearchResults()
        }
    }

    private fun performSearch(query: String) {
        viewModelScope.launch {
            val contacts = contactProvider.getContacts().filter {
                it.name?.contains(query, ignoreCase = true) == true || 
                it.phoneNumber.contains(query)
            }
            
            noteRepository.getAllNotes().collectLatest { notes ->
                val filteredNotes = notes.filter { 
                    it.content.contains(query, ignoreCase = true) ||
                    it.phoneNumber.contains(query)
                }
                _searchResults.value = SearchResults(contacts, filteredNotes)
            }
        }
    }
}

data class SearchResults(
    val contacts: List<ContactEntity> = emptyList(),
    val notes: List<NoteEntity> = emptyList()
)
