package com.sentinel.ai.ui.screens.history

import com.sentinel.ai.core.model.ScanResult

data class HistoryUiState(
    val isLoading: Boolean = false,
    val history: List<ScanResult> = emptyList(),
    val error: String? = null
)

sealed interface HistoryUiAction {
    data object Refresh : HistoryUiAction
}
