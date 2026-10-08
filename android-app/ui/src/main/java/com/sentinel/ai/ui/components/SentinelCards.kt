package com.sentinel.ai.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sentinel.ai.ui.theme.SentinelElevation
import com.sentinel.ai.ui.theme.SentinelSize
import com.sentinel.ai.ui.theme.SentinelSpacing

enum class CardVariant {
    Filled,
    Elevated,
    Outlined
}

@Composable
fun SentinelCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    BaseSentinelCard(
        variant = CardVariant.Filled,
        modifier = modifier,
        onClick = onClick,
        content = content
    )
}

@Composable
fun ElevatedSentinelCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    BaseSentinelCard(
        variant = CardVariant.Elevated,
        modifier = modifier,
        onClick = onClick,
        content = content
    )
}

@Composable
fun OutlinedSentinelCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    BaseSentinelCard(
        variant = CardVariant.Outlined,
        modifier = modifier,
        onClick = onClick,
        content = content
    )
}

@Composable
private fun BaseSentinelCard(
    variant: CardVariant,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val colors = CardDefaults.cardColors(
        containerColor = if (variant == CardVariant.Outlined) {
            Color.Unspecified
        } else {
            MaterialTheme.colorScheme.surface
        }
    )

    val border = if (variant == CardVariant.Outlined) {
        androidx.compose.foundation.BorderStroke(
            width = SentinelSize.BorderThickness,
            color = MaterialTheme.colorScheme.outline
        )
    } else {
        null
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = colors,
        elevation = CardDefaults.cardElevation(
            when (variant) {
                CardVariant.Elevated -> SentinelElevation.CardRaised
                else -> SentinelElevation.CardResting
            }
        ),
        border = border,
        onClick = onClick ?: {},
        interactionSource = interactionSource
    ) {
        Column(
            modifier = Modifier.padding(SentinelSpacing.CardPadding)
        ) {
            content()
        }
    }
}
