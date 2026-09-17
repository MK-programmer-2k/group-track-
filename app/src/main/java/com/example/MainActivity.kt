package com.example

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.service.GeofenceNotificationHelper
import com.example.ui.components.ErrorBoundary
import com.example.ui.components.ErrorBoundaryController
import com.example.ui.navigation.NavRoutes
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.ChatViewModel
import com.example.ui.viewmodel.GeofenceViewModel
import com.example.ui.viewmodel.GroupViewModel
import com.example.ui.viewmodel.HistoryViewModel
import com.example.ui.viewmodel.LiveMapViewModel
import com.example.ui.viewmodel.RetentionSettingsViewModel

class MainActivity : ComponentActivity() {

    private val globalErrorState = mutableStateOf<Throwable?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Intercept uncaught main looper exceptions to display ErrorBoundary instead of closing app
        Handler(Looper.getMainLooper()).post {
            while (true) {
                try {
                    Looper.loop()
                } catch (t: Throwable) {
                    Log.e("MainActivity", "Intercepted runtime exception in main looper", t)
                    runOnUiThread {
                        globalErrorState.value = t
                    }
                }
            }
        }

        // Global uncaught exception listener for coroutines/background threads
        val defaultUncaughtHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("MainActivity", "Intercepted uncaught exception on thread: ${thread.name}", throwable)
            runOnUiThread {
                globalErrorState.value = throwable
            }
        }

        val app = application as GroupTrackApplication
        val authViewModel by viewModels<AuthViewModel> {
            AuthViewModel.Factory(app.repository, app.sessionManager)
        }
        val groupViewModel by viewModels<GroupViewModel> {
            GroupViewModel.Factory(app.repository, app.sessionManager)
        }
        val liveMapViewModel by viewModels<LiveMapViewModel> {
            LiveMapViewModel.Factory(this, app.repository, app.sessionManager, app.socketManager)
        }
        val historyViewModel by viewModels<HistoryViewModel> {
            HistoryViewModel.Factory(app.repository)
        }
        val retentionSettingsViewModel by viewModels<RetentionSettingsViewModel> {
            RetentionSettingsViewModel.Factory(app.repository, app.sessionManager)
        }
        val chatViewModel by viewModels<ChatViewModel> {
            ChatViewModel.Factory(this, app.repository, app.sessionManager)
        }
        val geofenceViewModel by viewModels<GeofenceViewModel>()

        GeofenceNotificationHelper.ensureChannel(this)

        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                val token by authViewModel.isLoggedIn.collectAsState(initial = null)

                val errorController = remember {
                    object : ErrorBoundaryController {
                        override val currentError: Throwable?
                            get() = globalErrorState.value

                        override fun reportError(error: Throwable) {
                            globalErrorState.value = error
                        }

                        override fun clearError() {
                            globalErrorState.value = null
                        }
                    }
                }

                ErrorBoundary(
                    controller = errorController,
                    onRestartApp = {
                        val intent = intent
                        finish()
                        startActivity(intent)
                    },
                    onNavigateHome = {
                        try {
                            navController.navigate(NavRoutes.HOME) {
                                popUpTo(0) { inclusive = true }
                            }
                        } catch (_: Exception) {
                            recreate()
                        }
                    }
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = NavRoutes.SPLASH,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable(NavRoutes.SPLASH) {
                            SplashScreen(
                                isLoggedIn = token != null,
                                onNavigateToHome = {
                                    navController.navigate(NavRoutes.HOME) {
                                        popUpTo(NavRoutes.SPLASH) { inclusive = true }
                                    }
                                },
                                onNavigateToLogin = {
                                    navController.navigate(NavRoutes.LOGIN) {
                                        popUpTo(NavRoutes.SPLASH) { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable(NavRoutes.LOGIN) {
                            LoginScreen(
                                authViewModel = authViewModel,
                                onNavigateToRegister = {
                                    navController.navigate(NavRoutes.REGISTER)
                                },
                                onLoginSuccess = {
                                    navController.navigate(NavRoutes.HOME) {
                                        popUpTo(NavRoutes.LOGIN) { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable(NavRoutes.REGISTER) {
                            RegisterScreen(
                                authViewModel = authViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onRegisterSuccess = {
                                    navController.navigate(NavRoutes.HOME) {
                                        popUpTo(NavRoutes.LOGIN) { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable(NavRoutes.HOME) {
                            HomeScreen(
                                groupViewModel = groupViewModel,
                                liveMapViewModel = liveMapViewModel,
                                onNavigateToMap = { navController.navigate(NavRoutes.LIVE_MAP) },
                                onNavigateToGroups = { navController.navigate(NavRoutes.GROUPS) },
                                onNavigateToMembers = { groupId ->
                                    navController.navigate(NavRoutes.groupMembers(groupId))
                                },
                                onNavigateToSharingSettings = {
                                    navController.navigate(NavRoutes.SHARING_SETTINGS)
                                },
                                onNavigateToSettings = { navController.navigate(NavRoutes.SETTINGS) },
                                onNavigateToGeofences = { navController.navigate(NavRoutes.GEOFENCES) },
                                onNavigateToChat = { groupId ->
                                    navController.navigate(NavRoutes.groupChat(groupId))
                                }
                            )
                        }

                        composable(NavRoutes.LIVE_MAP) {
                            LiveMapScreen(
                                groupViewModel = groupViewModel,
                                liveMapViewModel = liveMapViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToHistory = { groupId, userId ->
                                    navController.navigate(NavRoutes.locationHistory(groupId, userId))
                                },
                                onNavigateToGeofences = { groupId ->
                                    navController.navigate(NavRoutes.GEOFENCES)
                                },
                                onNavigateToChat = { groupId ->
                                    navController.navigate(NavRoutes.groupChat(groupId))
                                }
                            )
                        }

                        composable(NavRoutes.GROUPS) {
                            GroupListScreen(
                                groupViewModel = groupViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToCreate = { navController.navigate(NavRoutes.CREATE_GROUP) },
                                onNavigateToJoin = { navController.navigate(NavRoutes.JOIN_GROUP) },
                                onNavigateToMembers = { groupId ->
                                    navController.navigate(NavRoutes.groupMembers(groupId))
                                }
                            )
                        }

                        composable(NavRoutes.CREATE_GROUP) {
                            CreateGroupScreen(
                                groupViewModel = groupViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onGroupCreated = {
                                    groupViewModel.clearActionState()
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable(NavRoutes.JOIN_GROUP) {
                            JoinGroupScreen(
                                groupViewModel = groupViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onGroupJoined = {
                                    groupViewModel.clearActionState()
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable(
                            route = NavRoutes.GROUP_MEMBERS,
                            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
                            GroupMembersScreen(
                                groupId = groupId,
                                groupViewModel = groupViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToChat = { gId ->
                                    navController.navigate(NavRoutes.groupChat(gId))
                                }
                            )
                        }

                        composable(
                            route = NavRoutes.GROUP_CHAT,
                            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
                            GroupChatScreen(
                                groupId = groupId,
                                chatViewModel = chatViewModel,
                                groupViewModel = groupViewModel,
                                liveMapViewModel = liveMapViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToMap = { navController.navigate(NavRoutes.LIVE_MAP) }
                            )
                        }

                        composable(
                            route = NavRoutes.LOCATION_HISTORY,
                            arguments = listOf(
                                navArgument("groupId") { type = NavType.StringType },
                                navArgument("userId") {
                                    type = NavType.StringType
                                    nullable = true
                                    defaultValue = null
                                }
                            )
                        ) { backStackEntry ->
                            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
                            val userId = backStackEntry.arguments?.getString("userId")
                            LocationHistoryScreen(
                                groupId = groupId,
                                userId = userId,
                                historyViewModel = historyViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToRetentionSettings = { navController.navigate(NavRoutes.DATA_RETENTION) }
                            )
                        }

                        composable(NavRoutes.SHARING_SETTINGS) {
                            LocationSharingSettingsScreen(
                                groupViewModel = groupViewModel,
                                liveMapViewModel = liveMapViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable(NavRoutes.SETTINGS) {
                            SettingsScreen(
                                authViewModel = authViewModel,
                                sessionManager = app.sessionManager,
                                retentionSettingsViewModel = retentionSettingsViewModel,
                                onNavigateToRetentionSettings = { navController.navigate(NavRoutes.DATA_RETENTION) },
                                onNavigateBack = { navController.popBackStack() },
                                onLoggedOut = {
                                    navController.navigate(NavRoutes.LOGIN) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable(NavRoutes.DATA_RETENTION) {
                            DataRetentionSettingsScreen(
                                viewModel = retentionSettingsViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable(NavRoutes.GEOFENCES) {
                            GeofenceManagementScreen(
                                viewModel = geofenceViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
