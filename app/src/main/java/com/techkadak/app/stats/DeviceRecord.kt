package com.techkadak.app.stats

import android.content.Context
import android.os.Build

/**
 * v1.5 privacy-safe per-device record (Firestore devices/{random install id}).
 * Sent: model, Android + app version, first/last seen, login type, and a name ONLY when the user is logged in or typed one
 * (feedback / review form). Apps: only the ones installed or opened THROUGH Tech Kadak. There is NO scan of the installed-app list.
 * "Usage stats bhejo" OFF => nothing is sent and the device doc is deleted.
 */
object DeviceRecord {
    private const val PREF = "tk_device"
    const val MAX_APPS = 40

    /** Pure builder (unit tested): the exact map written to Firestore, trimmed to what the rules accept. */
    fun buildDoc(
        first: String, last: String, versionCode: Long, versionName: String, sdk: Long, loginType: String,
        model: String, osRelease: String, apps: Map<String, String>, name: String?,
    ): Map<String, Any> = buildMap {
        put("first", first); put("last", last); put("v", versionCode); put("vn", versionName.take(20)); put("sdk", sdk)
        put("lt", loginType)
        clean(model, 40).takeIf { it.isNotEmpty() }?.let { put("model", it) }
        clean(osRelease, 12).takeIf { it.isNotEmpty() }?.let { put("os", it) }
        val a = apps.entries.filter { it.value == "i" || it.value == "o" }.take(MAX_APPS).associate { it.key.take(60) to it.value }
        if (a.isNotEmpty()) put("apps", a)
        name?.let { clean(it, 40) }?.takeIf { it.isNotEmpty() }?.let { put("nm", it) }
    }

    fun clean(s: String, max: Int): String = s.replace(Regex("[\\u0000-\\u001f]"), " ").trim().take(max)

    /** What the dashboard shows when there is no name. */
    fun label(deviceId: String, name: String?): String = name?.takeIf { it.isNotBlank() } ?: "Anonymous device #" + deviceId.take(4)

    /** Merge rule for the opened/installed marks: installed ("i") beats opened ("o"); capped. */
    fun mark(apps: Map<String, String>, slug: String, kind: String): Map<String, String> {
        if (slug.isBlank() || (kind != "i" && kind != "o")) return apps
        val cur = apps[slug]
        if (cur == "i") return apps
        if (cur == null && apps.size >= MAX_APPS) return apps
        return apps + (slug.take(60) to kind)
    }

    private fun sp(ctx: Context) = ctx.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun apps(ctx: Context): Map<String, String> =
        sp(ctx).getString("apps", "").orEmpty().lines().mapNotNull { l -> l.split('=').takeIf { it.size == 2 && it[0].isNotBlank() }?.let { it[0] to it[1] } }.toMap()

    fun markApp(ctx: Context, slug: String?, kind: String) {
        if (slug.isNullOrBlank() || !Stats.enabled(ctx)) return
        synchronized(this) {
            val cur = apps(ctx); val next = mark(cur, slug, kind)
            if (next != cur) sp(ctx).edit().putString("apps", next.entries.joinToString("\n") { "${it.key}=${it.value}" }).apply()
        }
    }

    fun typedName(ctx: Context): String? = sp(ctx).getString("typed_name", null)?.takeIf { it.isNotBlank() }
    fun setTypedName(ctx: Context, name: String) { sp(ctx).edit().putString("typed_name", clean(name, 40)).apply() }

    /** Logged-in display name wins; otherwise a name the user typed in a form; otherwise none (Anonymous device #xxxx). */
    fun currentName(ctx: Context, loggedInName: String?): String? = loggedInName?.takeIf { it.isNotBlank() } ?: typedName(ctx)

    fun modelName(): String {
        val m = Build.MODEL.orEmpty(); val b = Build.MANUFACTURER.orEmpty()
        return if (m.startsWith(b, ignoreCase = true) || b.isBlank()) m else "$b $m"
    }
}
