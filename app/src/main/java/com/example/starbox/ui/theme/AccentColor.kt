package com.example.starbox.ui.theme

import androidx.annotation.StringRes
import com.example.starbox.R

/**
 * Accent colors the user can pick. Each one drives the whole palette: primary, softer
 * secondary, a neighbouring-hue tertiary, and neutrals faintly tinted toward the accent.
 * [Wallpaper] uses Android 12+ dynamic color instead.
 */
enum class AccentColor(val hue: Double, val chroma: Double, @param:StringRes val label: Int) {
    Rose(hue = 10.0, chroma = 0.21, label = R.string.accent_rose),
    Coral(hue = 38.0, chroma = 0.17, label = R.string.accent_coral),
    Amber(hue = 72.0, chroma = 0.16, label = R.string.accent_amber),
    Green(hue = 150.0, chroma = 0.15, label = R.string.accent_green),
    Teal(hue = 195.0, chroma = 0.12, label = R.string.accent_teal),
    Blue(hue = 255.0, chroma = 0.16, label = R.string.accent_blue),
    Violet(hue = 292.0, chroma = 0.18, label = R.string.accent_violet),
    Orchid(hue = 335.0, chroma = 0.18, label = R.string.accent_orchid),
    Wallpaper(hue = 0.0, chroma = 0.0, label = R.string.accent_wallpaper),
    ;

    val isDynamic: Boolean get() = this == Wallpaper

    companion object {
        val DEFAULT = Rose

        fun fromName(name: String?): AccentColor = entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}

/** Every color role as ARGB, so the palette can be unit tested without Compose. */
data class AccentRoles(
    val primary: Int, val onPrimary: Int, val primaryContainer: Int, val onPrimaryContainer: Int,
    val inversePrimary: Int,
    val secondary: Int, val onSecondary: Int, val secondaryContainer: Int, val onSecondaryContainer: Int,
    val tertiary: Int, val onTertiary: Int, val tertiaryContainer: Int, val onTertiaryContainer: Int,
    val background: Int, val onBackground: Int, val surface: Int, val onSurface: Int,
    val surfaceVariant: Int, val onSurfaceVariant: Int,
    val surfaceContainerLowest: Int, val surfaceContainerLow: Int, val surfaceContainer: Int,
    val surfaceContainerHigh: Int, val surfaceContainerHighest: Int,
    val inverseSurface: Int, val inverseOnSurface: Int,
    val outline: Int, val outlineVariant: Int,
    /** Vivid accent for unread dots, badges and star pills. */
    val star: Int, val onStar: Int, val starContainer: Int, val onStarContainer: Int,
)

/**
 * Clean, untinted backgrounds: white in light mode, the familiar #121212 in dark mode, with
 * cards and inputs stepping up in clear, neutral layers. Shared by every accent and by
 * wallpaper colors, so only the accent changes between choices.
 */
object CleanNeutrals {
    data class Set(
        val background: Int, val onSurface: Int, val surfaceVariant: Int, val onSurfaceVariant: Int,
        val lowest: Int, val low: Int, val container: Int, val high: Int, val highest: Int,
        val bright: Int, val dim: Int, val inverseSurface: Int, val inverseOnSurface: Int,
        val outline: Int, val outlineVariant: Int,
    )

    val light = Set(
        background = 0xFFFFFFFF.toInt(), onSurface = 0xFF18181B.toInt(),
        surfaceVariant = 0xFFE4E4E7.toInt(), onSurfaceVariant = 0xFF52525B.toInt(),
        lowest = 0xFFFFFFFF.toInt(), low = 0xFFF4F4F5.toInt(), container = 0xFFEFEFF1.toInt(),
        high = 0xFFE8E8EB.toInt(), highest = 0xFFE2E2E5.toInt(),
        bright = 0xFFFFFFFF.toInt(), dim = 0xFFE4E4E7.toInt(),
        inverseSurface = 0xFF27272A.toInt(), inverseOnSurface = 0xFFF4F4F5.toInt(),
        outline = 0xFF71717A.toInt(), outlineVariant = 0xFFE0E0E3.toInt(),
    )

    val dark = Set(
        background = 0xFF121212.toInt(), onSurface = 0xFFECECEE.toInt(),
        surfaceVariant = 0xFF2C2C2F.toInt(), onSurfaceVariant = 0xFFA1A1AA.toInt(),
        lowest = 0xFF0C0C0C.toInt(), low = 0xFF1C1C1E.toInt(), container = 0xFF222224.toInt(),
        high = 0xFF2A2A2D.toInt(), highest = 0xFF333336.toInt(),
        bright = 0xFF3A3A3E.toInt(), dim = 0xFF121212.toInt(),
        inverseSurface = 0xFFE4E4E7.toInt(), inverseOnSurface = 0xFF27272A.toInt(),
        outline = 0xFF8A8A93.toInt(), outlineVariant = 0xFF333336.toInt(),
    )

    fun of(dark: Boolean) = if (dark) this.dark else light
}

/** Material 3 tone mapping applied to palettes derived from the accent's hue, on clean neutrals. */
fun accentRoles(accent: AccentColor, dark: Boolean): AccentRoles {
    val base = if (accent.isDynamic) AccentColor.DEFAULT else accent
    val hue = base.hue
    fun p(tone: Double) = TonalPalette.argb(hue, base.chroma, tone)
    fun s(tone: Double) = TonalPalette.argb(hue, base.chroma * 0.2, tone)
    fun t(tone: Double) = TonalPalette.argb((hue + 55.0) % 360.0, base.chroma * 0.65, tone)
    val n = CleanNeutrals.of(dark)

    return if (!dark) {
        AccentRoles(
            // Tone 44 instead of Material's 40: livelier, still >4.5:1 with white text.
            primary = p(44.0), onPrimary = p(100.0), primaryContainer = p(90.0), onPrimaryContainer = p(10.0),
            inversePrimary = p(80.0),
            secondary = s(40.0), onSecondary = s(100.0), secondaryContainer = s(90.0), onSecondaryContainer = s(10.0),
            tertiary = t(40.0), onTertiary = t(100.0), tertiaryContainer = t(90.0), onTertiaryContainer = t(10.0),
            background = n.background, onBackground = n.onSurface, surface = n.background, onSurface = n.onSurface,
            surfaceVariant = n.surfaceVariant, onSurfaceVariant = n.onSurfaceVariant,
            surfaceContainerLowest = n.lowest, surfaceContainerLow = n.low, surfaceContainer = n.container,
            surfaceContainerHigh = n.high, surfaceContainerHighest = n.highest,
            inverseSurface = n.inverseSurface, inverseOnSurface = n.inverseOnSurface,
            outline = n.outline, outlineVariant = n.outlineVariant,
            star = p(52.0), onStar = p(100.0), starContainer = p(92.0), onStarContainer = p(15.0),
        )
    } else {
        AccentRoles(
            primary = p(80.0), onPrimary = p(20.0), primaryContainer = p(30.0), onPrimaryContainer = p(90.0),
            inversePrimary = p(40.0),
            secondary = s(80.0), onSecondary = s(20.0), secondaryContainer = s(30.0), onSecondaryContainer = s(90.0),
            tertiary = t(80.0), onTertiary = t(20.0), tertiaryContainer = t(30.0), onTertiaryContainer = t(90.0),
            background = n.background, onBackground = n.onSurface, surface = n.background, onSurface = n.onSurface,
            surfaceVariant = n.surfaceVariant, onSurfaceVariant = n.onSurfaceVariant,
            surfaceContainerLowest = n.lowest, surfaceContainerLow = n.low, surfaceContainer = n.container,
            surfaceContainerHigh = n.high, surfaceContainerHighest = n.highest,
            inverseSurface = n.inverseSurface, inverseOnSurface = n.inverseOnSurface,
            outline = n.outline, outlineVariant = n.outlineVariant,
            star = p(72.0), onStar = p(10.0), starContainer = p(28.0), onStarContainer = p(92.0),
        )
    }
}
