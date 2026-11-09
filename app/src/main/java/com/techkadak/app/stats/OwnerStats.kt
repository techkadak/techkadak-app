package com.techkadak.app.stats

import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/** Dashboard ka poora data (sirf owner padh sakta hai — Firestore rules). */
data class DashData(
    val today: String,
    val activeToday: Long,
    val newToday: Long,
    /** Purane se naya, last 30 din: (date, active, new) */
    val days: List<DayPoint>,
    val totalDevices: Long,
    val active7: Long,
    val churned7: Long,
    val churned30: Long,
    val versions: List<Pair<String, Long>>,
    val topInstalls: List<Pair<String, Long>>,
    val topClicks: List<Pair<String, Long>>,
    val kitInstalls: List<Pair<String, Long>>,
    val posterShares: Long,
    val posterTop: List<Pair<String, Long>>,
    val events7: Map<String, Long>,
    val loginTypes: List<Pair<String, Long>>,
) {
    val returningToday: Long get() = (activeToday - newToday).coerceAtLeast(0)
}

data class DayPoint(val date: String, val active: Long, val new: Long)

/** Aggregation queries (count) — sasti: 1 read per 1000 docs. Top apps ke liye sirf last 7 din ke day-docs padhte hain. */
object OwnerStats {
    const val OWNER_EMAIL = "ridhirohit0@gmail.com"
    private val ist = TimeZone.getTimeZone("Asia/Kolkata")

    fun isOwner(email: String?): Boolean = email?.trim()?.equals(OWNER_EMAIL, ignoreCase = true) == true

    fun lastDays(n: Int, now: Long = System.currentTimeMillis()): List<String> {
        val f = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = ist }
        val c = Calendar.getInstance(ist).apply { timeInMillis = now }
        val out = ArrayList<String>(n)
        repeat(n) { out += f.format(c.time); c.add(Calendar.DAY_OF_YEAR, -1) }
        return out.reversed()
    }

    private suspend fun count(q: Query): Long = q.count().get(AggregateSource.SERVER).await().count

    suspend fun load(currentVersionCode: Int, versionNames: Map<Int, String>): DashData = coroutineScope {
        val db = FirebaseFirestore.getInstance()
        val days30 = lastDays(30)
        val today = days30.last()
        val d7 = lastDays(7)
        val cutoff7 = d7.first()           // last >= cutoff7 => active in last 7 days
        val cutoff30 = days30.first()

        val perDay = days30.map { d ->
            async {
                val col = db.collection("days").document(d).collection("d")
                DayPoint(d, count(col), count(col.whereEqualTo("n", true)))
            }
        }
        val devices = db.collection("devices")
        val total = async { count(devices) }
        val active7 = async { count(devices.whereGreaterThanOrEqualTo("last", cutoff7)) }
        val churn7 = async { count(devices.whereLessThan("last", cutoff7)) }
        val churn30 = async { count(devices.whereLessThan("last", cutoff30)) }
        val known = (versionNames.keys + currentVersionCode).distinct().sortedDescending()
        val versions = known.map { vc -> async { (versionNames[vc] ?: vc.toString()) to count(devices.whereEqualTo("v", vc.toLong())) } }
        val logins = listOf("none", "google", "email", "phone").map { lt -> async { lt to count(devices.whereEqualTo("lt", lt)) } }

        // Last 7 din ke day-docs: installs / kits / posters (har active device ka din me 1 doc)
        val docs = d7.map { d -> async { db.collection("days").document(d).collection("d").limit(3000).get().await().documents } }.awaitAll().flatten()
        val ic = HashMap<String, Long>(); val id = HashMap<String, Long>(); val kits = HashMap<String, Long>(); val ps = HashMap<String, Long>(); val ev = HashMap<String, Long>()
        fun addAll(dst: HashMap<String, Long>, raw: Any?) {
            (raw as? Map<*, *>)?.forEach { (k, v) -> val key = k?.toString() ?: return@forEach; dst[key] = (dst[key] ?: 0L) + ((v as? Number)?.toLong() ?: 0L) }
        }
        docs.forEach { s ->
            addAll(ic, s.get("ic")); addAll(id, s.get("id")); addAll(kits, s.get("kits")); addAll(ps, s.get("ps")); addAll(ev, s.get("ev"))
        }
        fun top(m: Map<String, Long>, n: Int = 10) = m.entries.sortedByDescending { it.value }.take(n).map { it.key to it.value }

        val points = perDay.awaitAll()
        val vList = versions.awaitAll().filter { it.second > 0 }
        val totalN = total.await()
        val vSum = vList.sumOf { it.second }
        DashData(
            today = today,
            activeToday = points.last().active,
            newToday = points.last().new,
            days = points,
            totalDevices = totalN,
            active7 = active7.await(),
            churned7 = churn7.await(),
            churned30 = churn30.await(),
            versions = if (totalN > vSum) vList + ("purane / dusre" to (totalN - vSum)) else vList,
            topInstalls = top(id), topClicks = top(ic), kitInstalls = top(kits),
            posterShares = ps.values.sum(), posterTop = top(ps, 5),
            events7 = ev,
            loginTypes = logins.awaitAll().filter { it.second > 0 },
        )
    }
}


/** v1.6: a device that chose to share (devices/{id}.sh == true). */
data class SharedDevice(val id: String, val name: String, val model: String, val os: String, val version: String, val last: String, val apps: List<String>)

object OwnerDevices {
    suspend fun load(): List<SharedDevice> {
        val snap = FirebaseFirestore.getInstance().collection("devices").whereEqualTo("sh", true).limit(300).get().await()
        return snap.documents.map { d ->
            @Suppress("UNCHECKED_CAST")
            SharedDevice(
                id = d.id, name = d.getString("dn").orEmpty().ifBlank { d.getString("model").orEmpty() },
                model = d.getString("model").orEmpty(), os = d.getString("os").orEmpty(), version = d.getString("vn").orEmpty(),
                last = d.getString("last").orEmpty(), apps = (d.get("ia") as? List<String>).orEmpty(),
            )
        }.sortedByDescending { it.last }
    }
}
