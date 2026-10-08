package com.sentinel.ai.ui.screens.scanner

import com.sentinel.ai.core.model.ScanResult

enum class ScanType {
    TEXT,
    LINK,
    FILE,
    IMAGE,
    QR
}

data class ScannerUiState(
    val scanInput: String = "",
    val scanType: ScanType = ScanType.TEXT,
    val isScanning: Boolean = false,
    val scanResult: ScanResult? = null,
    val error: String? = null,
    val isDemo: Boolean = false
)

sealed interface ScannerUiAction {
    data class UpdateInput(val text: String) : ScannerUiAction
    data class SetScanType(val type: ScanType) : ScannerUiAction
    data class LoadSample(val text: String, val type: ScanType) : ScannerUiAction
    data class ScanLiveQr(val content: String) : ScannerUiAction
    data object CancelScan : ScannerUiAction
    data object RunScan : ScannerUiAction
    data object ClearResult : ScannerUiAction
}
