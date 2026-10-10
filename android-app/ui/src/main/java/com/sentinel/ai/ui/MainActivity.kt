package com.sentinel.ai.ui

import android.app.AlertDialog
import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.navigation.compose.rememberNavController
import com.sentinel.ai.ui.BuildConfig
import com.sentinel.ai.core.feature.FeatureManager
import com.sentinel.ai.ui.navigation.SentinelNavGraph
import com.sentinel.ai.ui.navigation.Screen
import com.sentinel.ai.ui.theme.SentinelTheme
import com.sentinel.ai.ui.theme.ThemePreferences
import com.sentinel.ai.ui.theme.rememberThemeMode
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var storyRequest by mutableIntStateOf(0)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent); setIntent(intent)
        if (intent.getBooleanExtra(com.sentinel.ai.ui.screens.story.StoryNavigation.REQUEST, false)) storyRequest++
    }

    override fun onResume() {
        super.onResume()
        com.sentinel.ai.core.feature.DisplayPreferences.syncSystemLanguage(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.getBooleanExtra(com.sentinel.ai.ui.screens.story.StoryNavigation.REQUEST, false)) storyRequest = 1

        val preferences = getSharedPreferences(ONBOARDING_PREFERENCES, MODE_PRIVATE)
        val firstLaunch = preferences.getBoolean(KEY_FIRST_LAUNCH, true)

        isPermissionOnboardingLaunch = firstLaunch

        enableEdgeToEdge()

        setContent {
            val themeMode = rememberThemeMode(this)

            SentinelTheme(mode = themeMode.value) {
                val navController = rememberNavController()

                SentinelNavGraph(
                    navController = navController,
                    startDestination = if (com.sentinel.ai.ui.guidance.GuidancePreferences.needsIntro(this)) {
                        Screen.Welcome.route
                    } else if (isPermissionOnboardingLaunch) {
                        Screen.PermissionSetup.route
                    } else {
                        Screen.Dashboard.route
                    },
                    themeMode = themeMode.value,
                    onThemeModeSelected = { ThemePreferences.set(this, it) },
                    onPermissionOnboardingComplete = ::completePermissionOnboarding,
                    appVersion = BuildConfig.APP_VERSION,
                    openStoryRequest = storyRequest
                )
            }
        }
    }

    private fun completePermissionOnboarding() {
        if (!isPermissionOnboardingLaunch) return

        getSharedPreferences(ONBOARDING_PREFERENCES, MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_FIRST_LAUNCH, false)
            .apply()
        isPermissionOnboardingLaunch = false


    }

    private companion object {
        const val ONBOARDING_PREFERENCES = "sentinel_onboarding"
        const val KEY_FIRST_LAUNCH = "first_launch"
    }

    private var isPermissionOnboardingLaunch = false
}
