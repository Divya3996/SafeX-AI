package com.sentinel.ai.ui.screens.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sentinel.ai.core.event.ThreatJournal
import com.sentinel.ai.ui.protection.ProtectionControl
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@HiltViewModel
class DashboardViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        observeJournal()
        refreshStatus()
    }

    fun onAction(action: DashboardUiAction) {
        when (action) {
            DashboardUiAction.ToggleGuard -> toggleGuard()
            DashboardUiAction.RefreshStatus -> refreshStatus()
        }
    }

    private fun refreshStatus() {
        _uiState.update { current ->
            current.copy(
                isLoading = false,
                protection = ProtectionControl.snapshot(context)
            )
        }
    }

    private fun toggleGuard() {
        val enabled = !_uiState.value.protection.protectionEnabled
        ProtectionControl.setProtectionEnabled(context, enabled)
        refreshStatus()
    }

    private fun observeJournal() {
        viewModelScope.launch {
            ThreatJournal.alerts.collectLatest { alerts ->
                _uiState.update { current ->
                    current.copy(recentAlerts = alerts)
                }
                refreshStatus()
            }
        }
        viewModelScope.launch {
            ThreatJournal.scanResults.collectLatest { scans ->
                _uiState.update { current ->
                    current.copy(
                        recentScans = scans.filter { !it.target.isNullOrBlank() }.take(3),
                        latestScan = scans.firstOrNull(),
                        scanCount = scans.size
                    )
                }
            }
        }
    }
}
