package com.sentinel.ai.ui.navigation

sealed class Screen(val route: String) {
    data object Welcome : Screen("welcome")
    data object FeatureGuide : Screen("feature_guide")
    data object Story : Screen("story")
    data object Dashboard : Screen("dashboard")
    data object History : Screen("history")
    data object Settings : Screen("settings")
    data object PermissionSetup : Screen("permission_setup")
    data object IncidentHelp : Screen("incident_help")
    data object About : Screen("about")
    data object ThreatDetails : Screen("threat_details/{threatId}") {
        const val argumentName = "threatId"

        fun createRoute(threatId: String): String = "threat_details/$threatId"
    }

    // Legacy routes kept for compatibility with the frozen backend surface.
    data object Alerts : Screen("alerts")
    data object Scanner : Screen("scanner")
}
