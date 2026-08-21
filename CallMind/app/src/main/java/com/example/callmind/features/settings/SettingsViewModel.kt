package com.example.callmind.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.callmind.data.local.CallMindDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val database: CallMindDatabase
) : ViewModel() {

    fun clearAllData() {
        viewModelScope.launch {
            database.clearAllTables()
        }
    }
}
