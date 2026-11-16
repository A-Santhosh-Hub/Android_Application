package com.sanden.ui.onboarding

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sanden.domain.usecase.ScanVaultUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PermissionState {
    CHECKING,
    GRANTED,
    DENIED
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    application: Application,
    private val scanVaultUseCase: ScanVaultUseCase
) : AndroidViewModel(application) {

    private val _permissionState = MutableStateFlow(PermissionState.CHECKING)
    val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()

    init {
        checkPermission()
    }

    fun checkPermission() {
        val context = getApplication<Application>()
        val isGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            val read = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            val write = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            read && write
        }

        if (isGranted) {
            _permissionState.value = PermissionState.GRANTED
            // Run initial vault preparation and scan
            viewModelScope.launch {
                try {
                    scanVaultUseCase()
                } catch (_: Exception) {}
            }
        } else {
            _permissionState.value = PermissionState.DENIED
        }
    }

    fun getPermissionIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
            } catch (_: Exception) {
                Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        }
    }
}
