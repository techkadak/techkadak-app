package com.techkadak.app.reviews

import java.util.Locale

/** v1.5 reviews + feedback: pure logic (unit tested). Only REAL reviews exist: no seeded or fake data anywhere. */
data class Review(val slug: String, val uid: String, val rating: Int, val text: String, val name: String, val atMs: Long)

data class ReviewSummary(val count: Int, val average: Float, val histogram: IntArray)

object ReviewLogic {
    const val TEXT_MAX = 500
    const val NAME_MAX = 40
    const val FEEDBACK_MAX = 500
    const val CONTACT_MAX = 80
    const val FEEDBACK_PER_DAY = 3
    const val EMPTY_STATE = "Abhi koi review nahi, pehle tum likho"

    /** One review per user per app: the document id says it, and the rules enforce it. */
    fun docId(slug: String, uid: String) = "${slug}_$uid"

    fun validRating(r: Int) = r in 1..5

    sealed interface Check { data object Ok : Check; data class Bad(val hi: String, val en: String) : Check }

    fun check(rating: Int, text: String): Check = when {
        !validRating(rating) -> Check.Bad("Pehle stars do (1 se 5)", "Pick a star rating (1 to 5)")
        text.trim().length > TEXT_MAX -> Check.Bad("Review $TEXT_MAX letters se chhota rakho", "Keep the review under $TEXT_MAX characters")
        else -> Check.Ok
    }

    fun clean(s: String, max: Int): String = s.replace(Regex("[\\u0000-\\u0008\\u000b\\u000c\\u000e-\\u001f]"), " ").trim().take(max)

    fun summary(list: List<Review>): ReviewSummary {
        val h = IntArray(5)
        list.forEach { if (validRating(it.rating)) h[it.rating - 1]++ }
        val n = h.sum()
        val avg = if (n == 0) 0f else (h.indices.sumOf { (it + 1) * h[it] }).toFloat() / n
        return ReviewSummary(n, avg, h)
    }

    /** Newest first, one review per user (the latest wins even if the list somehow holds two). */
    fun order(list: List<Review>): List<Review> = list.sortedByDescending { it.atMs }.distinctBy { it.uid }

    fun avgLabel(avg: Float) = String.format(Locale.US, "%.1f", avg)

    // ── feedback: 3 per day per install, enforced in the rules by the document id ──
    fun feedbackId(installId: String, utcDay: String, n: Int) = "${installId}_${utcDay}_$n"

    /** Next free slot 1..3 for today (null = limit reached). */
    fun nextFeedbackSlot(usedToday: Int): Int? = if (usedToday in 0 until FEEDBACK_PER_DAY) usedToday + 1 else null

    fun checkFeedback(text: String): Check = when {
        text.trim().length < 5 -> Check.Bad("Thoda aur likho (kam se kam 5 letters)", "Write a little more (at least 5 characters)")
        text.trim().length > FEEDBACK_MAX -> Check.Bad("$FEEDBACK_MAX letters se chhota rakho", "Keep it under $FEEDBACK_MAX characters")
        else -> Check.Ok
    }

    fun utcDay(nowMs: Long): String = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }.format(java.util.Date(nowMs))

    fun timeAgo(nowMs: Long, atMs: Long): String {
        val s = ((nowMs - atMs) / 1000).coerceAtLeast(0)
        return when {
            s < 3600 -> "${(s / 60).coerceAtLeast(1)} min pehle"
            s < 86400 -> "${s / 3600} ghante pehle"
            s < 86400 * 30 -> "${s / 86400} din pehle"
            else -> "${s / (86400 * 30)} mahine pehle"
        }
    }
}
