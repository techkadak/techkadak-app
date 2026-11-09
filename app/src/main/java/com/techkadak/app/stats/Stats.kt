package com.techkadak.app.stats

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.techkadak.app.BuildConfig
import com.techkadak.app.firebase.TkFirebase
import org.json.JSONObject
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Owner dashboard ke liye ANONYMOUS usage stats (v1.2).
 *
 * - Koi email / phone / naam kabhi nahi bheja jaata. Sirf ek random device id (32 hex, phone pe hi bana).
 * - Firestore (Spark free): har device ka din me EK doc  days/{IST date}/d/{deviceId}  (us din ke counters, batch me)
 *   + ek doc  devices/{deviceId}  (first seen / last seen / version). Per-event write nahi — din me max ~8 writes.
 * - Google Analytics (Firebase) me same events (DAU/retention Firebase console me).
 * - Settings → "Usage stats bhejo" OFF = kuch nahi jaata.
 */
object Stats {
    private const val TAG = "TkStats"
    private const val PREF = "tk_stats"
    private const val MAX_WRITES_PER_DAY = 8
    private const val MIN_GAP_MS = 10 * 60 * 1000L
    private const val MAX_SLUGS = 80

    // Event names (dashboard + GA dono)
    const val APP_OPEN = "app_open"
    const val INSTALL_CLICK = "install_click"
    const val INSTALL_DONE = "install_done"
    const val KIT_INSTALL = "kit_install"
    const val POSTER_SHARE = "poster_share"
    const val UPDATE_INSTALLED = "update_installed"
    const val ONBOARDING_DONE = "onboarding_done"
    const val LOGIN = "login"
    const val VIDEO_OPEN = "video_open"

    private val ist: TimeZone = TimeZone.getTimeZone("Asia/Kolkata")
    fun today(now: Long = System.currentTimeMillis()): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = ist }.format(Date(now))

    private fun sp(ctx: Context): SharedPreferences = ctx.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun enabled(ctx: Context): Boolean = sp(ctx).getBoolean("on", true)

    fun setEnabled(ctx: Context, on: Boolean) {
        sp(ctx).edit().putBoolean("on", on).apply()
        ga(ctx)?.setAnalyticsCollectionEnabled(on)
        // v1.5: opt-out also removes this device's record from the dashboard (best effort; rules allow delete of own id)
        if (!on && TkFirebase.available(ctx)) runCatching {
            FirebaseFirestore.getInstance().collection("devices").document(deviceId(ctx)).delete()
                .addOnFailureListener { Log.w(TAG, "device delete failed", it) }
            sp(ctx).edit().remove("dev_hash").apply()
        }
        if (on) sp(ctx).edit().remove("dev_hash").apply() // next flush writes the record again
    }

    /** Random, anonymous, sirf is phone ka. Ek baar bana, phir wahi. */
    fun deviceId(ctx: Context): String {
        val p = sp(ctx)
        p.getString("did", null)?.let { if (validId(it)) return it }
        val b = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val id = b.joinToString("") { "%02x".format(it) }
        p.edit().putString("did", id).putString("first", today()).apply()
        return id
    }

    fun validId(id: String) = Regex("^[a-f0-9]{32}$").matches(id)

    private fun firstDay(ctx: Context): String {
        deviceId(ctx)
        return sp(ctx).getString("first", null) ?: today().also { sp(ctx).edit().putString("first", it).apply() }
    }

    private fun ga(ctx: Context): FirebaseAnalytics? =
        if (TkFirebase.available(ctx)) runCatching { FirebaseAnalytics.getInstance(ctx.applicationContext) }.getOrNull() else null

    /** App start pe ek baar: GA collection setting + version update check. */
    fun init(ctx: Context) {
        runCatching {
            ga(ctx)?.setAnalyticsCollectionEnabled(enabled(ctx))
            val p = sp(ctx)
            val last = p.getInt("last_vc", 0)
            if (last != BuildConfig.VERSION_CODE) {
                p.edit().putInt("last_vc", BuildConfig.VERSION_CODE).apply()
                if (last in 1 until BuildConfig.VERSION_CODE) event(ctx, UPDATE_INSTALLED, version = BuildConfig.VERSION_NAME)
            }
        }.onFailure { Log.w(TAG, "init", it) }
    }

    // ── Local day blob ──
    data class Day(
        val day: String,
        val ev: MutableMap<String, Long> = linkedMapOf(),
        val ic: MutableMap<String, Long> = linkedMapOf(),
        val id: MutableMap<String, Long> = linkedMapOf(),
        val kits: MutableMap<String, Long> = linkedMapOf(),
        val ps: MutableMap<String, Long> = linkedMapOf(),
        var up: String? = null,
        var writes: Int = 0,
        var lastWrite: Long = 0,
        var dirty: Boolean = true,
    ) {
        fun toJson(): String = JSONObject().apply {
            put("day", day); put("ev", JSONObject(ev as Map<*, *>)); put("ic", JSONObject(ic as Map<*, *>)); put("id", JSONObject(id as Map<*, *>))
            put("kits", JSONObject(kits as Map<*, *>)); put("ps", JSONObject(ps as Map<*, *>)); up?.let { put("up", it) }
            put("writes", writes); put("lastWrite", lastWrite); put("dirty", dirty)
        }.toString()

        /** Firestore doc (rules: v, n, ev, ic, id, kits, ps, up). */
        fun toDoc(versionCode: Int, isNew: Boolean): Map<String, Any> = buildMap {
            put("v", versionCode.toLong()); put("n", isNew)
            if (ev.isNotEmpty()) put("ev", HashMap(ev))
            if (ic.isNotEmpty()) put("ic", HashMap(ic))
            if (id.isNotEmpty()) put("id", HashMap(id))
            if (kits.isNotEmpty()) put("kits", HashMap(kits))
            if (ps.isNotEmpty()) put("ps", HashMap(ps))
            up?.let { put("up", it.take(20)) }
        }

        companion object {
            private fun map(o: JSONObject?): MutableMap<String, Long> {
                val m = linkedMapOf<String, Long>()
                o?.keys()?.forEach { k -> m[k] = o.optLong(k) }
                return m
            }
            fun fromJson(s: String?): Day? = runCatching {
                val o = JSONObject(s ?: return null)
                Day(
                    o.getString("day"), map(o.optJSONObject("ev")), map(o.optJSONObject("ic")), map(o.optJSONObject("id")),
                    map(o.optJSONObject("kits")), map(o.optJSONObject("ps")), o.optString("up").ifEmpty { null },
                    o.optInt("writes"), o.optLong("lastWrite"), o.optBoolean("dirty", true),
                )
            }.getOrNull()
        }
    }

    private val lock = Any()

    private fun loadDay(ctx: Context): Day {
        val t = today()
        val d = Day.fromJson(sp(ctx).getString("day_blob", null))
        return if (d != null && d.day == t) d else Day(t)
    }

    private fun saveDay(ctx: Context, d: Day) { sp(ctx).edit().putString("day_blob", d.toJson()).apply() }

    private fun bump(m: MutableMap<String, Long>, key: String) {
        val k = key.take(60)
        if (k.isEmpty()) return
        if (k !in m && m.size >= MAX_SLUGS) return
        m[k] = (m[k] ?: 0L) + 1
    }

    /** Ek event record karo (local counter + GA). Firestore pe baad me batch me jaata hai. */
    fun event(ctx: Context, name: String, slug: String? = null, kit: String? = null, version: String? = null, loginType: String? = null) {
        if (!enabled(ctx)) return
        runCatching {
            synchronized(lock) {
                val d = loadDay(ctx)
                bump(d.ev, name)
                when (name) {
                    INSTALL_CLICK -> slug?.let { bump(d.ic, it) }
                    INSTALL_DONE -> slug?.let { bump(d.id, it) }
                    POSTER_SHARE -> slug?.let { bump(d.ps, it) }
                    KIT_INSTALL -> kit?.let { bump(d.kits, it) }
                    UPDATE_INSTALLED -> d.up = version
                }
                d.dirty = true
                saveDay(ctx, d)
            }
            ga(ctx)?.let { fa ->
                val b = Bundle()
                slug?.let { b.putString("app_slug", it.take(90)) }
                kit?.let { b.putString("kit_id", it.take(90)) }
                version?.let { b.putString("version", it.take(30)) }
                loginType?.let { b.putString("method", it); fa.setUserProperty("login_type", it) }
                fa.logEvent(if (name == APP_OPEN) FirebaseAnalytics.Event.APP_OPEN else name, b)
            }
        }.onFailure { Log.w(TAG, "event $name", it) }
    }

    /** App foreground me aaya: app_open + (din ka pehla ho to) turant write. */
    fun appOpen(ctx: Context, loginType: String?) {
        event(ctx, APP_OPEN)
        flush(ctx, loginType, firstOfDayOnly = false)
    }

    /**
     * Din ke counters Firestore pe (absolute values, merge — dobara likhna safe hai).
     * Din ka pehla write turant; baaki kam se kam 10 min ke gap pe, din me max 8.
     */
    fun flush(ctx: Context, loginType: String? = null, firstOfDayOnly: Boolean = false, force: Boolean = false) {
        if (!enabled(ctx) || !TkFirebase.available(ctx)) return
        runCatching {
            val did = deviceId(ctx)
            val first = firstDay(ctx)
            val (d, doc) = synchronized(lock) {
                val d = loadDay(ctx)
                val now = System.currentTimeMillis()
                val firstWrite = d.writes == 0
                if (!firstWrite && firstOfDayOnly) return
                if (!firstWrite && !d.dirty) return
                if (!firstWrite && !force && now - d.lastWrite < MIN_GAP_MS) return
                if (d.writes >= MAX_WRITES_PER_DAY) return
                d.writes += 1; d.lastWrite = now; d.dirty = false
                saveDay(ctx, d)
                d to d.toDoc(BuildConfig.VERSION_CODE, first == d.day)
            }
            val db = FirebaseFirestore.getInstance()
            db.collection("days").document(d.day).collection("d").document(did).set(doc, SetOptions.merge())
                .addOnFailureListener { Log.w(TAG, "day write failed", it) }
            // devices/{id}: first write of the day (last seen), a login change, or changed content (apps / name / version), at most every 10 min
            val loggedName = runCatching { if (TkFirebase.available(ctx)) com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.displayName else null }.getOrNull()
            val dev0 = DeviceRecord.buildDoc(
                first, d.day, BuildConfig.VERSION_CODE.toLong(), BuildConfig.VERSION_NAME, Build.VERSION.SDK_INT.toLong(), normLogin(loginType ?: lastLogin(ctx)),
                DeviceRecord.modelName(), Build.VERSION.RELEASE.orEmpty(), DeviceRecord.apps(ctx), DeviceRecord.currentName(ctx, loggedName),
            )
            val dev: Map<String, Any> = dev0 + DeviceShare.fields(ctx)
            val p = sp(ctx)
            val hash = dev.entries.filter { it.key != "last" }.sortedBy { it.key }.joinToString("|") { "${it.key}=${it.value}" }.hashCode().toString()
            val lastDev = p.getLong("dev_at", 0L)
            val changed = hash != p.getString("dev_hash", null)
            if (d.writes == 1 || loginType != null || (changed && System.currentTimeMillis() - lastDev > MIN_GAP_MS) || force) {
                if (loginType != null) p.edit().putString("last_login", normLogin(loginType)).apply()
                p.edit().putString("dev_hash", hash).putLong("dev_at", System.currentTimeMillis()).apply()
                db.collection("devices").document(did).set(dev, SetOptions.merge())
                    .addOnFailureListener { Log.w(TAG, "device write failed", it) }
            }
        }.onFailure { Log.w(TAG, "flush", it) }
    }

    private fun lastLogin(ctx: Context): String? = sp(ctx).getString("last_login", null)

    fun normLogin(p: String?): String = when (p) {
        "google", "google.com" -> "google"
        "password", "email" -> "email"
        "phone" -> "phone"
        else -> "none"
    }

    /** Tests ke liye: local blob padho. */
    fun peekDay(ctx: Context): Day = synchronized(lock) { loadDay(ctx) }
}
