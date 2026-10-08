package com.sentinel.ai.ui.screens.history

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.sentinel.ai.core.event.ThreatJournal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

@HiltViewModel
class HistoryViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        observeHistory()
    }

    fun onAction(action: HistoryUiAction) {
        when (action) {
            HistoryUiAction.Refresh -> refresh()
        }
    }

    fun delete(id: String? = null) {
        viewModelScope.launch {
            try { ThreatJournal.delete(id) } catch (_: Exception) { _uiState.update { it.copy(error = "Unable to delete history. Try again.") } }
        }
    }

    private fun refresh() {
        _uiState.update { current ->
            current.copy(history = ThreatJournal.scanResults.value)
        }
    }

    private fun observeHistory() {
        viewModelScope.launch {
            ThreatJournal.scanResults.collectLatest { history ->
                _uiState.update { current ->
                    current.copy(history = history)
                }
            }
        }
    }
}
