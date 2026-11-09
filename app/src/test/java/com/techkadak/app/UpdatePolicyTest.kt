package com.techkadak.app

import com.techkadak.app.update.Release
import com.techkadak.app.update.UpdatePolicy
import com.techkadak.app.update.UpdatePolicy.Decision
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class UpdatePolicyTest {
    private fun rel(v: String, notes: String = "Kits, poster, videos") = Release(v, notes, "https://github.com/techkadak/techkadak-app/releases/download/v$v/TechKadak-v$v.apk", 5_000_000, "https://github.com/techkadak/techkadak-app/releases/tag/v$v")

    @Test fun newerReleaseShowsPopupOnEveryOpen() {
        // open 1
        assertEquals(Decision.POPUP, UpdatePolicy.decide(rel("1.3.0"), "1.2.0", 10200, dismissedThisOpen = false))
        // next open (dismiss flag naye open pe reset hota hai) -> phir popup
        assertEquals(Decision.POPUP, UpdatePolicy.decide(rel("1.3.0"), "1.2.0", 10200, dismissedThisOpen = false))
    }

    @Test fun cancelHidesOnlyForThisOpen() {
        assertEquals(Decision.NONE, UpdatePolicy.decide(rel("1.3.0"), "1.2.0", 10200, dismissedThisOpen = true))
        assertEquals(Decision.POPUP, UpdatePolicy.decide(rel("1.3.0"), "1.2.0", 10200, dismissedThisOpen = false))
    }

    @Test fun sameOrOlderVersionNone() {
        assertEquals(Decision.NONE, UpdatePolicy.decide(rel("1.2.0"), "1.2.0", 10200, false))
        assertEquals(Decision.NONE, UpdatePolicy.decide(rel("1.1.0"), "1.2.0", 10200, false))
    }

    @Test fun offlineNone() {
        assertEquals(Decision.NONE, UpdatePolicy.decide(null, "1.2.0", 10200, false))
    }

    @Test fun remoteMinIsEmergencyOnly() {
        assertEquals(Decision.FORCED, UpdatePolicy.decide(null, "1.2.0", 10200, true, remoteMinCode = 10300))
        assertEquals(Decision.NONE, UpdatePolicy.decide(null, "1.2.0", 10200, false, remoteMinCode = 0))
    }

    @Test fun notesCleaned() {
        val c = UpdatePolicy.cleanNotes("## Kya naya hai\n- **Kits**\n- Poster")
        assertEquals("• Kits\n• Poster", c)
        assertFalse(c.contains("#"))
    }
}
