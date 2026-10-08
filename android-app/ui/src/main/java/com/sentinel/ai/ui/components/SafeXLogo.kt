package com.sentinel.ai.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import com.sentinel.ai.ui.i18n.localized

/** Original vector brand asset, shared with the adaptive launcher and exported SVG. */
@Composable
fun SafeXLogo(modifier: Modifier = Modifier.size(48.dp)) {
    Image(painterResource(com.sentinel.ai.core.R.drawable.safex_logo), localized("SafeX AI logo"), modifier)
}
