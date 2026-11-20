package com.sanden.ui.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.sanden.domain.model.FileCategory
import com.sanden.domain.model.VaultFile
import com.sanden.ui.browser.FolderBrowserScreen
import com.sanden.ui.category.CategoryScreen
import com.sanden.ui.connect.ConnectScreen
import com.sanden.ui.home.HomeScreen
import com.sanden.ui.onboarding.OnboardingScreen
import com.sanden.ui.settings.SettingsScreen
import com.sanden.ui.viewer.AudioPlayerScreen
import com.sanden.ui.viewer.ImageViewerScreen
import com.sanden.ui.viewer.VideoPlayerScreen
import java.io.File

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
    data object Connect : Screen("connect")
}

@Composable
fun SanDenNavGraph(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val openFile: (VaultFile) -> Unit = { file ->
        when (file.category) {
            FileCategory.IMAGE -> navController.navigate(Screen.ImageViewer.createRoute(file.id))
            FileCategory.VIDEO -> navController.navigate(Screen.VideoPlayer.createRoute(file.id))
            FileCategory.AUDIO -> navController.navigate(Screen.AudioPlayer.createRoute(file.id))
            else -> openFileWithChooser(context, file)
        }
    }

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
                onConnectClick = {
                    navController.navigate(Screen.Connect.route)
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                },
                onFileClick = openFile
            )
        }

        composable(
            route = Screen.Category.route,
            arguments = listOf(navArgument("categoryName") { type = NavType.StringType })
        ) {
            CategoryScreen(
                onNavigateBack = { navController.popBackStack() },
                onFileClick = openFile
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
                onFileClick = openFile
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

        composable(Screen.Connect.route) {
            ConnectScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

private fun openFileWithChooser(context: Context, file: VaultFile) {
    try {
        val targetFile = File(file.path)
        if (!targetFile.exists()) {
            Toast.makeText(context, "File not found: ${file.name}", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            targetFile
        )
        val mime = file.mimeType ?: "*/*"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Open ${file.name}"))
    } catch (e: Exception) {
        Toast.makeText(context, "No app found to open ${file.name}", Toast.LENGTH_SHORT).show()
    }
}
