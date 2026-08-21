package com.example.callmind.features.onboarding

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.example.callmind.core.permissions.CallMindPermissions
import com.example.callmind.ui.theme.CallMindTheme
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = CallMindPermissions.RequiredPermissions.all { 
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED 
        }
        if (allGranted) {
            onFinished()
        }
    }

    CallMindTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(100.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Text(
                    text = "Welcome to CallMind",
                    style = MaterialTheme.typography.headlineLarge,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "To help you remember your conversations and organize next steps, CallMind needs access to your contacts and call logs.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(48.dp))
                
                Button(
                    onClick = { launcher.launch(CallMindPermissions.RequiredPermissions.toTypedArray()) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Get Started")
                }
            }
        }
    }
}
