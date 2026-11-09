package com.techkadak.app

import com.techkadak.app.member.MemberColor
import org.junit.Assert.assertEquals
import org.junit.Test

/** v1.5 pass 3: which member number the card shows. */
class MemberNumberResolveTest {
    @Test
    fun shownNumberNeverChanges() {
        assertEquals(42L, MemberColor.resolve(shown = 42L, server = 7L, offline = 1_000_123L))
    }

    @Test
    fun serverNumberWinsOverOfflineWhenNothingShown() {
        assertEquals(7L, MemberColor.resolve(shown = null, server = 7L, offline = 1_000_123L))
    }

    @Test
    fun offlineFallbackWhenNoServerAndNothingShown() {
        assertEquals(1_000_123L, MemberColor.resolve(shown = null, server = null, offline = 1_000_123L))
        assertEquals(1_000_123L, MemberColor.resolve(shown = -1L, server = 0L, offline = 1_000_123L))
    }
}
