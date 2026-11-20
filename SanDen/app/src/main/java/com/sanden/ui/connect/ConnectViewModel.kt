package com.sanden.ui.connect

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanden.data.db.SyncMessageDao
import com.sanden.data.db.SyncMessageEntity
import com.sanden.data.transfer.LanTransferHub
import com.sanden.domain.repository.VaultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConnectViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val hub: LanTransferHub,
    private val syncMessageDao: SyncMessageDao,
    private val vaultRepository: VaultRepository
) : ViewModel() {

    val isRunning: StateFlow<Boolean> = hub.isRunning
    val serverUrl: StateFlow<String> = hub.serverUrl
    val connectedClients: StateFlow<Int> = hub.connectedClients

    val messages: StateFlow<List<SyncMessageEntity>> = syncMessageDao.observeAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        if (!hub.isRunning.value) {
            hub.start()
        }
    }

    fun toggleHub() {
        if (hub.isRunning.value) {
            hub.stop()
        } else {
            hub.start()
        }
    }

    fun sendText(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            hub.sendPhoneMessage(trimmed, type = "TEXT")
        }
    }

    fun sendClipboard(): Boolean {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipText = cm.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()?.trim()
        if (clipText.isNullOrEmpty()) return false
        viewModelScope.launch {
            hub.sendPhoneMessage(clipText, type = "CLIPBOARD")
        }
        return true
    }

    fun copyToClipboard(text: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("SanDen", text))
    }

    fun sendFiles(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            for (uri in uris) {
                val res = vaultRepository.importFile(uri, "")
                res.onSuccess { vaultFile ->
                    hub.sendPhoneFile(
                        filePath = vaultFile.path,
                        fileName = vaultFile.name,
                        fileSize = vaultFile.size,
                        mimeType = vaultFile.mimeType ?: "application/octet-stream"
                    )
                }
            }
            vaultRepository.scanVault()
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            syncMessageDao.clearAll()
        }
    }
}
