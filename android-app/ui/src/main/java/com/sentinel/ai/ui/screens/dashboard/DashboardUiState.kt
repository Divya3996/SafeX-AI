package com.sentinel.ai.ui.screens.dashboard

import com.sentinel.ai.core.model.Alert
import com.sentinel.ai.core.model.ScanResult
import com.sentinel.ai.ui.protection.ProtectionSnapshot

data class DashboardUiState(
    val isLoading: Boolean = false,
    val protection: ProtectionSnapshot = ProtectionSnapshot(),
    val recentAlerts: List<Alert> = emptyList(),
    val recentScans: List<ScanResult> = emptyList(),
    val latestScan: ScanResult? = null,
    val scanCount: Int = 0,
    val error: String? = null
)

sealed interface DashboardUiAction {
    data object ToggleGuard : DashboardUiAction
    data object RefreshStatus : DashboardUiAction
}
