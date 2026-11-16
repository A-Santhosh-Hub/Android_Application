package com.sanden.ui

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.sanden.domain.repository.VaultRepository
import com.sanden.domain.usecase.ScanVaultUseCase
import com.sanden.ui.navigation.SanDenNavGraph
import com.sanden.ui.navigation.Screen
import com.sanden.ui.theme.SanDenTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var scanVaultUseCase: ScanVaultUseCase

    @Inject
    lateinit var vaultRepository: VaultRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val isPermissionGranted = checkStoragePermission()
        val startDestination = if (isPermissionGranted) Screen.Home.route else Screen.Onboarding.route

        setContent {
            SanDenTheme {
                val navController = rememberNavController()
                SanDenNavGraph(
                    navController = navController,
                    startDestination = startDestination
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (checkStoragePermission()) {
            vaultRepository.startWatching()
            lifecycleScope.launch {
                try {
                    scanVaultUseCase()
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Scan failed", e)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        vaultRepository.stopWatching()
    }

    private fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            val read = ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            val write = ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            read && write
        }
    }
}
