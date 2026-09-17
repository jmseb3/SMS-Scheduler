/* Hallmark · macrostructure: Narrative Workflow · tone: utilitarian-technical · anchor hue: ultramarine
 * theme: Grid · nav: N7 numbered native work bar · footer: none (native mobile shell)
 * pre-emit critique: P5 H5 E4 S5 R5 V5
 */
package com.wonddak.sms.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFAFC6FF),
    onPrimary = Color(0xFF08266E),
    primaryContainer = Color(0xFF173B91),
    onPrimaryContainer = Color(0xFFDDE6FF),
    secondary = Color(0xFFBFC7D8),
    onSecondary = Color(0xFF252B36),
    secondaryContainer = Color(0xFF343B49),
    onSecondaryContainer = Color(0xFFE3E7EF),
    tertiary = Color(0xFF9CD6B8),
    onTertiary = Color(0xFF073822),
    background = Color(0xFF11141A),
    onBackground = Color(0xFFE6E9F0),
    surface = Color(0xFF171B22),
    onSurface = Color(0xFFE6E9F0),
    surfaceVariant = Color(0xFF252A34),
    onSurfaceVariant = Color(0xFFBAC1CE),
    outline = Color(0xFF4A5260),
    error = Color(0xFFFFB4AB),
)

private val LightColorScheme = lightColorScheme(
    primary = GridBlue,
    onPrimary = GridPaper,
    primaryContainer = GridBlueLight,
    onPrimaryContainer = Color(0xFF102A72),
    secondary = GridMuted,
    onSecondary = GridPaper,
    secondaryContainer = GridSoft,
    onSecondaryContainer = GridInk,
    tertiary = GridSuccess,
    onTertiary = GridPaper,
    tertiaryContainer = Color(0xFFD9F3E4),
    onTertiaryContainer = Color(0xFF154A32),
    background = GridPaper,
    onBackground = GridInk,
    surface = GridSurface,
    onSurface = GridInk,
    surfaceVariant = GridSoft,
    onSurfaceVariant = GridMuted,
    outline = GridRule,
    error = GridError,
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(2.dp),
    medium = RoundedCornerShape(2.dp),
    large = RoundedCornerShape(4.dp),
)

@Composable
fun SMSSchedulerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = AppShapes,
        typography = Typography,
        content = content
    )
}
