package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val VaporGalaxyColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color(0xFF04101A),
    primaryContainer = Color(0xFF003847),
    onPrimaryContainer = Color(0xFF8CF4FF),

    secondary = NeonMagenta,
    onSecondary = Color(0xFF260012),
    secondaryContainer = Color(0xFF5A002E),
    onSecondaryContainer = Color(0xFFFFB2D1),

    tertiary = NeonPurple,
    onTertiary = Color(0xFF1D0033),
    tertiaryContainer = Color(0xFF450D75),
    onTertiaryContainer = Color(0xFFE7B8FF),

    background = SpaceVoid,
    onBackground = StarWhite,

    surface = NebulaSurface,
    onSurface = StarWhite,
    surfaceVariant = NebulaCard,
    onSurfaceVariant = StarDust,

    outline = CosmicBorder,
    outlineVariant = CosmicBorderGlow,

    error = DangerNeon,
    onError = Color(0xFF24000B)
)

val CyberMagentaColorScheme = darkColorScheme(
    primary = NeonMagenta,
    onPrimary = Color(0xFF260012),
    primaryContainer = Color(0xFF5A002E),
    onPrimaryContainer = Color(0xFFFFB2D1),

    secondary = NeonCyan,
    onSecondary = Color(0xFF04101A),
    secondaryContainer = Color(0xFF003847),
    onSecondaryContainer = Color(0xFF8CF4FF),

    tertiary = NeonSunset,
    onTertiary = Color(0xFF2B1000),
    tertiaryContainer = Color(0xFF6B2600),
    onTertiaryContainer = Color(0xFFFFCCAA),

    background = Color(0xFF0B0117),
    onBackground = StarWhite,
    surface = Color(0xFF16062B),
    onSurface = StarWhite,
    surfaceVariant = Color(0xFF230D42),
    onSurfaceVariant = StarDust,
    outline = Color(0xFF4A1A78),
    outlineVariant = Color(0xFF7A2BBB)
)

val RetroCyanColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color(0xFF00222B),
    primaryContainer = Color(0xFF004452),
    onPrimaryContainer = Color(0xFF80FAFF),

    secondary = NeonMint,
    onSecondary = Color(0xFF002B1E),
    secondaryContainer = Color(0xFF00543B),
    onSecondaryContainer = Color(0xFF7AFFE0),

    tertiary = NeonCoral,
    onTertiary = Color(0xFF33000E),
    tertiaryContainer = Color(0xFF66001D),
    onTertiaryContainer = Color(0xFFFFB3C1),

    background = Color(0xFF040A17),
    onBackground = StarWhite,
    surface = Color(0xFF09142B),
    onSurface = StarWhite,
    surfaceVariant = Color(0xFF102144),
    onSurfaceVariant = StarDust,
    outline = Color(0xFF1B3B6F),
    outlineVariant = Color(0xFF2E65B5)
)

val SunsetHorizonColorScheme = darkColorScheme(
    primary = NeonSunset,
    onPrimary = Color(0xFF331300),
    primaryContainer = Color(0xFF662600),
    onPrimaryContainer = Color(0xFFFFCCA3),

    secondary = NeonMagenta,
    onSecondary = Color(0xFF260012),
    secondaryContainer = Color(0xFF5A002E),
    onSecondaryContainer = Color(0xFFFFB2D1),

    tertiary = NeonYellow,
    onTertiary = Color(0xFF332E00),
    tertiaryContainer = Color(0xFF665C00),
    onTertiaryContainer = Color(0xFFFFF7A6),

    background = Color(0xFF140412),
    onBackground = StarWhite,
    surface = Color(0xFF210920),
    onSurface = StarWhite,
    surfaceVariant = Color(0xFF331130),
    onSurfaceVariant = StarDust,
    outline = Color(0xFF5E1B5A),
    outlineVariant = Color(0xFF992E92)
)

fun getPaletteColorScheme(paletteName: String) = when (paletteName) {
    "Cyber Magenta" -> CyberMagentaColorScheme
    "Retro Cyan" -> RetroCyanColorScheme
    "Sunset Horizon" -> SunsetHorizonColorScheme
    else -> VaporGalaxyColorScheme
}

@Composable
fun CosmicHubTheme(
    paletteName: String = "Neon Galaxy",
    content: @Composable () -> Unit
) {
    val colorScheme = getPaletteColorScheme(paletteName)
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
