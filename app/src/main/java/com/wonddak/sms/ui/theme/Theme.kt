/* Hallmark · macrostructure: Workbench · tone: utilitarian-soft · anchor hue: teal/navy
 * theme: Cobalt-inspired · nav: N5 floating work surface · footer: none (native mobile shell)
 * pre-emit critique: P5 H4 E4 S4 R4 V4
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
    primary = TealDark,
    onPrimary = Ink,
    primaryContainer = Color(0xFF164B48),
    onPrimaryContainer = TealContainer,
    secondary = Color(0xFFE0A184),
    onSecondary = Ink,
    tertiary = Color(0xFFA8CBE2),
    background = Color(0xFF0E1E21),
    onBackground = Color(0xFFE3EFEC),
    surface = Color(0xFF12272A),
    onSurface = Color(0xFFE3EFEC),
    surfaceVariant = Color(0xFF263B3D),
    onSurfaceVariant = Color(0xFFB6C8C5),
    outline = Color(0xFF718986),
)

private val LightColorScheme = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    primaryContainer = TealContainer,
    onPrimaryContainer = Ink,
    secondary = Terracotta,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF7E4D9),
    onSecondaryContainer = Ink,
    tertiary = Color(0xFF2E6387),
    onTertiary = Color.White,
    tertiaryContainer = NavyContainer,
    onTertiaryContainer = Ink,
    background = Paper,
    onBackground = Ink,
    surface = PaperElevated,
    onSurface = Ink,
    surfaceVariant = Mist,
    onSurfaceVariant = MutedInk,
    outline = Color(0xFFB9CBC7),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
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
