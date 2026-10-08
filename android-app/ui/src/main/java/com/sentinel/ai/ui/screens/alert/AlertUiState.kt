package com.sentinel.ai.ui.screens.alert

import com.sentinel.ai.core.model.Alert
import com.sentinel.ai.core.model.RiskLevel

data class AlertUiState(
    val isLoading: Boolean = false,
    val alerts: List<Alert> = emptyList(),
    val selectedAlert: Alert? = null,
    val filter: RiskLevel? = null
)

sealed interface AlertUiAction {
    data class SelectAlert(val alertId: String) : AlertUiAction
    data class FilterByRisk(val level: RiskLevel?) : AlertUiAction
    data class DismissAlert(val alertId: String) : AlertUiAction
}
