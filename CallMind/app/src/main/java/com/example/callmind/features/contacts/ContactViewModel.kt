package com.example.callmind.features.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.callmind.data.local.entities.ContactEntity
import com.example.callmind.data.system.ContactProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContactViewModel @Inject constructor(
    private val contactProvider: ContactProvider
) : ViewModel() {

    private val _contacts = MutableStateFlow<List<ContactEntity>>(emptyList())
    val contacts: StateFlow<List<ContactEntity>> = _contacts

    fun loadContacts() {
        viewModelScope.launch {
            _contacts.value = contactProvider.getContacts()
        }
    }
}
