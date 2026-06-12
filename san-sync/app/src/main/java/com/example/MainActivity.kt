package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.data.AppDatabase
import com.example.data.NoteRepository
import com.example.ui.KeepApp
import com.example.ui.NoteViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Setup notifications permissions & start persistent background task listener
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
        }
        com.example.data.TaskReminderService.startService(this)

        // Setup Room local database persistence
        val database = AppDatabase.getDatabase(this)
        val repository = NoteRepository(database.noteDao())

        // Setup ViewModel matching Keep's states
        val viewModel: NoteViewModel by viewModels {
            NoteViewModel.Factory(application, repository)
        }

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val useDarkTheme = when (themeMode) {
                com.example.ui.ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
                com.example.ui.ThemeMode.LIGHT -> false
                com.example.ui.ThemeMode.DARK -> true
            }

            MyApplicationTheme(darkTheme = useDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    KeepApp(viewModel = viewModel)
                }
            }
        }
    }
}
