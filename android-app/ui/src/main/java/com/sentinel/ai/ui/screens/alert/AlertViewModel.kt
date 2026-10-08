package com.sentinel.ai.ui.screens.alert

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import com.sentinel.ai.core.event.ThreatJournal
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class AlertViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(AlertUiState())
    val uiState: StateFlow<AlertUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            ThreatJournal.alerts.collectLatest { alerts ->
                _uiState.update { it.copy(alerts = alerts) }
            }
        }
    }

    fun onAction(action: AlertUiAction) {
        when (action) {
            is AlertUiAction.SelectAlert -> selectAlert(action.alertId)
            is AlertUiAction.FilterByRisk -> filterByRisk(action.level)
            is AlertUiAction.DismissAlert -> dismissAlert(action.alertId)
        }
    }

    private fun selectAlert(alertId: String) {
        _uiState.update { state ->
            state.copy(selectedAlert = state.alerts.firstOrNull { it.id == alertId })
        }
    }

    private fun filterByRisk(level: com.sentinel.ai.core.model.RiskLevel?) {
        _uiState.update { it.copy(filter = level) }
    }

    private fun dismissAlert(alertId: String) {
        _uiState.update { state ->
            state.copy(selectedAlert = null)
        }
    }
}
