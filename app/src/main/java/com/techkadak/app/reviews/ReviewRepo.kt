package com.techkadak.app.reviews

import android.content.Context
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.techkadak.app.firebase.TkFirebase
import com.techkadak.app.stats.DeviceRecord
import com.techkadak.app.stats.Stats
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

/** Firestore access for reviews (collection "reviews") and feedback (collection "feedback"). */
object ReviewRepo {
    fun available(ctx: Context) = TkFirebase.available(ctx)

    suspend fun load(ctx: Context, slug: String): Result<List<Review>> = runCatching {
        check(available(ctx)) { "offline" }
        val snap = withTimeout(10_000) { FirebaseFirestore.getInstance().collection("reviews").whereEqualTo("slug", slug).limit(60).get().await() }
        snap.documents.mapNotNull { d ->
            val rating = d.getLong("rating")?.toInt() ?: return@mapNotNull null
            val uid = d.getString("uid") ?: return@mapNotNull null
            Review(slug, uid, rating, d.getString("text").orEmpty(), d.getString("name").orEmpty(), (d.getTimestamp("at") ?: Timestamp.now()).toDate().time)
        }.let { ReviewLogic.order(it) }
    }

    suspend fun submit(ctx: Context, slug: String, uid: String, name: String, rating: Int, text: String): Result<Unit> = runCatching {
        check(available(ctx)) { "offline" }
        val c = ReviewLogic.check(rating, text)
        if (c is ReviewLogic.Check.Bad) error(c.hi)
        val nm = ReviewLogic.clean(name, ReviewLogic.NAME_MAX)
        val doc = hashMapOf<String, Any>(
            "slug" to slug, "uid" to uid, "rating" to rating.toLong(), "text" to ReviewLogic.clean(text, ReviewLogic.TEXT_MAX),
            "name" to nm, "at" to FieldValue.serverTimestamp(),
        )
        withTimeout(10_000) { FirebaseFirestore.getInstance().collection("reviews").document(ReviewLogic.docId(slug, uid)).set(doc).await() }
        if (nm.isNotEmpty()) DeviceRecord.setTypedName(ctx, nm)
    }

    suspend fun delete(ctx: Context, slug: String, uid: String): Result<Unit> = runCatching {
        withTimeout(10_000) { FirebaseFirestore.getInstance().collection("reviews").document(ReviewLogic.docId(slug, uid)).delete().await() }
    }

    // ── feedback ──
    private fun fsp(ctx: Context) = ctx.applicationContext.getSharedPreferences("tk_feedback", Context.MODE_PRIVATE)
    fun usedToday(ctx: Context, now: Long = System.currentTimeMillis()): Int {
        val sp = fsp(ctx); val day = ReviewLogic.utcDay(now)
        return if (sp.getString("day", null) == day) sp.getInt("n", 0) else 0
    }

    suspend fun sendFeedback(ctx: Context, text: String, name: String, contact: String): Result<Unit> = runCatching {
        check(available(ctx)) { "offline" }
        val c = ReviewLogic.checkFeedback(text); if (c is ReviewLogic.Check.Bad) error(c.hi)
        val now = System.currentTimeMillis()
        val slot = ReviewLogic.nextFeedbackSlot(usedToday(ctx, now)) ?: error("Aaj ke 3 feedback ho gaye, kal phir bhejna 🙏")
        val day = ReviewLogic.utcDay(now)
        val did = Stats.deviceId(ctx)
        val nm = ReviewLogic.clean(name, ReviewLogic.NAME_MAX); val ct = ReviewLogic.clean(contact, ReviewLogic.CONTACT_MAX)
        val doc = hashMapOf<String, Any>("text" to ReviewLogic.clean(text, ReviewLogic.FEEDBACK_MAX), "did" to did, "v" to com.techkadak.app.BuildConfig.VERSION_CODE.toLong(), "at" to FieldValue.serverTimestamp())
        if (nm.isNotEmpty()) doc["name"] = nm
        if (ct.isNotEmpty()) doc["contact"] = ct
        // the rules allow slots 1..3 per install per UTC day, create-only; if a slot is taken (reinstall) try the next one
        var done = false; var last: Throwable? = null
        for (s in slot..ReviewLogic.FEEDBACK_PER_DAY) {
            try {
                withTimeout(10_000) { FirebaseFirestore.getInstance().collection("feedback").document(ReviewLogic.feedbackId(did, day, s)).set(doc).await() }
                fsp(ctx).edit().putString("day", day).putInt("n", s).apply(); done = true; break
            } catch (e: Exception) { last = e; if (e !is com.google.firebase.firestore.FirebaseFirestoreException) break }
        }
        if (!done) {
            val denied = (last as? com.google.firebase.firestore.FirebaseFirestoreException)?.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED
            error(if (denied) "Abhi feedback nahi ja pa raha ya aaj ki limit (3) ho gayi. Thodi der baad try karo 🙏" else "Internet check karo aur dobara try karo")
        }
        if (nm.isNotEmpty()) DeviceRecord.setTypedName(ctx, nm)
    }
}
