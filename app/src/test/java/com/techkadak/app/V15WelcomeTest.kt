package com.techkadak.app

import com.techkadak.app.member.MemberColor
import com.techkadak.app.member.WelcomePolicy
import org.junit.Assert.*
import org.junit.Test

class V15WelcomeTest {
    @Test fun newAccountOnlyOnce() {
        assertTrue(WelcomePolicy.shouldOffer(true, false))
        assertFalse(WelcomePolicy.shouldOffer(true, true))
        assertFalse(WelcomePolicy.shouldOffer(false, false))
        assertFalse(WelcomePolicy.shouldOffer(false, true))
    }
    @Test fun replayKeepsColour() {
        // Profile replay uses the number already shown: the style must be identical every time
        val n = 77L
        assertEquals(MemberColor.of(n).styleKey, MemberColor.of(MemberColor.resolve(n, 78L, 1_000_005L)).styleKey)
    }

    @Test fun oldUserAfterUpdateSeesCardOnce() {
        val inst = "abc"
        // signed-in old user, nothing shown yet -> show, keyed by uid
        assertEquals(WelcomePolicy.Launch.Show("u1", true), WelcomePolicy.onLaunch("u1", inst, emptySet()))
        // after dismissing: never again
        assertEquals(WelcomePolicy.Launch.Skip, WelcomePolicy.onLaunch("u1", inst, setOf("u1")))
    }
    @Test fun guestKeyedByInstall() {
        assertEquals(WelcomePolicy.Launch.Show("guest_abc", false), WelcomePolicy.onLaunch(null, "abc", emptySet()))
        assertEquals(WelcomePolicy.Launch.Skip, WelcomePolicy.onLaunch(null, "abc", setOf("guest_abc")))
        // another install is unaffected
        assertEquals(WelcomePolicy.Launch.Show("guest_xyz", false), WelcomePolicy.onLaunch(null, "xyz", setOf("guest_abc")))
    }
    @Test fun existingAccountAfterGuestCardGetsNoSecond() {
        assertEquals(WelcomePolicy.Launch.MarkOnly("u1"), WelcomePolicy.onLaunch("u1", "abc", setOf("guest_abc")))
    }
    @Test fun newAccountStillGetsCardEvenIfGuestSawOne() {
        assertTrue(WelcomePolicy.shouldOffer(true, false))
    }
    @Test fun memberSinceUsesFirstSeen() {
        assertEquals("03 Oct 2026", WelcomePolicy.memberSince("2026-10-03", "05 Nov 2026"))
        assertEquals("05 Nov 2026", WelcomePolicy.memberSince(null, "05 Nov 2026"))
        assertEquals("05 Nov 2026", WelcomePolicy.memberSince("garbage", "05 Nov 2026"))
    }
}
