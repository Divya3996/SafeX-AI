package com.sentinel.ai.ui.i18n

import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.TextUnit
import com.sentinel.ai.core.feature.*
import com.sentinel.ai.core.i18n.I18n

@Composable fun localized(text: String): String {
    val settings by DisplayPreferences.settings.collectAsState()
    return I18n.translate(LocalContext.current, text, settings.language)
}
@Composable fun LocalizedText(text: String, modifier: Modifier = Modifier, color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified, fontStyle: FontStyle? = null, fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null, letterSpacing: TextUnit = TextUnit.Unspecified, textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null, lineHeight: TextUnit = TextUnit.Unspecified, overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true, maxLines: Int = Int.MAX_VALUE, minLines: Int = 1,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null, style: TextStyle = LocalTextStyle.current,
    localize: Boolean = true) {
    androidx.compose.material3.Text(if (localize) localized(text) else text, modifier, color, fontSize, fontStyle,
        fontWeight, fontFamily, letterSpacing, textDecoration, textAlign, lineHeight, overflow, softWrap,
        maxLines, minLines, onTextLayout, style)
}
