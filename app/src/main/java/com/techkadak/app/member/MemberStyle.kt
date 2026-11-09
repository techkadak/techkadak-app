package com.techkadak.app.member

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * v1.5 Welcome member card: every member number gets its own look.
 * Hue = golden angle (n * 137.508 mod 360), so consecutive numbers sit ~137.5 degrees apart.
 * Saturation, lightness, gradient angle and pattern come from fixed bands of n, so the full style
 * tuple repeats far less often than hue alone. Pure Kotlin (no Android types) so it is unit-tested on the JVM.
 */
data class MemberStyle(
    val hue: Float,
    val satIdx: Int,
    val lightIdx: Int,
    val angleIdx: Int,
    val patternIdx: Int,
    /** Gradient colours as ARGB (0xAARRGGBB). */
    val colorA: Long,
    val colorB: Long,
    /** Text colour chosen for >= 4.5:1 contrast on colorA. */
    val textOn: Long,
    val gradientAngleDeg: Float,
) {
    /** Everything that makes two cards look different: used by the uniqueness test. */
    val styleKey: String get() = "${"%.1f".format(hue)}|$satIdx|$lightIdx|$angleIdx|$patternIdx"
}

object MemberColor {
    const val GOLDEN_ANGLE = 137.508
    val SATURATIONS = floatArrayOf(0.72f, 0.84f, 0.95f)
    val LIGHTNESSES = floatArrayOf(0.42f, 0.50f, 0.58f)
    const val PATTERN_COUNT = 6
    const val MIN_CONTRAST = 4.6
    const val ANGLE_COUNT = 8

    fun hue(n: Long): Float {
        val h = (n.toDouble() * GOLDEN_ANGLE) % 360.0
        return (if (h < 0) h + 360.0 else h).toFloat()
    }

    fun of(n: Long): MemberStyle {
        val hue = hue(n)
        val satIdx = (abs(n) % 3).toInt()
        val lightIdx = ((abs(n) / 3) % 3).toInt()
        val angleIdx = ((abs(n) * 5) % ANGLE_COUNT).toInt()
        val patternIdx = ((abs(n) * 7) % PATTERN_COUNT).toInt()
        val sat = SATURATIONS[satIdx]
        var l = LIGHTNESSES[lightIdx]
        var a = hsl(hue, sat, l)
        var textOn = if (contrast(a, BLACK) >= contrast(a, WHITE)) BLACK else WHITE
        // Keep the text readable: nudge lightness away from the middle until the chosen text reaches 4.5:1.
        var steps = 0
        while (contrast(a, textOn) < MIN_CONTRAST && steps < 12) {
            l = if (textOn == BLACK) min(0.9f, l + 0.02f) else max(0.1f, l - 0.02f)
            a = hsl(hue, sat, l)
            textOn = if (contrast(a, BLACK) >= contrast(a, WHITE)) BLACK else WHITE
            steps++
        }
        // second stop: same hue pushed 28 degrees and a little lighter, so the gradient has its own colour
        val b = hsl((hue + 28f) % 360f, min(0.98f, sat + 0.05f), min(0.66f, l + 0.10f))
        return MemberStyle(hue, satIdx, lightIdx, angleIdx, patternIdx, a, b, textOn, angleIdx * 45f)
    }

    /**
     * Which number the card shows. A number already shown on this device wins (a colour the person has seen never changes);
     * then the server's sequential number; then the install-id fallback.
     */
    fun resolve(shown: Long?, server: Long?, offline: Long): Long = shown?.takeIf { it > 0L } ?: server?.takeIf { it > 0L } ?: offline

    /** Offline fallback: stable number from the install id, in a range the server counter never reaches. */
    fun offlineNumber(installId: String): Long {
        var h = 0xcbf29ce484222325uL
        for (ch in installId) { h = h xor ch.code.toULong(); h *= 0x100000001b3uL }
        return OFFLINE_BASE + (h % 9_000_000uL).toLong()
    }

    const val OFFLINE_BASE = 1_000_000L
    const val BLACK = 0xFF0A0A0AL
    const val WHITE = 0xFFFFFFFFL

    /** HSL (h in degrees, s and l in 0..1) to ARGB. */
    fun hsl(h: Float, s: Float, l: Float): Long {
        val c = (1f - abs(2f * l - 1f)) * s
        val hp = (h % 360f) / 60f
        val x = c * (1f - abs(hp % 2f - 1f))
        val (r1, g1, b1) = when {
            hp < 1f -> Triple(c, x, 0f)
            hp < 2f -> Triple(x, c, 0f)
            hp < 3f -> Triple(0f, c, x)
            hp < 4f -> Triple(0f, x, c)
            hp < 5f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        val m = l - c / 2f
        fun ch(v: Float) = ((v + m) * 255f).let { kotlin.math.round(it).toInt().coerceIn(0, 255) }
        return 0xFF000000L or (ch(r1).toLong() shl 16) or (ch(g1).toLong() shl 8) or ch(b1).toLong()
    }

    fun luminance(argb: Long): Double {
        fun lin(c: Int): Double { val v = c / 255.0; return if (v <= 0.03928) v / 12.92 else Math.pow((v + 0.055) / 1.055, 2.4) }
        val r = lin(((argb shr 16) and 0xFF).toInt()); val g = lin(((argb shr 8) and 0xFF).toInt()); val b = lin((argb and 0xFF).toInt())
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    /** WCAG contrast ratio, 1..21. */
    fun contrast(a: Long, b: Long): Double {
        val la = luminance(a); val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }
}
