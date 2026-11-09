package com.techkadak.app.data

import android.content.Context
import com.techkadak.app.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** v1.7: website ke data/events.json se "event aaya" pop-up. App update nahi lagta. */
data class TkEvent(val id: String, val title: String, val body: String, val emoji: String, val url: String?, val button: String, val image: String? = null, val repeat: Boolean = false)

object Events {
    private fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun parse(text: String, now: String = today()): List<TkEvent> {
        val arr = JSONObject(text).optJSONArray("events") ?: return emptyList()
        val out = ArrayList<TkEvent>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val id = o.optString("id").trim(); val title = o.optString("title").trim()
            if (id.isEmpty() || title.isEmpty()) continue
            val from = o.optString("from").trim(); val until = o.optString("until").trim()
            if (from.isNotEmpty() && now < from) continue
            if (until.isNotEmpty() && now > until) continue
            val url = o.optString("url").trim().takeIf { it.startsWith("https://") }
            out.add(TkEvent(id, title.take(80), o.optString("body").trim().take(300), o.optString("emoji").trim().ifEmpty { "\uD83C\uDF89" }.take(4), url, o.optString("button").trim().ifEmpty { "Dekho" }.take(20), o.optString("image").trim().takeIf { it.startsWith("https://") }, o.optBoolean("repeat", false)))
        }
        return out
    }

    suspend fun fetch(): List<TkEvent> = withContext(Dispatchers.IO) {
        runCatching { parse(Http.getText(Config.SITE_BASE + "/data/events.json?t=" + (System.currentTimeMillis() / 300_000))) }.getOrDefault(emptyList())
    }

    private fun sp(c: Context) = c.getSharedPreferences("tk_events", Context.MODE_PRIVATE)
    fun seen(c: Context): Set<String> = sp(c).getStringSet("seen", emptySet()) ?: emptySet()
    fun markSeen(c: Context, id: String) { sp(c).edit().putStringSet("seen", seen(c) + id).apply() }
}
