package com.techkadak.app.data

import org.json.JSONArray
import org.json.JSONObject

data class AppLink(val label: String, val url: String)
data class AppPost(val type: String, val url: String, val title: String)

data class AppItem(
    val slug: String,
    val name: String,
    val categories: List<String>,
    val what: String,
    val pricing: String,
    val priceNote: String?,
    val limit: String?,
    val platforms: String?,
    val replaces: String?,
    val links: List<AppLink>,
    val icon: String?,
    val posts: List<AppPost>,
    val added: String,
    val verified: String?,
    val aiAdded: Boolean,
    /** Optional (apps.json): "github:owner/repo" | "fdroid:pkg" | "url:https://…" — in-app install ka official source */
    val apkSource: String? = null,
    val packageName: String? = null,
    /** Optional (apps.json "videos": ["<YouTube id>"]) — Tech Kadak ka video jo is app pe hai. */
    val videoIds: List<String> = emptyList(),
    /** v1.5: apps.json "screenshots" — sirf https, max 6. Khali = carousel nahi dikhta. */
    val screenshots: List<String> = emptyList(),
) {
    fun iconUrl(base: String): String? = icon?.let { if (it.startsWith("https://")) it else base.trimEnd('/') + "/" + it.trimStart('/') }
    fun pageUrl(base: String): String = base.trimEnd('/') + "/apps/$slug.html"
    val initials: String
        get() = name.split(' ', '-', '_').filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }.ifEmpty { "?" }

    /** Download buttons: official store/source pehle (F-Droid, GitHub, Play Store), phir website. */
    val downloadLinks: List<AppLink>
        get() = links.sortedBy { linkRank(it.label) }

    val instagramPost: AppPost? get() = posts.firstOrNull { it.type.startsWith("instagram") }

    fun matches(query: String): Boolean {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return true
        return q.split(Regex("\\s+")).all { t ->
            name.lowercase().contains(t) || what.lowercase().contains(t) ||
                (replaces?.lowercase()?.contains(t) == true) ||
                categories.any { it.lowercase().contains(t) } || pricing.lowercase().contains(t)
        }
    }

    companion object {
        fun linkRank(label: String): Int = when (label.lowercase().trim()) {
            "f-droid" -> 0
            "github", "github releases" -> 1
            "play store", "google play" -> 2
            "download" -> 3
            "website", "web app" -> 4
            else -> 5
        }
    }
}

object AppsParser {
    private fun JSONObject.str(key: String): String? =
        if (!has(key) || isNull(key)) null else optString(key).trim().takeIf { it.isNotEmpty() && it != "null" }

    private fun isSafeUrl(u: String) = u.startsWith("https://")

    /** apps.json (website ka format) ko parse karta hai. Naye apps pehle (added desc), same din = file ka order. */
    fun parse(json: String): List<AppItem> {
        val root = JSONObject(json)
        val arr = root.optJSONArray("apps") ?: JSONArray()
        val out = ArrayList<Pair<Int, AppItem>>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val slug = o.str("slug") ?: continue
            val name = o.str("name") ?: continue
            val cats = o.optJSONArray("category")?.let { a -> (0 until a.length()).mapNotNull { a.optString(it).takeIf(String::isNotBlank) } }
                ?: listOfNotNull(o.str("category"))
            val links = o.optJSONArray("links")?.let { a ->
                (0 until a.length()).mapNotNull { j ->
                    val l = a.optJSONObject(j) ?: return@mapNotNull null
                    val url = l.str("url") ?: return@mapNotNull null
                    if (!isSafeUrl(url)) null else AppLink(l.str("label") ?: "Link", url)
                }
            } ?: emptyList()
            val posts = o.optJSONArray("posts")?.let { a ->
                (0 until a.length()).mapNotNull { j ->
                    val p = a.optJSONObject(j) ?: return@mapNotNull null
                    val url = p.str("url") ?: return@mapNotNull null
                    if (!isSafeUrl(url)) null else AppPost(p.str("type") ?: "link", url, p.str("title") ?: "")
                }
            } ?: emptyList()
            out += i to AppItem(
                slug = slug, name = name, categories = cats,
                what = o.str("what") ?: "",
                pricing = o.str("pricing") ?: "Free",
                priceNote = o.str("priceNote"), limit = o.str("limit"), platforms = o.str("platforms"),
                replaces = o.str("replaces"), links = links, icon = o.str("icon"), posts = posts,
                added = o.str("added") ?: "", verified = o.str("verified"),
                aiAdded = o.optBoolean("aiAdded", false),
                apkSource = o.str("apk_source"), packageName = o.str("package_name"),
                screenshots = o.optJSONArray("screenshots")?.let { a -> (0 until a.length()).mapNotNull { j -> a.optString(j).trim().takeIf { isSafeUrl(it) } }.take(6) } ?: emptyList(),
                videoIds = o.optJSONArray("videos")?.let { a -> (0 until a.length()).mapNotNull { a.optString(it).trim().takeIf { v -> Regex("^[A-Za-z0-9_-]{11}$").matches(v) } } } ?: emptyList(),
            )
        }
        return out.sortedWith(compareByDescending<Pair<Int, AppItem>> { it.second.added }.thenBy { it.first }).map { it.second }
    }
}

object Dates {
    private val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    /** "2026-11-03" -> "3 Nov 2026" */
    fun pretty(iso: String?): String {
        if (iso == null) return ""
        val p = iso.take(10).split("-")
        if (p.size != 3) return iso
        val m = p[1].toIntOrNull() ?: return iso
        val d = p[2].toIntOrNull() ?: return iso
        if (m !in 1..12) return iso
        return "$d ${months[m - 1]} ${p[0]}"
    }
}
