package com.techkadak.app.member

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Transaction
import com.techkadak.app.firebase.TkFirebase
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * v1.5 pass 3: sequential member number.
 * counters/members.n is bumped only inside a transaction that also creates members/{uid} once (rules in docs/firebase-v1.5).
 * Offline, timeout, rules not deployed, or Firebase off -> null, and the caller uses the install-id fallback.
 */
object MemberNumber {
    private const val TIMEOUT_MS = 5_000L

    suspend fun claim(context: Context, uid: String, name: String): Long? {
        if (!TkFirebase.available(context)) return null
        return withTimeoutOrNull(TIMEOUT_MS) {
            runCatching {
                val db = FirebaseFirestore.getInstance()
                val mine = db.collection("members").document(uid)
                val counter = db.collection("counters").document("members")
                val task: Task<Long> = db.runTransaction(Transaction.Function<Long> { tx ->
                    val existing = tx.get(mine)
                    if (existing.exists()) return@Function existing.getLong("n") ?: 0L
                    val next = (tx.get(counter).getLong("n") ?: 0L) + 1L
                    tx.set(counter, mapOf("n" to next))
                    tx.set(mine, mapOf("n" to next, "createdAt" to FieldValue.serverTimestamp(), "name" to name.take(40)))
                    next
                })
                awaitTask(task)
            }.getOrNull()?.takeIf { it > 0L }
        }
    }

    private suspend fun <T> awaitTask(task: Task<T>): T = suspendCancellableCoroutine { cont ->
        task.addOnSuccessListener { cont.resume(it) }
        task.addOnFailureListener { cont.resumeWithException(it) }
    }
}
