package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val WonderLightColorScheme = lightColorScheme(
    primary = WonderPalette.SkyBlue,
    onPrimary = Color.White,
    primaryContainer = WonderPalette.SkyLight,
    onPrimaryContainer = WonderPalette.SkyBlueDark,
    secondary = WonderPalette.CoralPop,
    onSecondary = Color.White,
    secondaryContainer = WonderPalette.CoralLight,
    onSecondaryContainer = WonderPalette.CoralPopDark,
    tertiary = WonderPalette.MintMeadow,
    onTertiary = Color.White,
    tertiaryContainer = WonderPalette.MintLight,
    onTertiaryContainer = WonderPalette.MintMeadowDark,
    background = WonderPalette.CreamBackground,
    onBackground = WonderPalette.CocoaText,
    surface = WonderPalette.WarmSurface,
    onSurface = WonderPalette.CocoaText,
    surfaceVariant = WonderPalette.SoftPeachSurface,
    onSurfaceVariant = WonderPalette.CocoaMuted,
    error = WonderPalette.CoralPopDark,
    onError = Color.White
)

private val WonderDarkColorScheme = darkColorScheme(
    primary = WonderPalette.SkyBlue,
    onPrimary = Color.White,
    primaryContainer = WonderPalette.NightCardAlt,
    onPrimaryContainer = WonderPalette.SkyLight,
    secondary = WonderPalette.CoralPop,
    onSecondary = Color.White,
    secondaryContainer = WonderPalette.NightCardAlt,
    onSecondaryContainer = WonderPalette.CoralLight,
    tertiary = WonderPalette.MintMeadow,
    onTertiary = Color.White,
    tertiaryContainer = WonderPalette.NightCardAlt,
    onTertiaryContainer = WonderPalette.MintLight,
    background = WonderPalette.NightSky,
    onBackground = WonderPalette.MoonCream,
    surface = WonderPalette.NightCard,
    onSurface = WonderPalette.MoonCream,
    surfaceVariant = WonderPalette.NightCardAlt,
    onSurfaceVariant = WonderPalette.MoonMuted,
    error = WonderPalette.CoralPop,
    onError = Color.White
)

val WonderShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

@Composable
fun WonderPlayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) WonderDarkColorScheme else WonderLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = WonderTypography,
        shapes = WonderShapes,
        content = content
    )
}
