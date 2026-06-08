package com.kay.cyberterrarium.jobmanagement.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
    colors: ButtonColors? = null,
    content: @Composable () -> Unit
) {
    val colorsScheme = MaterialTheme.colorScheme
    val buttonColors = colors ?: when (variant) {
        AppButtonVariant.DEFAULT -> ButtonDefaults.buttonColors()
        AppButtonVariant.PRIMARY -> ButtonDefaults.buttonColors(
            containerColor = colorsScheme.primary,
            contentColor = colorsScheme.onPrimary
        )

        AppButtonVariant.SUCCESS -> ButtonDefaults.buttonColors(
            containerColor = colorsScheme.tertiaryContainer,
            contentColor = colorsScheme.onTertiaryContainer
        )

        AppButtonVariant.DANGER -> ButtonDefaults.buttonColors(
            containerColor = colorsScheme.errorContainer,
            contentColor = colorsScheme.onErrorContainer
        )

        AppButtonVariant.MUTED -> ButtonDefaults.buttonColors(
            containerColor = colorsScheme.surfaceVariant,
            contentColor = colorsScheme.onSurfaceVariant
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
