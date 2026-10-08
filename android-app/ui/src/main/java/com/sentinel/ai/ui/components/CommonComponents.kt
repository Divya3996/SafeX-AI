package com.sentinel.ai.ui.components

import com.sentinel.ai.ui.i18n.LocalizedText as Text

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sentinel.ai.ui.theme.SentinelMotion
import com.sentinel.ai.ui.theme.SentinelSize
import com.sentinel.ai.ui.theme.SentinelSpacing

@Composable
fun ActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    contentDescription: String? = null
) {
    val description = com.sentinel.ai.ui.i18n.localized(contentDescription ?: text)

    Button(
        onClick = onClick,
        modifier = modifier
            .semantics(mergeDescendants = true) {
                this.contentDescription = description
                role = Role.Button
            },
        enabled = enabled && !loading,
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.38f)
        )
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(SentinelSize.IconSmall),
                strokeWidth = SentinelSize.BorderThickness,
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(SentinelSize.IconSmall)
                )
            }
            Spacer(modifier = Modifier.width(if (leadingIcon != null || trailingIcon != null) SentinelSpacing.XS else 0.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            if (trailingIcon != null) {
                Spacer(modifier = Modifier.width(SentinelSpacing.XS))
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(SentinelSize.IconSmall)
                )
            }
        }
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    variant: ButtonVariant = ButtonVariant.Outlined,
    contentDescription: String? = null
) {
    val description = com.sentinel.ai.ui.i18n.localized(contentDescription ?: text)

    when (variant) {
        ButtonVariant.Outlined -> {
            OutlinedButton(
                onClick = onClick,
                modifier = modifier
                    .semantics(mergeDescendants = true) {
                        this.contentDescription = description
                        role = Role.Button
                    },
                enabled = enabled && !loading,
                shape = MaterialTheme.shapes.medium
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(SentinelSize.IconSmall),
                        strokeWidth = SentinelSize.BorderThickness,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    if (leadingIcon != null) {
                        Icon(
                            imageVector = leadingIcon,
                            contentDescription = null,
                            modifier = Modifier.size(SentinelSize.IconSmall)
                        )
                    }
                    Spacer(modifier = Modifier.width(if (leadingIcon != null || trailingIcon != null) SentinelSpacing.XS else 0.dp))
                    Text(
                        text = text,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (trailingIcon != null) {
                        Spacer(modifier = Modifier.width(SentinelSpacing.XS))
                        Icon(
                            imageVector = trailingIcon,
                            contentDescription = null,
                            modifier = Modifier.size(SentinelSize.IconSmall)
                        )
                    }
                }
            }
        }
        ButtonVariant.Text -> {
            TextButton(
                onClick = onClick,
                modifier = modifier
                    .semantics(mergeDescendants = true) {
                        this.contentDescription = description
                        role = Role.Button
                    },
                enabled = enabled && !loading,
                shape = MaterialTheme.shapes.medium
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(SentinelSize.IconSmall),
                        strokeWidth = SentinelSize.BorderThickness,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    if (leadingIcon != null) {
                        Icon(
                            imageVector = leadingIcon,
                            contentDescription = null,
                            modifier = Modifier.size(SentinelSize.IconSmall)
                        )
                    }
                    Spacer(modifier = Modifier.width(if (leadingIcon != null || trailingIcon != null) SentinelSpacing.XS else 0.dp))
                    Text(
                        text = text,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (trailingIcon != null) {
                        Spacer(modifier = Modifier.width(SentinelSpacing.XS))
                        Icon(
                            imageVector = trailingIcon,
                            contentDescription = null,
                            modifier = Modifier.size(SentinelSize.IconSmall)
                        )
                    }
                }
            }
        }
    }
}

enum class ButtonVariant {
    Outlined,
    Text
}

@Composable
fun IconTextRow(
    icon: @Composable () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val desc = com.sentinel.ai.ui.i18n.localized(title) + (subtitle?.let { ". " + com.sentinel.ai.ui.i18n.localized(it) } ?: "")
    val semanticsModifier = modifier.semantics(mergeDescendants = true) {
        contentDescription = desc
        if (onClick != null) {
            role = Role.Button
        }
    }

    val rowModifier = if (onClick != null) {
        semanticsModifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
    } else {
        semanticsModifier
    }

    Row(
        modifier = if (isPressed) {
            rowModifier.padding(SentinelSpacing.SM)
        } else {
            rowModifier.padding(vertical = SentinelSpacing.XS)
        },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SentinelSpacing.SM)
    ) {
        Box(
            modifier = Modifier.size(SentinelSize.IconMedium),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(SentinelSpacing.XXS))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (trailing != null) {
            Box(
                modifier = Modifier.size(SentinelSize.IconMedium),
                contentAlignment = Alignment.Center
            ) {
                trailing()
            }
        }
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    showDivider: Boolean = false,
    localizeValue: Boolean = true
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SentinelSpacing.SM)
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier.size(SentinelSize.IconMedium),
                    contentAlignment = Alignment.Center
                ) {
                    icon()
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(SentinelSpacing.XXS))
                Text(
                    text = value,
                    localize = localizeValue,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        if (showDivider) {
            Spacer(modifier = Modifier.height(SentinelSpacing.XS))
            androidx.compose.material3.HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = SentinelSpacing.DividerAlpha)
            )
        }
    }
}

@Composable
fun SettingRow(
    icon: @Composable () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val desc = com.sentinel.ai.ui.i18n.localized(title) + (description?.let { ". " + com.sentinel.ai.ui.i18n.localized(it) } ?: "")
    val semanticsModifier = modifier.semantics(mergeDescendants = true) {
        contentDescription = desc
        if (onClick != null) {
            role = Role.Button
        }
    }

    val rowModifier = if (onClick != null) {
        semanticsModifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
    } else {
        semanticsModifier
    }

    Row(
        modifier = if (isPressed) {
            rowModifier.padding(SentinelSpacing.SM)
        } else {
            rowModifier.padding(vertical = SentinelSpacing.SM)
        },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SentinelSpacing.SM)
    ) {
        Box(
            modifier = Modifier.size(SentinelSize.IconMedium),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (description != null) {
                Spacer(modifier = Modifier.height(SentinelSpacing.XXS))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (trailing != null) {
            Box(
                modifier = Modifier.size(SentinelSize.IconMedium),
                contentAlignment = Alignment.Center
            ) {
                trailing()
            }
        }
    }
}

@Composable
fun EmptyState(
    icon: @Composable () -> Unit,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
        action: (@Composable () -> Unit)? = null
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(SentinelSpacing.XL),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SentinelSpacing.MD)
    ) {
        Box(
            modifier = Modifier.size(SentinelSize.IconXL * 1.5f),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }

        Spacer(modifier = Modifier.height(SentinelSpacing.SM))

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (action != null) {
            Spacer(modifier = Modifier.height(SentinelSpacing.MD))
            action()
        }
    }
}

@Composable
fun LoadingState(
    modifier: Modifier = Modifier,
    message: String? = null,
    progress: Float? = null
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        LoadingShield(
            loadingText = message,
            progress = progress
        )
    }
}

@Composable
fun ErrorState(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    secondaryAction: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(SentinelSpacing.XL),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SentinelSpacing.MD)
    ) {
        Box(
            modifier = Modifier.size(SentinelSize.IconXL),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                modifier = Modifier.size(SentinelSize.IconXL),
                tint = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(SentinelSpacing.SM))

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (onRetry != null) {
            Spacer(modifier = Modifier.height(SentinelSpacing.MD))
            ActionButton(
                text = "Retry",
                onClick = onRetry
            )
        }

        if (secondaryAction != null) {
            Spacer(modifier = Modifier.height(SentinelSpacing.SM))
            secondaryAction()
        }
    }
}
