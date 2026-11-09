package com.techkadak.app.data

/** Tech Kadak YouTube channel ka public video (RSS feed se). */
data class TkVideo(
    val id: String,
    val title: String,
    val description: String,
    val published: String,
) {
    val url: String get() = "https://www.youtube.com/watch?v=$id"
    val thumb: String get() = "https://i.ytimg.com/vi/$id/hqdefault.jpg"
}

object Videos {
    const val CHANNEL_ID = "UCpxdwzHB-66cPyssRH5IaTA"
    const val FEED_URL = "https://www.youtube.com/feeds/videos.xml?channel_id=$CHANNEL_ID"

    private val entryRx = Regex("<entry>(.*?)</entry>", RegexOption.DOT_MATCHES_ALL)
    private fun tag(body: String, t: String): String? =
        Regex("<$t(?:\\s[^>]*)?>(.*?)</$t>", RegexOption.DOT_MATCHES_ALL).find(body)?.groupValues?.get(1)

    private fun unescape(s: String) = s.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'").replace("&apos;", "'").replace("&amp;", "&")

    /** YouTube Atom feed -> videos (naye pehle). Sirf valid 11-char ids. */
    fun parseFeed(xml: String?): List<TkVideo> {
        if (xml.isNullOrBlank()) return emptyList()
        return entryRx.findAll(xml).mapNotNull { m ->
            val b = m.groupValues[1]
            val id = tag(b, "yt:videoId")?.trim()?.takeIf { Regex("^[A-Za-z0-9_-]{11}$").matches(it) } ?: return@mapNotNull null
            TkVideo(
                id = id,
                title = unescape(tag(b, "title")?.trim().orEmpty()),
                description = unescape(tag(b, "media:description")?.trim().orEmpty()),
                published = tag(b, "published")?.trim().orEmpty(),
            )
        }.distinctBy { it.id }.sortedByDescending { it.published }.toList()
    }

    /** "DaVinci Resolve (free version)" -> "DaVinci Resolve", "Goodtime: Pomodoro Timer" -> "Goodtime" */
    fun matchName(name: String): String = name.substringBefore('(').substringBefore(':').substringBefore(" — ").substringBefore(" - ").trim()

    /**
     * Is app ke videos: pehle apps.json ka explicit "videos" field, phir title/description me naam (poora shabd).
     * Chhote naam (<4 letters, jaise VLC/GKD) sirf exact case me match hote hain, taaki "jan" jaisa shabd galti se na jude.
     */
    fun forApp(app: AppItem, videos: List<TkVideo>): List<TkVideo> {
        val explicit = app.videoIds.mapNotNull { id -> videos.firstOrNull { it.id == id } }
        val n = matchName(app.name)
        if (n.length < 3) return explicit
        val rx = if (n.length < 4) Regex("(?<![A-Za-z0-9])" + Regex.escape(n) + "(?![A-Za-z0-9])")
        else Regex("(?<![A-Za-z0-9])" + Regex.escape(n) + "(?![A-Za-z0-9])", RegexOption.IGNORE_CASE)
        val byName = videos.filter { v -> rx.containsMatchIn(v.title) || rx.containsMatchIn(v.description) }
        return (explicit + byName).distinctBy { it.id }
    }
}
