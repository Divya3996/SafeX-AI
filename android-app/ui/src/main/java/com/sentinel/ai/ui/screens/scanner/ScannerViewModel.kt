package com.sentinel.ai.ui.screens.scanner

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sentinel.ai.core.data.ScanRepository
import com.sentinel.ai.core.event.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

@HiltViewModel
class ScannerViewModel @Inject constructor(private val repository: ScanRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()
    private var scanJob: Job? = null
    fun onAction(action: ScannerUiAction) {
        when (action) {
            is ScannerUiAction.UpdateInput -> _uiState.update { it.copy(scanInput = action.text.take(12001), error = null, isDemo = false) }
            is ScannerUiAction.SetScanType -> _uiState.update { it.copy(scanType = action.type, scanInput = "", error = null, isDemo = false) }
            is ScannerUiAction.LoadSample -> _uiState.update { it.copy(scanInput = action.text, scanType = action.type, isDemo = true, error = null) }
            is ScannerUiAction.ScanLiveQr -> {
                _uiState.update { it.copy(scanInput = action.content, scanType = ScanType.QR, isDemo = false, error = null) }
                scan(liveQr = true)
            }
            ScannerUiAction.CancelScan -> { scanJob?.cancel(); _uiState.update { it.copy(isScanning = false) } }
            ScannerUiAction.ClearResult -> _uiState.update { it.copy(scanResult = null, error = null) }
            ScannerUiAction.RunScan -> scan()
        }
    }
    private fun scan(liveQr: Boolean = false) {
        if (_uiState.value.isScanning) return
        val snapshot = _uiState.value
        val input = snapshot.scanInput.trim()
        if (input.isBlank()) { _uiState.update { it.copy(error = "Add content to analyze first.") }; return }
        _uiState.update { it.copy(isScanning = true, error = null, scanResult = null) }
        scanJob = viewModelScope.launch {
            try {
                val result = withTimeout(20000) {
                    if (liveQr) repository.scanQrContent(input) else when (snapshot.scanType) {
                        ScanType.TEXT -> repository.scanText(input)
                        ScanType.LINK -> repository.scanLink(input)
                        ScanType.FILE -> repository.scanFile(Uri.parse(input))
                        ScanType.IMAGE -> repository.scanImage(Uri.parse(input))
                        ScanType.QR -> repository.scanImage(Uri.parse(input), qrOnly = true)
                    }
                }.copy(isDemo = snapshot.isDemo)
                if (snapshot.isDemo) ThreatJournal.recordDurably(ThreatEvent.LinkThreatDetected(result))
                _uiState.update { it.copy(isScanning = false, scanResult = result, scanInput = if (liveQr) "" else it.scanInput) }
            } catch (_: TimeoutCancellationException) {
                _uiState.update { it.copy(isScanning = false, error = "Analysis timed out. Try a smaller or clearer item.") }
            } catch (e: CancellationException) { throw e } catch (e: Exception) {
                _uiState.update { it.copy(isScanning = false, error = e.message ?: "Could not analyze this content.") }
            }
        }
    }
}
