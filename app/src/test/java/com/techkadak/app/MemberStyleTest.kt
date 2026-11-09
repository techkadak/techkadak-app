package com.techkadak.app

import com.techkadak.app.member.MemberColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class MemberStyleTest {
    private fun circ(a: Float, b: Float): Float { val d = abs(a - b) % 360f; return minOf(d, 360f - d) }

    @Test fun first500_have_no_identical_full_style() {
        val keys = (1L..500L).map { MemberColor.of(it).styleKey }
        assertEquals(500, keys.toSet().size)
    }

    @Test fun consecutive_numbers_are_far_apart_in_hue() {
        for (n in 1L..499L) {
            val d = circ(MemberColor.hue(n), MemberColor.hue(n + 1))
            assertTrue("n=$n gap=$d", d >= 60f)
        }
    }

    @Test fun same_number_always_same_style() {
        for (n in listOf(1L, 42L, 500L, 123_456L)) assertEquals(MemberColor.of(n), MemberColor.of(n))
    }

    @Test fun text_meets_contrast_on_every_first_500_card() {
        for (n in 1L..500L) {
            val s = MemberColor.of(n)
            val c = MemberColor.contrast(s.colorA, s.textOn)
            assertTrue("n=$n contrast=$c", c >= 4.5)
        }
    }

    @Test fun offline_number_is_stable_and_outside_server_range() {
        val a = MemberColor.offlineNumber("install-abc")
        assertEquals(a, MemberColor.offlineNumber("install-abc"))
        assertTrue(a >= MemberColor.OFFLINE_BASE)
        assertTrue(MemberColor.offlineNumber("install-xyz") != a)
    }

    @Test fun hsl_primaries() {
        assertEquals(0xFFFF0000L, MemberColor.hsl(0f, 1f, 0.5f))
        assertEquals(0xFF00FF00L, MemberColor.hsl(120f, 1f, 0.5f))
    }
}
