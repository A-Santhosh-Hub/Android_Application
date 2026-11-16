package com.sanden.ui.navigation

import android.net.Uri
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.sanden.domain.model.FileCategory
import com.sanden.ui.browser.FolderBrowserScreen
import com.sanden.ui.category.CategoryScreen
import com.sanden.ui.home.HomeScreen
import com.sanden.ui.onboarding.OnboardingScreen
import com.sanden.ui.settings.SettingsScreen
import com.sanden.ui.viewer.AudioPlayerScreen
import com.sanden.ui.viewer.ImageViewerScreen
import com.sanden.ui.viewer.VideoPlayerScreen

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Home : Screen("home")
    data object Category : Screen("category/{categoryName}") {
        fun createRoute(category: FileCategory) = "category/${category.name}"
    }
    data object FolderBrowser : Screen("browser?path={path}") {
        fun createRoute(path: String = "") = if (path.isEmpty()) "browser" else "browser?path=${Uri.encode(path)}"
    }
    data object ImageViewer : Screen("image_viewer/{fileId}") {
        fun createRoute(fileId: Long) = "image_viewer/$fileId"
    }
    data object VideoPlayer : Screen("video_player/{fileId}") {
        fun createRoute(fileId: Long) = "video_player/$fileId"
    }
    data object AudioPlayer : Screen("audio_player/{fileId}") {
        fun createRoute(fileId: Long) = "audio_player/$fileId"
    }
    data object Settings : Screen("settings")
}

@Composable
fun SanDenNavGraph(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = { fadeIn(animationSpec = tween(220)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(220)) },
        exitTransition = { fadeOut(animationSpec = tween(200)) },
        popEnterTransition = { fadeIn(animationSpec = tween(200)) },
        popExitTransition = { fadeOut(animationSpec = tween(200)) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(200)) }
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onPermissionGranted = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onCategoryClick = { category ->
                    navController.navigate(Screen.Category.createRoute(category))
                },
                onBrowseFoldersClick = {
                    navController.navigate(Screen.FolderBrowser.createRoute())
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                },
                onFileClick = { file ->
                    when (file.category) {
                        FileCategory.IMAGE -> navController.navigate(Screen.ImageViewer.createRoute(file.id))
                        FileCategory.VIDEO -> navController.navigate(Screen.VideoPlayer.createRoute(file.id))
                        FileCategory.AUDIO -> navController.navigate(Screen.AudioPlayer.createRoute(file.id))
                        else -> { /* Handled via FileProvider Intent */ }
                    }
                }
            )
        }

        composable(
            route = Screen.Category.route,
            arguments = listOf(navArgument("categoryName") { type = NavType.StringType })
        ) {
            CategoryScreen(
                onNavigateBack = { navController.popBackStack() },
                onFileClick = { file ->
                    when (file.category) {
                        FileCategory.IMAGE -> navController.navigate(Screen.ImageViewer.createRoute(file.id))
                        FileCategory.VIDEO -> navController.navigate(Screen.VideoPlayer.createRoute(file.id))
                        FileCategory.AUDIO -> navController.navigate(Screen.AudioPlayer.createRoute(file.id))
                        else -> { /* Intent chooser */ }
                    }
                }
            )
        }

        composable(
            route = Screen.FolderBrowser.route,
            arguments = listOf(
                navArgument("path") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            FolderBrowserScreen(
                onNavigateBack = { navController.popBackStack() },
                onFolderClick = { folderPath ->
                    navController.navigate(Screen.FolderBrowser.createRoute(folderPath))
                },
                onFileClick = { file ->
                    when (file.category) {
                        FileCategory.IMAGE -> navController.navigate(Screen.ImageViewer.createRoute(file.id))
                        FileCategory.VIDEO -> navController.navigate(Screen.VideoPlayer.createRoute(file.id))
                        FileCategory.AUDIO -> navController.navigate(Screen.AudioPlayer.createRoute(file.id))
                        else -> { /* Intent chooser */ }
                    }
                }
            )
        }

        composable(
            route = Screen.ImageViewer.route,
            arguments = listOf(navArgument("fileId") { type = NavType.LongType })
        ) {
            ImageViewerScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.VideoPlayer.route,
            arguments = listOf(navArgument("fileId") { type = NavType.LongType })
        ) {
            VideoPlayerScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.AudioPlayer.route,
            arguments = listOf(navArgument("fileId") { type = NavType.LongType })
        ) {
            AudioPlayerScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
