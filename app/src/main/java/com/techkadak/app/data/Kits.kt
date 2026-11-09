package com.techkadak.app.data

import org.json.JSONObject

/** Kit = ek theme ke 4-6 apps ka bundle ("Student Kit" etc.). Sirf catalog (apps.json) ke apps. */
data class Kit(
    val id: String,
    val name: String,
    val emoji: String,
    val tagline: String,
    val slugs: List<String>,
) {
    fun apps(all: List<AppItem>): List<AppItem> = slugs.mapNotNull { s -> all.firstOrNull { it.slug == s } }
}

object Kits {
    /**
     * kits.json: {"kits":[{"id","name","emoji","tagline","apps":[slug…]}]}
     * Galat entry chup-chaap skip. Catalog me jo slug nahi, wo hata do; 2 se kam apps = kit hi nahi.
     */
    fun parse(json: String?, catalog: List<AppItem>? = null): List<Kit> = runCatching {
        val arr = JSONObject(json ?: return emptyList()).optJSONArray("kits") ?: return emptyList()
        val known = catalog?.map { it.slug }?.toHashSet()
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val id = o.optString("id").trim().takeIf { Regex("^[a-z0-9-]{2,40}$").matches(it) } ?: return@mapNotNull null
            val name = o.optString("name").trim().takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            val apps = o.optJSONArray("apps")?.let { a -> (0 until a.length()).mapNotNull { a.optString(it).trim().takeIf(String::isNotEmpty) } }.orEmpty()
                .distinct().filter { known == null || it in known }.take(8)
            if (apps.size < 2) return@mapNotNull null
            Kit(id, name.take(40), o.optString("emoji").trim().take(4).ifEmpty { "🧰" }, o.optString("tagline").trim().take(90), apps)
        }.distinctBy { it.id }
    }.getOrDefault(emptyList())

    /** Remote (Remote Config kits_json ya website data/kits.json) mile aur valid ho to wahi, warna bundled. */
    fun choose(remote: List<Kit>?, bundled: List<Kit>): List<Kit> = if (!remote.isNullOrEmpty()) remote else bundled
}
