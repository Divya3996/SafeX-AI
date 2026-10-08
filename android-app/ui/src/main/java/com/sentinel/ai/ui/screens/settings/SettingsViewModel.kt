package com.sentinel.ai.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.sentinel.ai.ui.protection.ProtectionControl

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        refreshStatus()
    }

    fun onAction(action: SettingsUiAction) {
        when (action) {
            is SettingsUiAction.SetGuardEnabled ->
                setGuardEnabled(action.enabled)
            is SettingsUiAction.SetNotificationsEnabled ->
                refreshStatus()
            is SettingsUiAction.SetOverlayAlertsEnabled ->
                refreshStatus()
            SettingsUiAction.RefreshStatus ->
                refreshStatus()
        }
    }

    private fun setGuardEnabled(enabled: Boolean) {
        ProtectionControl.setProtectionEnabled(context, enabled)
        refreshStatus()
    }

    private fun refreshStatus() {
        _uiState.update { current ->
            current.copy(
                protection = ProtectionControl.snapshot(context)
            )
        }
    }
}
