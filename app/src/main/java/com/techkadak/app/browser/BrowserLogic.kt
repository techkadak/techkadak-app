package com.techkadak.app.browser

import java.net.URLEncoder
import java.util.Locale

/** v1.5 in-app browser: pure logic (no Android classes) so it is unit-testable. */

enum class SearchEngine(val id: String, val label: String, val template: String) {
    GOOGLE("google", "Google", "https://www.google.com/search?q=%s"),
    DUCKDUCKGO("ddg", "DuckDuckGo", "https://duckduckgo.com/?q=%s");

    fun searchUrl(q: String): String = template.replace("%s", URLEncoder.encode(q.trim(), "UTF-8"))

    companion object { fun byId(id: String?): SearchEngine = entries.firstOrNull { it.id == id } ?: GOOGLE }
}

/** What to do with what the user typed in the address bar. */
sealed interface Resolved {
    data class Url(val url: String) : Resolved
    data class Search(val url: String, val query: String) : Resolved
    data object Blocked : Resolved
}

object UrlInput {
    private val schemeRe = Regex("^([a-zA-Z][a-zA-Z0-9+.-]*):")
    private val hostRe = Regex("^(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,24}(?::\\d{1,5})?(?:[/?#].*)?$")
    private val ipRe = Regex("^\\d{1,3}(?:\\.\\d{1,3}){3}(?::\\d{1,5})?(?:[/?#].*)?$")
    private val localRe = Regex("^localhost(?::\\d{1,5})?(?:[/?#].*)?$", RegexOption.IGNORE_CASE)
    /** Schemes the address bar never loads (script / local file / app intents). */
    val blockedSchemes = setOf("javascript", "file", "content", "intent", "data", "blob", "vbscript", "view-source")

    fun resolve(raw: String, engine: SearchEngine): Resolved {
        val s = raw.trim()
        if (s.isEmpty()) return Resolved.Blocked
        val scheme = schemeRe.find(s)?.groupValues?.get(1)?.lowercase(Locale.ROOT)
        // "localhost:8080" / "site.com:8080" look like a scheme: treat host:port as a host
        val looksHostPort = scheme != null && (hostRe.matches(s) || localRe.matches(s) || ipRe.matches(s)) && !s.contains("://")
        if (scheme != null && !looksHostPort) {
            if (scheme in blockedSchemes) return Resolved.Blocked
            if (scheme == "http" || scheme == "https") return if (s.length > scheme.length + 3 && !s.contains(' ')) Resolved.Url(s) else Resolved.Search(engine.searchUrl(s), s)
            if (scheme == "about" && s.equals("about:blank", true)) return Resolved.Url("about:blank")
            return Resolved.Blocked
        }
        if (!s.contains(' ') && (hostRe.matches(s) || ipRe.matches(s) || localRe.matches(s))) {
            val local = localRe.matches(s) || ipRe.matches(s)
            return Resolved.Url((if (local) "http://" else "https://") + s)
        }
        return Resolved.Search(engine.searchUrl(s), s)
    }

    fun hostOf(url: String): String = runCatching {
        val rest = url.substringAfter("://", "")
        rest.substringBefore('/').substringBefore('?').substringBefore('#').substringAfterLast('@').substringBefore(':').lowercase(Locale.ROOT)
    }.getOrDefault("")

    fun isSecure(url: String) = url.startsWith("https://", true)
    fun display(url: String): String = url.removePrefix("https://").removePrefix("http://").removePrefix("www.").trimEnd('/')
}

/** Simple host blocklist: a request is blocked when its host (or a parent domain) is listed. */
object AdBlock {
    val hosts: Set<String> = setOf(
        "doubleclick.net", "googlesyndication.com", "googleadservices.com", "adservice.google.com", "pagead2.googlesyndication.com",
        "google-analytics.com", "googletagmanager.com", "googletagservices.com", "adnxs.com", "adsrvr.org", "ads.twitter.com",
        "advertising.com", "taboola.com", "outbrain.com", "criteo.com", "criteo.net", "rubiconproject.com", "pubmatic.com",
        "openx.net", "casalemedia.com", "amazon-adsystem.com", "moatads.com", "scorecardresearch.com", "quantserve.com",
        "adcolony.com", "unityads.unity3d.com", "applovin.com", "mopub.com", "inmobi.com", "popads.net", "popcash.net",
        "propellerads.com", "adsterra.com", "exoclick.com", "juicyads.com", "trafficjunky.net", "revcontent.com", "mgid.com",
        "smartadserver.com", "adform.net", "bidswitch.net", "media.net", "yieldmo.com", "zedo.com", "ads.yahoo.com",
    )
    fun isBlocked(host: String, enabled: Boolean = true): Boolean {
        if (!enabled || host.isBlank()) return false
        val parts = host.lowercase(Locale.ROOT).trimEnd('.').split('.')
        for (i in 0 until parts.size - 1) if (parts.drop(i).joinToString(".") in hosts) return true
        return false
    }
}

/** Desktop-mode user agent and dark-mode CSS fallback (Android 9 and older force-dark). */
object BrowserUa {
    const val DESKTOP = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    /** Mobile UA is the WebView default; we only mark desktop. Returns null to restore the WebView default. */
    fun ua(desktop: Boolean, default: String): String = if (desktop) DESKTOP else default
    const val DARK_CSS = "html{filter:invert(1) hue-rotate(180deg)}img,video,picture,canvas{filter:invert(1) hue-rotate(180deg)}"
}

/** Incognito clean-up: cookies set during incognito for hosts that no normal tab visited are expired on close. */
object IncognitoCleaner {
    fun parentDomains(host: String): List<String> {
        val parts = host.lowercase(Locale.ROOT).split('.').filter { it.isNotEmpty() }
        if (parts.size < 2) return listOf(host)
        return (0..parts.size - 2).map { parts.drop(it).joinToString(".") }
    }
    /** Set-Cookie strings that expire every cookie named in a Cookie header for these hosts. */
    fun expiryCookies(host: String, cookieHeader: String?): List<String> {
        if (cookieHeader.isNullOrBlank()) return emptyList()
        val names = cookieHeader.split(';').map { it.substringBefore('=').trim() }.filter { it.isNotEmpty() }
        return names.flatMap { n -> parentDomains(host).flatMap { d -> listOf("$n=; Max-Age=0; Path=/; Domain=$d", "$n=; Max-Age=0; Path=/; Domain=.$d") } + "$n=; Max-Age=0; Path=/" }
    }
    fun hostsToClear(incognitoHosts: Collection<String>, normalHosts: Collection<String>): Set<String> = incognitoHosts.toSet() - normalHosts.toSet()
}

data class HistoryEntry(val url: String, val title: String, val at: Long)
data class Bookmark(val url: String, val title: String, val at: Long)

/** History and bookmarks as plain data with a line-based format (tab separated, no JSON dependency). */
object BrowserLists {
    const val HISTORY_MAX = 500
    private fun clean(s: String) = s.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ').take(300)

    fun addHistory(list: List<HistoryEntry>, url: String, title: String, at: Long): List<HistoryEntry> {
        if (!(url.startsWith("http://") || url.startsWith("https://"))) return list
        val first = list.firstOrNull()
        val e = HistoryEntry(url, title.ifBlank { UrlInput.hostOf(url) }, at)
        // same page again right away: refresh the title and time instead of a duplicate row
        val rest = if (first != null && first.url == url) list.drop(1) else list
        return (listOf(e) + rest).take(HISTORY_MAX)
    }
    fun removeHistory(list: List<HistoryEntry>, url: String, at: Long) = list.filterNot { it.url == url && it.at == at }
    fun searchHistory(list: List<HistoryEntry>, q: String): List<HistoryEntry> {
        val k = q.trim().lowercase(Locale.ROOT); if (k.isEmpty()) return list
        return list.filter { it.url.lowercase(Locale.ROOT).contains(k) || it.title.lowercase(Locale.ROOT).contains(k) }
    }
    fun toggleBookmark(list: List<Bookmark>, url: String, title: String, at: Long): List<Bookmark> =
        if (list.any { it.url == url }) list.filterNot { it.url == url } else listOf(Bookmark(url, title.ifBlank { UrlInput.hostOf(url) }, at)) + list
    fun isBookmarked(list: List<Bookmark>, url: String) = list.any { it.url == url }

    fun encodeHistory(l: List<HistoryEntry>) = l.joinToString("\n") { "${it.at}\t${clean(it.url)}\t${clean(it.title)}" }
    fun decodeHistory(s: String?): List<HistoryEntry> = s.orEmpty().lines().mapNotNull {
        val p = it.split('\t'); if (p.size < 3) null else p[0].toLongOrNull()?.let { t -> HistoryEntry(p[1], p[2], t) }
    }
    fun encodeBookmarks(l: List<Bookmark>) = l.joinToString("\n") { "${it.at}\t${clean(it.url)}\t${clean(it.title)}" }
    fun decodeBookmarks(s: String?): List<Bookmark> = s.orEmpty().lines().mapNotNull {
        val p = it.split('\t'); if (p.size < 3) null else p[0].toLongOrNull()?.let { t -> Bookmark(p[1], p[2], t) }
    }
}

/** Download file name from Content-Disposition / URL, safe for the Downloads folder. */
object DownloadNames {
    fun fileName(url: String, contentDisposition: String?, mime: String?): String {
        var n: String? = null
        contentDisposition?.let { cd ->
            Regex("filename\\*?=(?:UTF-8'')?\"?([^\";]+)\"?", RegexOption.IGNORE_CASE).find(cd)?.groupValues?.get(1)?.let { n = runCatching { java.net.URLDecoder.decode(it, "UTF-8") }.getOrDefault(it) }
        }
        if (n.isNullOrBlank()) n = url.substringBefore('#').substringBefore('?').substringAfterLast('/').takeIf { it.isNotBlank() }?.let { runCatching { java.net.URLDecoder.decode(it, "UTF-8") }.getOrDefault(it) }
        var name = (n ?: "download").replace(Regex("[\\\\/:*?\"<>|\\u0000-\\u001f]"), "_").trim().trim('.').take(120)
        if (name.isBlank()) name = "download"
        if (!name.contains('.')) {
            val ext = when (mime?.lowercase(Locale.ROOT)) {
                "application/vnd.android.package-archive" -> "apk"; "application/pdf" -> "pdf"; "image/png" -> "png"; "image/jpeg" -> "jpg"; "application/zip" -> "zip"; "text/plain" -> "txt"; else -> null
            }
            if (ext != null) name += ".$ext"
        }
        return name
    }
}
