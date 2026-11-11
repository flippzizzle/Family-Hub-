package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.models.CustomThemeConfig

fun getPaletteColorScheme(paletteName: String): androidx.compose.material3.ColorScheme {
    val def = getThemeDefinitionByName(paletteName)
    return createColorSchemeForTheme(def)
}

fun createCustomColorScheme(config: CustomThemeConfig): androidx.compose.material3.ColorScheme {
    val preset = getThemeDefinitionByName(config.activePaletteName)
    val primary = parseColorHex(config.primaryColorHex, preset.primary)
    val secondary = parseColorHex(config.secondaryColorHex, preset.secondary)
    val tertiary = parseColorHex(config.tertiaryColorHex, preset.tertiary)
    val bg = parseColorHex(config.backgroundColorHex, preset.background)
    val surface = parseColorHex(config.surfaceColorHex, preset.surface)
    val surfaceVar = surface.copy(alpha = 0.8f)

    return darkColorScheme(
        primary = primary,
        onPrimary = Color(0xFF04101A),
        primaryContainer = primary.copy(alpha = 0.25f),
        onPrimaryContainer = primary,

        secondary = secondary,
        onSecondary = Color(0xFF260012),
        secondaryContainer = secondary.copy(alpha = 0.25f),
        onSecondaryContainer = secondary,

        tertiary = tertiary,
        onTertiary = Color(0xFF1D0033),
        tertiaryContainer = tertiary.copy(alpha = 0.25f),
        onTertiaryContainer = tertiary,

        background = bg,
        onBackground = StarWhite,

        surface = surface,
        onSurface = StarWhite,
        surfaceVariant = surfaceVar,
        onSurfaceVariant = StarDust,

        outline = primary.copy(alpha = 0.35f),
        outlineVariant = secondary.copy(alpha = 0.5f),

        error = DangerNeon,
        onError = Color(0xFF24000B)
    )
}

@Composable
fun CosmicHubTheme(
    paletteName: String = "Neon Galaxy",
    customThemeConfig: CustomThemeConfig? = null,
    content: @Composable () -> Unit
) {
    val colorScheme = if (customThemeConfig != null && customThemeConfig.isCustomEnabled) {
        createCustomColorScheme(customThemeConfig)
    } else {
        val effectivePalette = customThemeConfig?.activePaletteName ?: paletteName
        getPaletteColorScheme(effectivePalette)
    }

    val cornerRadius = customThemeConfig?.cornerRadiusDp?.dp ?: 16.dp
    val dynamicShapes = Shapes(
        extraSmall = RoundedCornerShape(cornerRadius / 4),
        small = RoundedCornerShape(cornerRadius / 2),
        medium = RoundedCornerShape(cornerRadius),
        large = RoundedCornerShape(cornerRadius * 1.25f),
        extraLarge = RoundedCornerShape(cornerRadius * 1.5f)
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = dynamicShapes,
        content = content
    )
}
