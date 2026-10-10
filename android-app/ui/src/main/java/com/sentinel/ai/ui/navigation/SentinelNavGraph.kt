@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.sentinel.ai.ui.navigation

import com.sentinel.ai.ui.guidance.*
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.focus.focusProperties
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.sentinel.ai.ui.screens.about.AboutScreen
import com.sentinel.ai.ui.screens.alert.AlertScreen
import com.sentinel.ai.ui.screens.dashboard.DashboardScreen
import com.sentinel.ai.ui.screens.history.HistoryScreen
import com.sentinel.ai.ui.screens.permissions.PermissionOnboardingScreen
import com.sentinel.ai.ui.screens.scanner.ScannerScreen
import com.sentinel.ai.ui.screens.settings.SettingsScreen
import com.sentinel.ai.ui.theme.SentinelThemeMode
import com.sentinel.ai.ui.screens.threat.ThreatDetailsScreen
import com.sentinel.ai.ui.theme.rememberWindowWidthClass
import kotlinx.coroutines.launch

/**
 * Root Sentinel navigation shell.
 *
 * Assembles a premium, adaptive Material 3 application shell: a [ModalNavigationDrawer], a
 * contextual top app bar, and an adaptive navigation surface (bottom bar on phones, navigation
 * rail on tablets). It applies edge-to-edge-friendly window-inset handling and subtle Material
 * Motion transitions between destinations. No routes, navigation logic or ViewModels are changed.
 *
 * @param navController the app's navigation controller.
 * @param startDestination initial route, defaults to [Screen.Dashboard].
 * @param appVersion version string forwarded to screens that display it.
 */
@Composable
fun SentinelNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Dashboard.route,
    themeMode: SentinelThemeMode = SentinelThemeMode.Dark,
    onThemeModeSelected: (SentinelThemeMode) -> Unit = {},
    onPermissionOnboardingComplete: () -> Unit = {},
    appVersion: String = "1.0.0"
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isPermissionSetup = currentRoute == Screen.PermissionSetup.route || currentRoute == Screen.Welcome.route
    val context = LocalContext.current
    val tourProgress = rememberSaveable { mutableIntStateOf(-1) }
    val guide = remember { GuideController(context.applicationContext, tourProgress) }
    LaunchedEffect(currentRoute) {
        if (currentRoute == Screen.Dashboard.route && GuidancePreferences.consumePendingTour(context)) guide.start()
    }
    LaunchedEffect(guide.index) {
        guide.current?.let { step ->
            if (currentRoute != step.route) navController.navigate(step.route) { launchSingleTop = true }
        }
    }

    val windowWidth = rememberWindowWidthClass()
    val isCompact = windowWidth.isCompact

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val onDestinationSelected: (Screen) -> Unit = { screen ->
        scope.launch { drawerState.close() }
        navController.navigate(screen.route) {
            if (screen == Screen.Dashboard) {
                popUpTo(Screen.Dashboard.route) { inclusive = true }
            } else {
                popUpTo(Screen.Dashboard.route) {
                    saveState = true
                }
            }
            launchSingleTop = true
            restoreState = screen != Screen.Dashboard
        }
    }

    CompositionLocalProvider(LocalGuideController provides guide) {
    Box(Modifier.fillMaxSize()) {
    ModalNavigationDrawer(
        modifier = Modifier.focusProperties { canFocus = !guide.active }
            .then(if (guide.active) Modifier.semantics { invisibleToUser() } else Modifier),
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerContentColor = MaterialTheme.colorScheme.onSurface
            ) {
                SentinelNavDrawerContent(
                    currentRoute = currentRoute,
                    onDestinationSelected = onDestinationSelected
                )
            }
        },
        gesturesEnabled = false
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            if (!isCompact && !isPermissionSetup) {
                SentinelNavRail(
                    currentRoute = currentRoute,
                    onDestinationSelected = onDestinationSelected,
                    modifier = Modifier.fillMaxHeight()
                )
            }

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    if (!isPermissionSetup) {
                        SentinelTopBar(
                            currentRoute = currentRoute,
                            onMenuClicked = { scope.launch { drawerState.open() } },
                            onBackClicked = { navController.popBackStack() }
                        )
                    }
                },
                bottomBar = {
                    if (isCompact && !isPermissionSetup && currentRoute?.startsWith("threat_details") != true && currentRoute != Screen.IncidentHelp.route) {
                        SentinelBottomNav(
                            currentRoute = currentRoute,
                            onDestinationSelected = onDestinationSelected
                        )
                    }
                },
                containerColor = MaterialTheme.colorScheme.background
            ) { paddingValues ->
                SentinelNavHost(
                    navController = navController,
                    paddingValues = paddingValues,
                    startDestination = startDestination,
                    themeMode = themeMode,
                    onThemeModeSelected = onThemeModeSelected,
                    onPermissionOnboardingComplete = onPermissionOnboardingComplete,
                    appVersion = appVersion
                )
            }
        }
    }
    if (guide.active) key(guide.index) {
        GuideOverlay(guide) { navController.navigate(Screen.IncidentHelp.route) { launchSingleTop = true } }
    }
    }
    }
}

/**
 * Hosts every destination in the app. Each [composable] carries a subtle fade-through transition
 * built from the shared [SentinelNavEnterTransition] family.
 */
@Composable
private fun SentinelNavHost(
    navController: NavHostController,
    paddingValues: PaddingValues,
    startDestination: String,
    themeMode: SentinelThemeMode,
    onThemeModeSelected: (SentinelThemeMode) -> Unit,
    onPermissionOnboardingComplete: () -> Unit,
    appVersion: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier.padding(paddingValues)
    ) {
        composable(Screen.Welcome.route) {
            val context = LocalContext.current
            WelcomeScreen(onIncidentHelp = { navController.navigate(Screen.IncidentHelp.route) }) { tour ->
                GuidancePreferences.finishIntro(context, tour)
                navController.navigate(Screen.PermissionSetup.route) {
                    popUpTo(Screen.Welcome.route) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
        composable(Screen.FeatureGuide.route) {
            val guide = LocalGuideController.current
            FeatureGuideScreen { index -> guide?.start(index) }
        }
        composable(
            route = Screen.Dashboard.route,
            enterTransition = { SentinelNavEnterTransition },
            exitTransition = { SentinelNavExitTransition },
            popEnterTransition = { SentinelNavPopEnterTransition },
            popExitTransition = { SentinelNavPopExitTransition }
        ) {
            DashboardScreen(
                onFeatureGuide = { navController.navigate(Screen.FeatureGuide.route) },
                onIncidentHelp = { navController.navigate(Screen.IncidentHelp.route) },
                onThreatSelected = { threatId ->
                    navController.navigate(Screen.ThreatDetails.createRoute(threatId))
                },
                onNavigateToScanner = {
                    navController.navigate(Screen.Scanner.route)
                }
            )
        }
        composable(
            route = Screen.ThreatDetails.route,
            arguments = listOf(navArgument(Screen.ThreatDetails.argumentName) { type = NavType.StringType }),
            enterTransition = { SentinelNavEnterTransition },
            exitTransition = { SentinelNavExitTransition },
            popEnterTransition = { SentinelNavPopEnterTransition },
            popExitTransition = { SentinelNavPopExitTransition }
        ) { backStackEntry ->
            ThreatDetailsScreen(
                threatId = backStackEntry.arguments?.getString(Screen.ThreatDetails.argumentName).orEmpty(),
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.History.route,
            enterTransition = { SentinelNavEnterTransition },
            exitTransition = { SentinelNavExitTransition },
            popEnterTransition = { SentinelNavPopEnterTransition },
            popExitTransition = { SentinelNavPopExitTransition }
        ) {
            HistoryScreen()
        }
        composable(
            route = Screen.Settings.route,
            enterTransition = { SentinelNavEnterTransition },
            exitTransition = { SentinelNavExitTransition },
            popEnterTransition = { SentinelNavPopEnterTransition },
            popExitTransition = { SentinelNavPopExitTransition }
        ) {
            SettingsScreen(
                onFeatureGuide = { navController.navigate(Screen.FeatureGuide.route) },
                appVersion = appVersion,
                selectedTheme = themeMode,
                onThemeSelected = onThemeModeSelected,
                onNavigateToAbout = {
                    navController.navigate(Screen.About.route)
                }
            )
        }
        composable(
            route = Screen.PermissionSetup.route,
            enterTransition = { SentinelNavEnterTransition },
            exitTransition = { SentinelNavExitTransition },
            popEnterTransition = { SentinelNavPopEnterTransition },
            popExitTransition = { SentinelNavPopExitTransition }
        ) {
            PermissionOnboardingScreen(
                onPermissionsComplete = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.PermissionSetup.route) { inclusive = true }
                        launchSingleTop = true
                    }
                    onPermissionOnboardingComplete()
                }
            )
        }
        composable(
            route = Screen.About.route,
            enterTransition = { SentinelNavEnterTransition },
            exitTransition = { SentinelNavExitTransition },
            popEnterTransition = { SentinelNavPopEnterTransition },
            popExitTransition = { SentinelNavPopExitTransition }
        ) {
            AboutScreen(
                appVersion = appVersion
            )
        }
        composable(
            route = Screen.Alerts.route,
            enterTransition = { SentinelNavEnterTransition },
            exitTransition = { SentinelNavExitTransition },
            popEnterTransition = { SentinelNavPopEnterTransition },
            popExitTransition = { SentinelNavPopExitTransition }
        ) {
            AlertScreen(
                onNavigateToDetails = { threatId ->
                    navController.navigate(Screen.ThreatDetails.createRoute(threatId))
                }
            )
        }
        composable(Screen.IncidentHelp.route) {
            com.sentinel.ai.ui.screens.help.IncidentHelpScreen { navController.popBackStack() }
        }
        composable(
            route = Screen.Scanner.route,
            enterTransition = { SentinelNavEnterTransition },
            exitTransition = { SentinelNavExitTransition },
            popEnterTransition = { SentinelNavPopEnterTransition },
            popExitTransition = { SentinelNavPopExitTransition }
        ) {
            ScannerScreen()
        }
    }
}
