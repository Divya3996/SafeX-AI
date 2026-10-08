package com.sentinel.ai.ui.screens.settings

import com.sentinel.ai.ui.protection.ProtectionSnapshot

data class SettingsUiState(
    val protection: ProtectionSnapshot = ProtectionSnapshot(),
    val onboardingComplete: Boolean = false
)

sealed interface SettingsUiAction {
    data class SetGuardEnabled(val enabled: Boolean) : SettingsUiAction
    data class SetNotificationsEnabled(val enabled: Boolean) : SettingsUiAction
    data class SetOverlayAlertsEnabled(val enabled: Boolean) : SettingsUiAction
    data object RefreshStatus : SettingsUiAction
}
