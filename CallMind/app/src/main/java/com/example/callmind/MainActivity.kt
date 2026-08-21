package com.example.callmind

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.callmind.features.home.HomeScreen
import com.example.callmind.features.home.HomeViewModel
import com.example.callmind.features.calls.CallsScreen
import com.example.callmind.features.contacts.ContactsScreen
import com.example.callmind.features.notes.NotesScreen
import com.example.callmind.features.reminders.RemindersScreen
import com.example.callmind.features.settings.SettingsScreen
import com.example.callmind.features.search.SearchScreen
import com.example.callmind.features.onboarding.OnboardingScreen
import com.example.callmind.navigation.Screen
import com.example.callmind.navigation.bottomNavItems
import com.example.callmind.ui.theme.*
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val sharedPrefs = remember { context.getSharedPreferences("callmind_prefs", Context.MODE_PRIVATE) }
            var showOnboarding by remember { 
                mutableStateOf(sharedPrefs.getBoolean("show_onboarding", true)) 
            }

            if (showOnboarding) {
                OnboardingScreen(onFinished = {
                    sharedPrefs.edit().putBoolean("show_onboarding", false).apply()
                    showOnboarding = false
                })
            } else {
                CallMindAppContent()
            }
        }
    }
}

@Composable
fun CallMindAppContent() {
    val navController = rememberNavController()
    val homeViewModel: HomeViewModel = hiltViewModel()
    val activeReminderCount by homeViewModel.activeReminderCount.collectAsState(initial = 0)
    
    CallMindTheme {
        Scaffold(
            containerColor = BackgroundMain,
            bottomBar = {
                Surface(
                    color = SurfaceCard,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    NavigationBar(
                        containerColor = Color.Transparent,
                        tonalElevation = 0.dp
                    ) {
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentDestination = navBackStackEntry?.destination
                        
                        bottomNavItems.forEach { screen ->
                            val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                            
                            NavigationBarItem(
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (screen.route == Screen.Reminders.route && activeReminderCount > 0) {
                                                Badge(containerColor = AccentRed) {
                                                    Text(activeReminderCount.toString(), color = Color.White)
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = screen.icon,
                                            contentDescription = screen.label,
                                            tint = if (selected) PurpleGradientStart else TextSecondary
                                        )
                                    }
                                },
                                label = { 
                                    Text(
                                        text = screen.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (selected) Color.White else TextSecondary,
                                        maxLines = 1
                                    ) 
                                },
                                selected = selected,
                                alwaysShowLabel = false,
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = SurfaceAction
                                ),
                                onClick = {
                                    if (currentDestination?.route != screen.route) {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Home.route) { 
                    HomeScreen(
                        viewModel = homeViewModel,
                        onSearchClick = { navController.navigate("search") },
                        onCallsClick = { navController.navigate(Screen.Calls.route) },
                        onNotesClick = { navController.navigate(Screen.Notes.route) },
                        onRemindersClick = { navController.navigate(Screen.Reminders.route) }
                    )
                }
                composable(Screen.Calls.route) { CallsScreen() }
                composable(Screen.Contacts.route) { ContactsScreen() }
                composable(Screen.Reminders.route) { RemindersScreen() }
                composable(Screen.Notes.route) { NotesScreen() }
                composable(Screen.Settings.route) { SettingsScreen() }
                composable("search") { SearchScreen() }
            }
        }
    }
}
