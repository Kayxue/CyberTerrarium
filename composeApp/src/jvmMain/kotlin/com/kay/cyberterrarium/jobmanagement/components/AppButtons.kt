package com.kay.cyberterrarium.jobmanagement.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class AppButtonVariant {
    DEFAULT,
    PRIMARY,
    SUCCESS,
    DANGER,
    MUTED
}

@Composable
fun AppButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: AppButtonVariant = AppButtonVariant.DEFAULT,
    content: @Composable () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val buttonColors = when (variant) {
        AppButtonVariant.DEFAULT -> ButtonDefaults.buttonColors()
        AppButtonVariant.PRIMARY -> ButtonDefaults.buttonColors(
            containerColor = colors.primary,
            contentColor = colors.onPrimary
        )
        AppButtonVariant.SUCCESS -> ButtonDefaults.buttonColors(
            containerColor = colors.tertiaryContainer,
            contentColor = colors.onTertiaryContainer
        )
        AppButtonVariant.DANGER -> ButtonDefaults.buttonColors(
            containerColor = colors.errorContainer,
            contentColor = colors.onErrorContainer
        )
        AppButtonVariant.MUTED -> ButtonDefaults.buttonColors(
            containerColor = colors.surfaceVariant,
            contentColor = colors.onSurfaceVariant
        )
    }
    Button(
        onClick = onClick,
        modifier = modifier
            .defaultMinSize(minWidth = 84.dp, minHeight = 34.dp)
            .widthIn(max = 168.dp),
        enabled = enabled,
        shape = RoundedCornerShape(6.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        colors = buttonColors,
        content = { content() }
    )
}

@Composable
fun AppTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = modifier
            .defaultMinSize(minWidth = 74.dp, minHeight = 34.dp)
            .widthIn(max = 148.dp),
        enabled = enabled,
        shape = RoundedCornerShape(6.dp),
        colors = ButtonDefaults.textButtonColors(),
        content = { content() }
    )
}
