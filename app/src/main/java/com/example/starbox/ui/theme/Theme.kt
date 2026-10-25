package com.example.starbox.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The vivid accent for star highlights (unread dots, badges, rule pills, pinned memos). */
@Immutable
data class StarColors(val star: Color, val onStar: Color, val starContainer: Color, val onStarContainer: Color)

val LocalStarColors = staticCompositionLocalOf {
    accentRoles(AccentColor.DEFAULT, dark = false).toStarColors()
}

/** The resolved light/dark choice (which may differ from the phone's when the user picked one). */
val LocalDarkTheme = staticCompositionLocalOf { false }

private val BaseTypography = Typography()

/** Bolder titles, roomier body text; metadata stays small. */
private val StarBoxTypography = BaseTypography.copy(
    headlineSmall = BaseTypography.headlineSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 32.sp),
    titleLarge = BaseTypography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = BaseTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold, lineHeight = 22.sp),
    titleSmall = BaseTypography.titleSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    bodyLarge = BaseTypography.bodyLarge.copy(lineHeight = 25.sp),
    bodyMedium = BaseTypography.bodyMedium.copy(lineHeight = 21.sp),
    bodySmall = BaseTypography.bodySmall.copy(lineHeight = 18.sp),
    labelLarge = BaseTypography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
)

private val StarBoxShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** Secondary text such as timestamps and metadata: quieter than onSurfaceVariant. */
val ColorScheme.muted: Color get() = onSurfaceVariant.copy(alpha = 0.72f)

/** Whether [AccentColor.Wallpaper] can be offered (dynamic color needs Android 12+). */
val wallpaperColorsAvailable: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

@Composable
fun StarBoxTheme(
    accent: AccentColor = AccentColor.DEFAULT,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val useWallpaper = accent.isDynamic && wallpaperColorsAvailable
    val (colorScheme, starColors) = remember(accent, darkTheme, useWallpaper) {
        if (useWallpaper) {
            // Wallpaper accents, but the same clean backgrounds as every other choice.
            val scheme = (if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context))
                .withCleanNeutrals(darkTheme)
            scheme to StarColors(scheme.primary, scheme.onPrimary, scheme.primaryContainer, scheme.onPrimaryContainer)
        } else {
            val roles = accentRoles(accent, darkTheme)
            roles.toColorScheme(darkTheme) to roles.toStarColors()
        }
    }
    CompositionLocalProvider(LocalStarColors provides starColors, LocalDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = StarBoxTypography,
            shapes = StarBoxShapes,
            content = content,
        )
    }
}

object StarBoxTheme {
    val star: StarColors
        @Composable get() = LocalStarColors.current

    val isDark: Boolean
        @Composable get() = LocalDarkTheme.current

    /**
     * Sheets, dialogs and menus: crisp white in light mode; one step up from the background in
     * dark mode so they stand out from the dimmed page behind them.
     */
    val floating: Color
        @Composable get() = if (LocalDarkTheme.current) {
            MaterialTheme.colorScheme.surfaceContainerLow
        } else {
            MaterialTheme.colorScheme.surface
        }
}

private fun ColorScheme.withCleanNeutrals(dark: Boolean): ColorScheme {
    val n = CleanNeutrals.of(dark)
    val c = { argb: Int -> Color(argb) }
    return copy(
        background = c(n.background), onBackground = c(n.onSurface),
        surface = c(n.background), onSurface = c(n.onSurface),
        surfaceVariant = c(n.surfaceVariant), onSurfaceVariant = c(n.onSurfaceVariant),
        surfaceContainerLowest = c(n.lowest), surfaceContainerLow = c(n.low), surfaceContainer = c(n.container),
        surfaceContainerHigh = c(n.high), surfaceContainerHighest = c(n.highest),
        surfaceBright = c(n.bright), surfaceDim = c(n.dim),
        inverseSurface = c(n.inverseSurface), inverseOnSurface = c(n.inverseOnSurface),
        outline = c(n.outline), outlineVariant = c(n.outlineVariant),
    )
}

private fun AccentRoles.toStarColors() = StarColors(Color(star), Color(onStar), Color(starContainer), Color(onStarContainer))

private fun AccentRoles.toColorScheme(dark: Boolean): ColorScheme {
    val c = { argb: Int -> Color(argb) }
    return if (dark) {
        darkColorScheme(
            primary = c(primary), onPrimary = c(onPrimary),
            primaryContainer = c(primaryContainer), onPrimaryContainer = c(onPrimaryContainer),
            inversePrimary = c(inversePrimary),
            secondary = c(secondary), onSecondary = c(onSecondary),
            secondaryContainer = c(secondaryContainer), onSecondaryContainer = c(onSecondaryContainer),
            tertiary = c(tertiary), onTertiary = c(onTertiary),
            tertiaryContainer = c(tertiaryContainer), onTertiaryContainer = c(onTertiaryContainer),
            background = c(background), onBackground = c(onBackground),
            surface = c(surface), onSurface = c(onSurface),
            surfaceVariant = c(surfaceVariant), onSurfaceVariant = c(onSurfaceVariant),
            surfaceTint = c(primary),
            inverseSurface = c(inverseSurface), inverseOnSurface = c(inverseOnSurface),
            outline = c(outline), outlineVariant = c(outlineVariant),
            surfaceBright = c(CleanNeutrals.dark.bright), surfaceDim = c(CleanNeutrals.dark.dim),
            surfaceContainerLowest = c(surfaceContainerLowest), surfaceContainerLow = c(surfaceContainerLow),
            surfaceContainer = c(surfaceContainer), surfaceContainerHigh = c(surfaceContainerHigh),
            surfaceContainerHighest = c(surfaceContainerHighest),
        )
    } else {
        lightColorScheme(
            primary = c(primary), onPrimary = c(onPrimary),
            primaryContainer = c(primaryContainer), onPrimaryContainer = c(onPrimaryContainer),
            inversePrimary = c(inversePrimary),
            secondary = c(secondary), onSecondary = c(onSecondary),
            secondaryContainer = c(secondaryContainer), onSecondaryContainer = c(onSecondaryContainer),
            tertiary = c(tertiary), onTertiary = c(onTertiary),
            tertiaryContainer = c(tertiaryContainer), onTertiaryContainer = c(onTertiaryContainer),
            background = c(background), onBackground = c(onBackground),
            surface = c(surface), onSurface = c(onSurface),
            surfaceVariant = c(surfaceVariant), onSurfaceVariant = c(onSurfaceVariant),
            surfaceTint = c(primary),
            inverseSurface = c(inverseSurface), inverseOnSurface = c(inverseOnSurface),
            outline = c(outline), outlineVariant = c(outlineVariant),
            surfaceBright = c(CleanNeutrals.light.bright), surfaceDim = c(CleanNeutrals.light.dim),
            surfaceContainerLowest = c(surfaceContainerLowest), surfaceContainerLow = c(surfaceContainerLow),
            surfaceContainer = c(surfaceContainer), surfaceContainerHigh = c(surfaceContainerHigh),
            surfaceContainerHighest = c(surfaceContainerHighest),
        )
    }
}
