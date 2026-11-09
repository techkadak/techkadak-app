package com.techkadak.app

import com.techkadak.app.reviews.Review
import com.techkadak.app.reviews.ReviewLogic
import org.junit.Assert.*
import org.junit.Test

class V15ReviewTest {
    private fun r(uid: String, rating: Int, at: Long) = Review("app", uid, rating, "t", "n", at)

    @Test fun emptyStateIsHonest() {
        assertEquals("Abhi koi review nahi, pehle tum likho", ReviewLogic.EMPTY_STATE)
        val s = ReviewLogic.summary(emptyList())
        assertEquals(0, s.count); assertEquals(0f, s.average, 0f)
    }

    @Test fun summaryAndOrder() {
        val l = listOf(r("a", 5, 1), r("b", 4, 3), r("c", 3, 2))
        val s = ReviewLogic.summary(l)
        assertEquals(3, s.count); assertEquals(4f, s.average, 0.001f)
        assertEquals(listOf("b", "c", "a"), ReviewLogic.order(l).map { it.uid })
        assertEquals("4.0", ReviewLogic.avgLabel(s.average))
    }

    @Test fun onePerUserPerApp() {
        assertEquals("newpipe_uid1", ReviewLogic.docId("newpipe", "uid1"))
        assertEquals(ReviewLogic.docId("a", "u"), ReviewLogic.docId("a", "u"))
        assertNotEquals(ReviewLogic.docId("a", "u"), ReviewLogic.docId("b", "u"))
        // even if two docs slipped in, the list shows only the newest per user
        val l = ReviewLogic.order(listOf(r("u", 1, 10), r("u", 5, 20)))
        assertEquals(1, l.size); assertEquals(5, l[0].rating)
    }

    @Test fun validation() {
        assertTrue(ReviewLogic.check(5, "") is ReviewLogic.Check.Ok)
        assertTrue(ReviewLogic.check(0, "x") is ReviewLogic.Check.Bad)
        assertTrue(ReviewLogic.check(6, "x") is ReviewLogic.Check.Bad)
        assertTrue(ReviewLogic.check(3, "a".repeat(501)) is ReviewLogic.Check.Bad)
        assertTrue(ReviewLogic.check(3, "a".repeat(500)) is ReviewLogic.Check.Ok)
        assertEquals("ab c", ReviewLogic.clean("ab\u0001c ", 40))
    }

    @Test fun feedbackLimitThreePerDay() {
        assertEquals(1, ReviewLogic.nextFeedbackSlot(0)); assertEquals(3, ReviewLogic.nextFeedbackSlot(2)); assertNull(ReviewLogic.nextFeedbackSlot(3))
        assertEquals("abc_2026-11-05_2", ReviewLogic.feedbackId("abc", "2026-11-05", 2))
        assertTrue(ReviewLogic.checkFeedback("hi") is ReviewLogic.Check.Bad)
        assertTrue(ReviewLogic.checkFeedback("dark mode chahiye") is ReviewLogic.Check.Ok)
        assertTrue(ReviewLogic.checkFeedback("x".repeat(501)) is ReviewLogic.Check.Bad)
        assertTrue(ReviewLogic.checkFeedback("x".repeat(500)) is ReviewLogic.Check.Ok)
        assertEquals("2026-11-05", ReviewLogic.utcDay(1793836800000L + 3600_000L))
    }

    @Test fun timeAgo() {
        assertEquals("1 min pehle", ReviewLogic.timeAgo(1000, 1000))
        assertEquals("2 ghante pehle", ReviewLogic.timeAgo(2 * 3600_000L + 5, 5))
        assertEquals("3 din pehle", ReviewLogic.timeAgo(3 * 86400_000L, 0))
    }
}
