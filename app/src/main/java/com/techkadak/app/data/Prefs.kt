package com.techkadak.app.data

import android.content.Context
import android.content.SharedPreferences

/** Chhoti settings (SharedPreferences). */
class Prefs(context: Context) {
    private val sp: SharedPreferences = context.getSharedPreferences("tk", Context.MODE_PRIVATE)

    var dailyNotify: Boolean
        get() = sp.getBoolean("daily_notify", true)
        set(v) = sp.edit().putBoolean("daily_notify", v).apply()
    var pushNotify: Boolean
        get() = sp.getBoolean("push_notify", true)
        set(v) = sp.edit().putBoolean("push_notify", v).apply()
    /** Aakhri app jiski notification bheji (ya jo user dekh chuka) — duplicate notification na aaye. */
    var lastNotifiedSlug: String?
        get() = sp.getString("last_notified", null)
        set(v) = sp.edit().putString("last_notified", v).apply()
    var lastUpdateCheck: Long
        get() = sp.getLong("last_update_check", 0L)
        set(v) = sp.edit().putLong("last_update_check", v).apply()
    /** "Baad me" dabaya to is time tak update dialog dobara nahi (24 ghante). */
    var updateRemindAt: Long
        get() = sp.getLong("update_remind_at", 0L)
        set(v) = sp.edit().putLong("update_remind_at", v).apply()
    var skippedVersion: String?
        get() = sp.getString("skipped_version", null)
        set(v) = sp.edit().putString("skipped_version", v).apply()
    var recentSearches: List<String>
        get() = sp.getString("recent_searches", "")!!.split("\n").filter { it.isNotBlank() }
        set(v) = sp.edit().putString("recent_searches", v.take(8).joinToString("\n")).apply()
    var seenSlugs: Set<String>
        get() = sp.getStringSet("seen_slugs", emptySet())!!
        set(v) = sp.edit().putStringSet("seen_slugs", v).apply()

    fun addRecent(q: String) {
        val t = q.trim(); if (t.length < 2) return
        recentSearches = listOf(t) + recentSearches.filterNot { it.equals(t, true) }
    }
}
