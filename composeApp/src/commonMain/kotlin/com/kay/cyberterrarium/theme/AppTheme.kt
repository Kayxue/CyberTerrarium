package com.kay.cyberterrarium.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cyberterrarium.composeapp.generated.resources.LXGWWenKaiMonoTC_BoldFont
import cyberterrarium.composeapp.generated.resources.LXGWWenKaiMonoTC_RegularFont
import org.jetbrains.compose.resources.Font as ResourceFont

private val WarmLightColors = lightColorScheme(
    primary = Color(0xFF6B7D5E),
    onPrimary = Color(0xFFF7F1E8),
    primaryContainer = Color(0xFFDDE6D5),
    onPrimaryContainer = Color(0xFF243021),
    secondary = Color(0xFFB98A5C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF0DBC8),
    onSecondaryContainer = Color(0xFF3B2716),
    tertiary = Color(0xFF6F8D82),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD8E6E0),
    onTertiaryContainer = Color(0xFF21342F),
    background = Color(0xFFF7F1E8),
    onBackground = Color(0xFF2A2622),
    surface = Color(0xFFFFF9F2),
    onSurface = Color(0xFF2A2622),
    surfaceVariant = Color(0xFFE9DED0),
    onSurfaceVariant = Color(0xFF5F564E),
    outline = Color(0xFFD6C8B8),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    surfaceTint = Color(0xFF6B7D5E)
)

private val DeepNightColors = darkColorScheme(
    primary = Color(0xFF86D0FF),
    onPrimary = Color(0xFF00263A),
    primaryContainer = Color(0xFF1B4A63),
    onPrimaryContainer = Color(0xFFD3EEFF),
    secondary = Color(0xFF7CC8A4),
    onSecondary = Color(0xFF003829),
    secondaryContainer = Color(0xFF134F3D),
    onSecondaryContainer = Color(0xFFB7F2D7),
    tertiary = Color(0xFFB7C7D9),
    onTertiary = Color(0xFF13202E),
    tertiaryContainer = Color(0xFF33465B),
    onTertiaryContainer = Color(0xFFE0EEF9),
    background = Color(0xFF08111C),
    onBackground = Color(0xFFE5EDF7),
    surface = Color(0xFF101B28),
    onSurface = Color(0xFFE5EDF7),
    surfaceVariant = Color(0xFF162334),
    onSurfaceVariant = Color(0xFFB6C4D3),
    outline = Color(0xFF304257),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    surfaceTint = Color(0xFF86D0FF)
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
private fun rememberHeadingFontFamily(): FontFamily {
    val regular = ResourceFont(LXGWWenKaiMonoTC_RegularFont)
    val bold = ResourceFont(LXGWWenKaiMonoTC_BoldFont)

    return remember(regular, bold) {
        FontFamily(regular, bold)
    }
}

@Composable
private fun rememberAppTypography(): Typography {
    val headingFontFamily = rememberHeadingFontFamily()

    return remember(headingFontFamily) {
        Typography(
            displayLarge = TextStyle(
                fontFamily = headingFontFamily,
                fontSize = 36.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 44.sp
            ),
            headlineLarge = TextStyle(
                fontFamily = headingFontFamily,
                fontSize = 32.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 40.sp
            ),
            headlineMedium = TextStyle(
                fontFamily = headingFontFamily,
                fontSize = 28.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 36.sp
            ),
            headlineSmall = TextStyle(
                fontFamily = headingFontFamily,
                fontSize = 24.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 32.sp
            ),
            titleLarge = TextStyle(
                fontFamily = headingFontFamily,
                fontSize = 22.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 28.sp
            ),
            titleMedium = TextStyle(
                fontFamily = headingFontFamily,
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 24.sp
            ),
            titleSmall = TextStyle(
                fontFamily = headingFontFamily,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 22.sp
            ),
            labelLarge = TextStyle(
                fontFamily = headingFontFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal
            )
        )
    }
}

@Composable
fun AppTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DeepNightColors else WarmLightColors,
        typography = rememberAppTypography(),
        shapes = AppShapes,
        content = content
    )
}
