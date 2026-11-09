package com.techkadak.app.install

import android.content.Context
import com.techkadak.app.data.AppItem
import com.techkadak.app.data.Http
import com.techkadak.app.update.Versions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection

/** Kisi app ka APK kahan se aata hai — hamesha app ka OFFICIAL source, tap ke time live (hum APK re-host nahi karte). */
data class ApkSource(
    val kind: Kind,
    /** "github:owner/repo" | "fdroid:pkg" | "url:https://…" (kind=APK), warna null */
    val source: String?,
    val packageName: String?,
    val label: String,
    val assetHint: String? = null,
    val link: String? = null,
    /** Kam se kam Android SDK (e.g. 31 = Android 12). 0 = pata nahi. */
    val minSdk: Int = 0,
    val assetFallback: String? = null,
) {
    enum class Kind { APK, PLAY, WEB }

    val sourceLabel: String get() = when {
        source == null -> label
        source.startsWith("github:") -> "Official GitHub"
        source.startsWith("fdroid:") -> "F-Droid"
        else -> "Developer site"
    }

    companion object {
        fun fromJson(o: JSONObject): ApkSource? {
            val src = o.optString("apk_source").trim().takeIf { it.isNotEmpty() && it != "null" }
            val pkg = o.optString("package_name").trim().takeIf { it.isNotEmpty() && it != "null" }
            val kindS = o.optString("kind").lowercase()
            val kind = when {
                src != null && valid(src) -> Kind.APK
                kindS == "play" -> Kind.PLAY
                kindS == "web" -> Kind.WEB
                pkg != null -> Kind.PLAY
                else -> return null
            }
            return ApkSource(kind, src?.takeIf { kind == Kind.APK }, pkg, o.optString("label").ifBlank { if (kind == Kind.WEB) "PC / Web" else "Play Store" },
                o.optString("asset_hint").takeIf { it.isNotBlank() },
                (o.optString("url").takeIf { it.startsWith("https://") } ?: o.optString("link").takeIf { it.startsWith("https://") }),
                o.optInt("min_sdk", 0), o.optString("asset_fallback").takeIf { it.isNotBlank() })
        }

        /** Sirf ye 3 tarah ke official source chalte hain. */
        fun valid(src: String): Boolean = when {
            src.startsWith("github:") -> !src.contains("..") && Regex("^github:[A-Za-z0-9][A-Za-z0-9-]*/[A-Za-z0-9_][A-Za-z0-9_.-]*$").matches(src)
            src.startsWith("fdroid:") -> Regex("^fdroid:[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+$").matches(src)
            src.startsWith("url:https://") -> true
            else -> false
        }

        /** Bundled mapping (assets/apk_sources.json). apps.json me apk_source/package_name ho to wo jeet-ta hai. */
        fun loadBundled(context: Context): Map<String, ApkSource> = runCatching {
            val o = JSONObject(context.assets.open("apk_sources.json").bufferedReader().use { it.readText() }).getJSONObject("apps")
            o.keys().asSequence().mapNotNull { k -> fromJson(o.getJSONObject(k))?.let { k to it } }.toMap()
        }.getOrDefault(emptyMap())

        /** Mapping na ho to app ke links se andaaza: Play link = PLAY, sirf website = WEB. */
        fun guess(app: AppItem): ApkSource {
            val play = app.links.firstOrNull { it.url.contains("play.google.com/store/apps/details") }
            if (play != null) {
                val pkg = Regex("[?&]id=([A-Za-z0-9_.]+)").find(play.url)?.groupValues?.get(1)
                return ApkSource(Kind.PLAY, null, pkg, "Play Store", link = play.url)
            }
            val android = (app.platforms ?: "").contains("android", true)
            return ApkSource(Kind.WEB, null, null, if (android) "Official site" else "PC / Web", link = app.downloadLinks.firstOrNull()?.url)
        }
    }
}

/** Tap ke time live nikala gaya APK. */
data class ResolvedApk(
    val url: String,
    val size: Long,
    val versionName: String?,
    val versionCode: Long?,
    val sha256: String?,
    val fileName: String,
)

object ApkResolver {
    private val BAD_ABI = Regex("(x86|x86_64|armeabi-v7a|armeabi|mips)", RegexOption.IGNORE_CASE)

    /** GitHub release assets me se sahi APK: hint > universal > arm64 > koi bhi (x86/v7a nahi). */
    fun pickGithubAsset(release: JSONObject, hint: String?, strict: Boolean = false): ResolvedApk? {
        val assets = release.optJSONArray("assets") ?: return null
        val apks = (0 until assets.length()).map { assets.getJSONObject(it) }
            .filter { it.optString("name").endsWith(".apk", true) && it.optString("browser_download_url").startsWith("https://") }
        if (apks.isEmpty()) return null
        val hints = hint?.split('\n')?.mapNotNull { h -> h.takeIf { it.isNotBlank() }?.let { runCatching { Regex(it, RegexOption.IGNORE_CASE) }.getOrNull() } }.orEmpty()
        val byHint = hints.firstNotNullOfOrNull { re -> apks.firstOrNull { re.containsMatchIn(it.optString("name")) } }
        if (strict && hints.isNotEmpty() && byHint == null) return null
        val pick = byHint
            ?: apks.firstOrNull { it.optString("name").contains("universal", true) }
            ?: apks.firstOrNull { it.optString("name").contains("arm64", true) }
            ?: apks.firstOrNull { !BAD_ABI.containsMatchIn(it.optString("name")) }
            ?: apks.first()
        val digest = pick.optString("digest").takeIf { it.startsWith("sha256:") }?.removePrefix("sha256:")
        return ResolvedApk(
            pick.getString("browser_download_url"), pick.optLong("size"),
            release.optString("tag_name").removePrefix("v").ifBlank { null }, null, digest, pick.optString("name"),
        )
    }

    /** F-Droid API -> repo APK URL. */
    fun fdroidUrl(pkg: String, json: JSONObject): Pair<String, Long>? {
        val code = json.optLong("suggestedVersionCode", -1).takeIf { it > 0 } ?: return null
        return "https://f-droid.org/repo/${pkg}_$code.apk" to code
    }

    suspend fun resolve(s: ApkSource): ResolvedApk = withContext(Dispatchers.IO) {
        val src = s.source ?: error("Is app ka APK source nahi hai")
        when {
            src.startsWith("github:") -> {
                val repo = src.removePrefix("github:")
                val hint = listOfNotNull(s.assetHint, s.assetFallback).joinToString("\n").ifBlank { null }
                val j = JSONObject(Http.getText("https://api.github.com/repos/$repo/releases/latest", accept = "application/vnd.github+json"))
                pickGithubAsset(j, hint, strict = true) ?: run {
                    // Repo me dusre app ki release latest ho sakti hai (e.g. Bitwarden Authenticator) — haal ki releases me dhoondo
                    val arr = org.json.JSONArray(Http.getText("https://api.github.com/repos/$repo/releases?per_page=15", accept = "application/vnd.github+json"))
                    (0 until arr.length()).map { arr.getJSONObject(it) }.filter { !it.optBoolean("draft") && !it.optBoolean("prerelease") }
                        .firstNotNullOfOrNull { pickGithubAsset(it, hint, strict = true) }
                } ?: pickGithubAsset(j, hint) ?: error("Is release me APK nahi mila")
            }
            src.startsWith("fdroid:") -> {
                val pkg = src.removePrefix("fdroid:")
                val j = JSONObject(Http.getText("https://f-droid.org/api/v1/packages/$pkg"))
                val (url, code) = fdroidUrl(pkg, j) ?: error("F-Droid pe version nahi mila")
                val name = j.optJSONArray("packages")?.let { arr ->
                    (0 until arr.length()).map { arr.getJSONObject(it) }.firstOrNull { it.optLong("versionCode") == code }?.optString("versionName")
                }
                ResolvedApk(url, headSize(url), name, code, null, "${pkg}_$code.apk")
            }
            else -> {
                val url = src.removePrefix("url:")
                ResolvedApk(url, headSize(url), null, null, null, url.substringAfterLast('/').substringBefore('?').ifBlank { "app.apk" })
            }
        }
    }

    private fun headSize(url: String): Long = runCatching {
        val c = Http.open(url); c.requestMethod = "HEAD"
        try { if (c.responseCode in 200..299) c.contentLengthLong else -1L } finally { c.disconnect() }
    }.getOrDefault(-1L)

    /** Installed version purana hai? (F-Droid: versionCode, GitHub: version name). */
    fun isUpdate(installedName: String?, installedCode: Long, r: ResolvedApk): Boolean = when {
        r.versionCode != null -> r.versionCode > installedCode
        r.versionName != null && installedName != null -> Versions.isNewer(r.versionName, installedName)
        else -> false
    }
}

@Suppress("unused")
private fun HttpURLConnection.safeClose() = runCatching { disconnect() }
