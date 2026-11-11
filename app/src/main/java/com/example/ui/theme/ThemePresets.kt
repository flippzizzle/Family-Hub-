package com.example.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

data class PresetThemeDefinition(
    val name: String,
    val category: String, // Cosmic, Vaporwave, Cyberpunk, Retro, Dark, Pastel
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val description: String = ""
)

val PRESET_THEMES = listOf(
    // 1. Original 4 Cosmic Palettes
    PresetThemeDefinition(
        name = "Neon Galaxy",
        category = "Cosmic",
        primary = Color(0xFF00F5FF),
        secondary = Color(0xFFFF007F),
        tertiary = Color(0xFFB026FF),
        background = Color(0xFF080114),
        surface = Color(0xFF120826),
        surfaceVariant = Color(0xFF1C0D38),
        description = "Vaporwave signature galaxy with electric cyan, neon magenta, and deep nebula purple"
    ),
    PresetThemeDefinition(
        name = "Cyber Magenta",
        category = "Cyberpunk",
        primary = Color(0xFFFF007F),
        secondary = Color(0xFF00F5FF),
        tertiary = Color(0xFFFF7A00),
        background = Color(0xFF0B0117),
        surface = Color(0xFF16062B),
        surfaceVariant = Color(0xFF230D42),
        description = "High-voltage neon fuchsia with cyan accents and warm cyberpunk amber"
    ),
    PresetThemeDefinition(
        name = "Retro Cyan",
        category = "Retro",
        primary = Color(0xFF00F5FF),
        secondary = Color(0xFF00FFB2),
        tertiary = Color(0xFFFF4D6D),
        background = Color(0xFF040A17),
        surface = Color(0xFF09142B),
        surfaceVariant = Color(0xFF102144),
        description = "Cool electric turquoise and mint with retro arcade coral"
    ),
    PresetThemeDefinition(
        name = "Sunset Horizon",
        category = "Vaporwave",
        primary = Color(0xFFFF7A00),
        secondary = Color(0xFFFF007F),
        tertiary = Color(0xFFFFE600),
        background = Color(0xFF140412),
        surface = Color(0xFF210920),
        surfaceVariant = Color(0xFF331130),
        description = "1980s palm sunset with glowing solar orange and hot fuchsia"
    ),

    // 5 - 10: Vaporwave & Synthwave
    PresetThemeDefinition(
        name = "Synthwave 1984",
        category = "Vaporwave",
        primary = Color(0xFFFF2A85),
        secondary = Color(0xFF00E5FF),
        tertiary = Color(0xFFFFD600),
        background = Color(0xFF0C0721),
        surface = Color(0xFF170E38),
        surfaceVariant = Color(0xFF241552),
        description = "Classic outrun neon grid aesthetic with laser pink and cyan"
    ),
    PresetThemeDefinition(
        name = "Miami Vice",
        category = "Vaporwave",
        primary = Color(0xFFFF6EB4),
        secondary = Color(0xFF00F0FF),
        tertiary = Color(0xFF7000FF),
        background = Color(0xFF090D1A),
        surface = Color(0xFF131A30),
        surfaceVariant = Color(0xFF1D2847),
        description = "Pastel flamingo neon and tropical sea ocean teal"
    ),
    PresetThemeDefinition(
        name = "Outrun Grid",
        category = "Vaporwave",
        primary = Color(0xFF9D00FF),
        secondary = Color(0xFFFF3366),
        tertiary = Color(0xFF00F0FF),
        background = Color(0xFF050014),
        surface = Color(0xFF0F002E),
        surfaceVariant = Color(0xFF1C004D),
        description = "Endless neon horizon grid with ultraviolet and electric crimson"
    ),
    PresetThemeDefinition(
        name = "Vapor Palms",
        category = "Vaporwave",
        primary = Color(0xFFFF7597),
        secondary = Color(0xFF5CE1E6),
        tertiary = Color(0xFFFFB347),
        background = Color(0xFF0A0F1D),
        surface = Color(0xFF141C33),
        surfaceVariant = Color(0xFF1F2B4A),
        description = "Dreamy Japanese lo-fi aesthetic with soft sunset pink and aqua"
    ),
    PresetThemeDefinition(
        name = "Arcade Dreams",
        category = "Retro",
        primary = Color(0xFF00FF66),
        secondary = Color(0xFFFF0066),
        tertiary = Color(0xFF00FFFF),
        background = Color(0xFF0A0A1E),
        surface = Color(0xFF141433),
        surfaceVariant = Color(0xFF1F1F4D),
        description = "Coin-op pixel glow with phosphorescent green and coin magenta"
    ),
    PresetThemeDefinition(
        name = "Cassette Tape",
        category = "Retro",
        primary = Color(0xFFFF9900),
        secondary = Color(0xFF00CCCC),
        tertiary = Color(0xFFFF3366),
        background = Color(0xFF121014),
        surface = Color(0xFF1F1B24),
        surfaceVariant = Color(0xFF2E2936),
        description = "Vintage 80s boombox with warm magnetic amber and tape teal"
    ),

    // 11 - 18: Cyberpunk & Matrix
    PresetThemeDefinition(
        name = "Midnight Tokyo",
        category = "Cyberpunk",
        primary = Color(0xFFFF1744),
        secondary = Color(0xFF00E5FF),
        tertiary = Color(0xFFFFEA00),
        background = Color(0xFF0A0A10),
        surface = Color(0xFF14141E),
        surfaceVariant = Color(0xFF20202F),
        description = "Rain-soaked neon alleyways of Shinjuku with crimson neon"
    ),
    PresetThemeDefinition(
        name = "Laser Matrix",
        category = "Cyberpunk",
        primary = Color(0xFF00FF41),
        secondary = Color(0xFF008F11),
        tertiary = Color(0xFF00FF88),
        background = Color(0xFF020D04),
        surface = Color(0xFF061A0A),
        surfaceVariant = Color(0xFF0B2B12),
        description = "Terminal green phosphor glyphs drifting through black cyber void"
    ),
    PresetThemeDefinition(
        name = "Cyberpunk 2077",
        category = "Cyberpunk",
        primary = Color(0xFFFCEE09),
        secondary = Color(0xFF00F0FF),
        tertiary = Color(0xFFFF003C),
        background = Color(0xFF0D0D11),
        surface = Color(0xFF18181F),
        surfaceVariant = Color(0xFF252530),
        description = "High-contrast samurai yellow with cyber cyan and warning red"
    ),
    PresetThemeDefinition(
        name = "Quantum Realm",
        category = "Cyberpunk",
        primary = Color(0xFF00FFFF),
        secondary = Color(0xFFE000FF),
        tertiary = Color(0xFF0066FF),
        background = Color(0xFF060417),
        surface = Color(0xFF0E0B2B),
        surfaceVariant = Color(0xFF171342),
        description = "Subatomic glow with quantum cyan and particle plasma violet"
    ),
    PresetThemeDefinition(
        name = "Hyperdrive",
        category = "Cyberpunk",
        primary = Color(0xFF0099FF),
        secondary = Color(0xFFFF1A75),
        tertiary = Color(0xFF00FFCC),
        background = Color(0xFF040614),
        surface = Color(0xFF0A0F29),
        surfaceVariant = Color(0xFF10193D),
        description = "Warp-speed relativistic blues with ion thruster pink"
    ),
    PresetThemeDefinition(
        name = "Chroma Waves",
        category = "Cyberpunk",
        primary = Color(0xFF8A2BE2),
        secondary = Color(0xFF00FFAA),
        tertiary = Color(0xFFFF1493),
        background = Color(0xFF070517),
        surface = Color(0xFF120E30),
        surfaceVariant = Color(0xFF1E174A),
        description = "Prismatic chromatic aberration with blue-violet and spectral neon"
    ),
    PresetThemeDefinition(
        name = "Electric Oasis",
        category = "Cyberpunk",
        primary = Color(0xFF00FFA3),
        secondary = Color(0xFF00B8FF),
        tertiary = Color(0xFFFFE600),
        background = Color(0xFF021214),
        surface = Color(0xFF062124),
        surfaceVariant = Color(0xFF0C3338),
        description = "Bioluminescent cybernetic lagoon with emerald and vivid electric cyan"
    ),
    PresetThemeDefinition(
        name = "Hologram Horizon",
        category = "Cyberpunk",
        primary = Color(0xFF70D6FF),
        secondary = Color(0xFFFF70A6),
        tertiary = Color(0xFFFF9770),
        background = Color(0xFF080C1A),
        surface = Color(0xFF11182E),
        surfaceVariant = Color(0xFF1A2442),
        description = "Flickering holographic projection with pastel neon translucence"
    ),

    // 19 - 26: Cosmic & Celestial Deep Space
    PresetThemeDefinition(
        name = "Cosmic Aurora",
        category = "Cosmic",
        primary = Color(0xFF00FFA3),
        secondary = Color(0xFF7928CA),
        tertiary = Color(0xFF0070F3),
        background = Color(0xFF050B14),
        surface = Color(0xFF0D1726),
        surfaceVariant = Color(0xFF16243B),
        description = "Northern polar lights dancing over deep arctic midnight"
    ),
    PresetThemeDefinition(
        name = "Deep Space Void",
        category = "Cosmic",
        primary = Color(0xFF00D2FF),
        secondary = Color(0xFF9D50BB),
        tertiary = Color(0xFF6E48AA),
        background = Color(0xFF030108),
        surface = Color(0xFF0A0517),
        surfaceVariant = Color(0xFF140D26),
        description = "Absolute cosmic darkness punctuated by distant supernova stars"
    ),
    PresetThemeDefinition(
        name = "Nebula Dream",
        category = "Cosmic",
        primary = Color(0xFFFF61D2),
        secondary = Color(0xFFFE90AF),
        tertiary = Color(0xFF5BE7FF),
        background = Color(0xFF0E0720),
        surface = Color(0xFF1A0E38),
        surfaceVariant = Color(0xFF281752),
        description = "Interstellar gas cloud illuminated with luminous magenta and cyan"
    ),
    PresetThemeDefinition(
        name = "Solar Flare",
        category = "Cosmic",
        primary = Color(0xFFFF5E00),
        secondary = Color(0xFFFFB300),
        tertiary = Color(0xFFFF0055),
        background = Color(0xFF140502),
        surface = Color(0xFF240C06),
        surfaceVariant = Color(0xFF38160B),
        description = "Coronal mass ejection glowing with molten orange and solar gold"
    ),
    PresetThemeDefinition(
        name = "Supernova",
        category = "Cosmic",
        primary = Color(0xFFFF0055),
        secondary = Color(0xFF00F5FF),
        tertiary = Color(0xFFFFF000),
        background = Color(0xFF12030C),
        surface = Color(0xFF210918),
        surfaceVariant = Color(0xFF331027),
        description = "Dying star explosion erupting into blinding cosmic radiance"
    ),
    PresetThemeDefinition(
        name = "Galactic Core",
        category = "Cosmic",
        primary = Color(0xFFFF8008),
        secondary = Color(0xFFFFC837),
        tertiary = Color(0xFF9D00FF),
        background = Color(0xFF120700),
        surface = Color(0xFF241004),
        surfaceVariant = Color(0xFF381B09),
        description = "Swirling gravitational center of the galaxy with superheated gas"
    ),
    PresetThemeDefinition(
        name = "Emerald Nebula",
        category = "Cosmic",
        primary = Color(0xFF00FF87),
        secondary = Color(0xFF60EFFF),
        tertiary = Color(0xFF00A86B),
        background = Color(0xFF02120A),
        surface = Color(0xFF052115),
        surfaceVariant = Color(0xFF0A3322),
        description = "Deep celestial jade nebula with ionised oxygen turquoise glow"
    ),
    PresetThemeDefinition(
        name = "Dark Matter",
        category = "Cosmic",
        primary = Color(0xFFB388FF),
        secondary = Color(0xFF7C4DFF),
        tertiary = Color(0xFF651FFF),
        background = Color(0xFF05050A),
        surface = Color(0xFF0C0C17),
        surfaceVariant = Color(0xFF151524),
        description = "Invisible cosmic structure mapped in eerie violet gravitational ripples"
    ),

    // 27 - 35: Retro Future, Pastel & Velvet
    PresetThemeDefinition(
        name = "Digital Twilight",
        category = "Retro",
        primary = Color(0xFFB39DDB),
        secondary = Color(0xFF80DEEA),
        tertiary = Color(0xFFFFAB91),
        background = Color(0xFF0F0B1A),
        surface = Color(0xFF1A142B),
        surfaceVariant = Color(0xFF261F3D),
        description = "Soft purple-blue dusk with serene lavender and glowing teal"
    ),
    PresetThemeDefinition(
        name = "Velvet Eclipse",
        category = "Retro",
        primary = Color(0xFFFFD700),
        secondary = Color(0xFFE040FB),
        tertiary = Color(0xFFFF5252),
        background = Color(0xFF120517),
        surface = Color(0xFF1E0C26),
        surfaceVariant = Color(0xFF2E153B),
        description = "Royal gold halo encircling an eclipsed violet celestial body"
    ),
    PresetThemeDefinition(
        name = "Starlight Prism",
        category = "Retro",
        primary = Color(0xFFD4AF37),
        secondary = Color(0xFF00E5FF),
        tertiary = Color(0xFFFF4081),
        background = Color(0xFF0A0D17),
        surface = Color(0xFF131A2B),
        surfaceVariant = Color(0xFF1E283F),
        description = "Pure optical white refracted into starlight gold and prismatic hues"
    ),
    PresetThemeDefinition(
        name = "Sunset Boulevard",
        category = "Vaporwave",
        primary = Color(0xFFFF6F61),
        secondary = Color(0xFFFFD166),
        tertiary = Color(0xFF6B5B95),
        background = Color(0xFF1A0B14),
        surface = Color(0xFF291523),
        surfaceVariant = Color(0xFF3D2136),
        description = "Warm California sunset with retro coral, warm honey, and twilight mauve"
    ),
    PresetThemeDefinition(
        name = "Lo-Fi Beats",
        category = "Pastel",
        primary = Color(0xFFFFB4A2),
        secondary = Color(0xFFB5E48C),
        tertiary = Color(0xFFE5989B),
        background = Color(0xFF1A1412),
        surface = Color(0xFF261E1A),
        surfaceVariant = Color(0xFF382D28),
        description = "Warm coffee shop study session with cozy peach and soft matcha"
    ),
    PresetThemeDefinition(
        name = "Pastel Vapor",
        category = "Pastel",
        primary = Color(0xFFB8C0FF),
        secondary = Color(0xFFFFC6FF),
        tertiary = Color(0xFFC8B6FF),
        background = Color(0xFF0F0D1C),
        surface = Color(0xFF1A162E),
        surfaceVariant = Color(0xFF272242),
        description = "Gentle dreamy pastel lavender, candy pink, and soft baby blue"
    ),
    PresetThemeDefinition(
        name = "Neon Tropic",
        category = "Vaporwave",
        primary = Color(0xFFFF1361),
        secondary = Color(0xFFFFF800),
        tertiary = Color(0xFF00F0FF),
        background = Color(0xFF0B1410),
        surface = Color(0xFF12211C),
        surfaceVariant = Color(0xFF1B3029),
        description = "Fluorescent jungle with wild passionfruit pink and canary yellow"
    ),
    PresetThemeDefinition(
        name = "Retro Future",
        category = "Retro",
        primary = Color(0xFF00ADB5),
        secondary = Color(0xFFFF2E63),
        tertiary = Color(0xFFEAEAEA),
        background = Color(0xFF11141E),
        surface = Color(0xFF1A2030),
        surfaceVariant = Color(0xFF252D42),
        description = "Sleek 1960s space-age industrial design with teal and crimson"
    ),
    PresetThemeDefinition(
        name = "Neon Boulevard",
        category = "Cyberpunk",
        primary = Color(0xFFFF00A0),
        secondary = Color(0xFF00F9FF),
        tertiary = Color(0xFFFFEE00),
        background = Color(0xFF090014),
        surface = Color(0xFF140029),
        surfaceVariant = Color(0xFF220042),
        description = "Ultra-saturated neon city avenue glowing through the dark midnight"
    )
)

fun getThemeDefinitionByName(name: String): PresetThemeDefinition {
    return PRESET_THEMES.firstOrNull { it.name.equals(name, ignoreCase = true) }
        ?: PRESET_THEMES.first()
}

fun createColorSchemeForTheme(preset: PresetThemeDefinition): androidx.compose.material3.ColorScheme {
    return darkColorScheme(
        primary = preset.primary,
        onPrimary = Color(0xFF04101A),
        primaryContainer = preset.primary.copy(alpha = 0.25f),
        onPrimaryContainer = preset.primary,

        secondary = preset.secondary,
        onSecondary = Color(0xFF260012),
        secondaryContainer = preset.secondary.copy(alpha = 0.25f),
        onSecondaryContainer = preset.secondary,

        tertiary = preset.tertiary,
        onTertiary = Color(0xFF1D0033),
        tertiaryContainer = preset.tertiary.copy(alpha = 0.25f),
        onTertiaryContainer = preset.tertiary,

        background = preset.background,
        onBackground = StarWhite,

        surface = preset.surface,
        onSurface = StarWhite,
        surfaceVariant = preset.surfaceVariant,
        onSurfaceVariant = StarDust,

        outline = preset.primary.copy(alpha = 0.35f),
        outlineVariant = preset.secondary.copy(alpha = 0.5f),

        error = DangerNeon,
        onError = Color(0xFF24000B)
    )
}

fun parseColorHex(hex: String, defaultColor: Color): Color {
    return try {
        val clean = hex.removePrefix("#")
        val full = when (clean.length) {
            6 -> "FF$clean"
            8 -> clean
            else -> return defaultColor
        }
        Color(full.toLong(16))
    } catch (_: Exception) {
        defaultColor
    }
}
