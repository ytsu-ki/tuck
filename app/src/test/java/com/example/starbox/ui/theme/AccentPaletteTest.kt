package com.example.starbox.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccentPaletteTest {

    @Test
    fun toneExtremesAreBlackAndWhite() {
        assertEquals(0xFF000000.toInt(), TonalPalette.argb(10.0, 0.2, 0.0))
        assertEquals(0xFFFFFFFF.toInt(), TonalPalette.argb(10.0, 0.2, 100.0))
    }

    @Test
    fun roseDefaultIsARoseRed() {
        val primary = accentRoles(AccentColor.Rose, dark = false).primary
        val r = (primary shr 16) and 0xFF
        val g = (primary shr 8) and 0xFF
        val b = primary and 0xFF
        // Red-dominant with a little blue (pinkish), not orange.
        assertTrue("r=$r g=$g b=$b", r > g * 2 && r > b && b > g)
    }

    @Test
    fun everyAccentKeepsTextReadable() {
        for (accent in AccentColor.entries) {
            for (dark in listOf(false, true)) {
                val roles = accentRoles(accent, dark)
                val where = "$accent dark=$dark"
                assertReadable(where + " primary", roles.primary, roles.onPrimary, 4.5)
                assertReadable(where + " primaryContainer", roles.primaryContainer, roles.onPrimaryContainer, 4.5)
                assertReadable(where + " secondaryContainer", roles.secondaryContainer, roles.onSecondaryContainer, 4.5)
                assertReadable(where + " tertiaryContainer", roles.tertiaryContainer, roles.onTertiaryContainer, 4.5)
                assertReadable(where + " surface", roles.surface, roles.onSurface, 7.0)
                assertReadable(where + " surfaceVariant text", roles.surfaceContainerLow, roles.onSurfaceVariant, 4.5)
                assertReadable(where + " starContainer", roles.starContainer, roles.onStarContainer, 4.5)
                assertReadable(where + " star badge", roles.star, roles.onStar, 3.0)
            }
        }
    }

    private fun assertReadable(where: String, background: Int, foreground: Int, minimum: Double) {
        val ratio = TonalPalette.contrast(background, foreground)
        assertTrue("$where contrast %.2f < $minimum".format(ratio), ratio >= minimum)
    }
}
