package com.sentinel.ai.ui.components

import androidx.compose.ui.graphics.Color
import com.sentinel.ai.ui.theme.SentinelCyan
import com.sentinel.ai.ui.theme.SentinelGreen
import com.sentinel.ai.ui.theme.SentinelRed
import com.sentinel.ai.ui.theme.SentinelYellow

enum class RiskState {
    Safe,
    Suspicious,
    Dangerous,
    Neutral,
    Scanning
}

fun riskColor(state: RiskState): Color = when (state) {
    RiskState.Safe -> SentinelGreen
    RiskState.Suspicious -> SentinelYellow
    RiskState.Dangerous -> SentinelRed
    RiskState.Neutral -> SentinelCyan
    RiskState.Scanning -> SentinelYellow
}
