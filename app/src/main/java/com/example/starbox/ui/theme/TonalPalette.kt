package com.example.starbox.ui.theme

import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Builds Material-style tonal palettes from a hue without any extra library.
 *
 * Colors are made in OKLCH (perceptual hue and chroma), with "tone" mapped to CIE L* the way
 * Material 3 uses it: tone 0 is black, 100 is white, and pairs ~50 tones apart are readable.
 * Chroma is reduced as needed to stay inside sRGB.
 */
object TonalPalette {

    /** ARGB color for [hueDegrees] / [chroma] (OKLCH) at [tone] (0–100). */
    fun argb(hueDegrees: Double, chroma: Double, tone: Double): Int {
        if (tone <= 0.0) return 0xFF000000.toInt()
        if (tone >= 100.0) return 0xFFFFFFFF.toInt()
        val lightness = cbrt(toneToLuminance(tone))
        // Binary search for the most chroma that still fits in sRGB at this lightness.
        var low = 0.0
        var high = chroma
        if (!inGamut(lightness, hueDegrees, high)) {
            repeat(24) {
                val mid = (low + high) / 2
                if (inGamut(lightness, hueDegrees, mid)) low = mid else high = mid
            }
            high = low
        }
        val (r, g, b) = oklchToLinearSrgb(lightness, hueDegrees, high)
        return (0xFF shl 24) or (encode(r) shl 16) or (encode(g) shl 8) or encode(b)
    }

    /** WCAG contrast ratio between two ARGB colors (1–21). */
    fun contrast(a: Int, b: Int): Double {
        val la = relativeLuminance(a)
        val lb = relativeLuminance(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    private fun toneToLuminance(tone: Double): Double {
        val ft = (tone + 16.0) / 116.0
        return if (tone > 8.0) ft.pow(3) else tone / 903.2962962962963
    }

    private fun inGamut(lightness: Double, hue: Double, chroma: Double): Boolean {
        val (r, g, b) = oklchToLinearSrgb(lightness, hue, chroma)
        val eps = 1e-4
        return r in -eps..1 + eps && g in -eps..1 + eps && b in -eps..1 + eps
    }

    private fun oklchToLinearSrgb(lightness: Double, hueDegrees: Double, chroma: Double): Triple<Double, Double, Double> {
        val h = Math.toRadians(hueDegrees)
        val a = chroma * cos(h)
        val b = chroma * sin(h)
        val l = (lightness + 0.3963377774 * a + 0.2158037573 * b).pow(3)
        val m = (lightness - 0.1055613458 * a - 0.0638541728 * b).pow(3)
        val s = (lightness - 0.0894841775 * a - 1.2914855480 * b).pow(3)
        return Triple(
            4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
            -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
            -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s,
        )
    }

    private fun encode(linear: Double): Int {
        val v = linear.coerceIn(0.0, 1.0)
        val srgb = if (v <= 0.0031308) 12.92 * v else 1.055 * v.pow(1 / 2.4) - 0.055
        return (srgb * 255).roundToInt().coerceIn(0, 255)
    }

    private fun relativeLuminance(argb: Int): Double {
        fun channel(shift: Int): Double {
            val c = ((argb shr shift) and 0xFF) / 255.0
            return if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }
}
