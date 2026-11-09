package com.techkadak.app.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import com.techkadak.app.BuildConfig
import com.techkadak.app.Config
import com.techkadak.app.data.Http
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

data class Release(val version: String, val notes: String, val apkUrl: String, val apkSize: Long, val pageUrl: String)

object Versions {
    /** "v1.2.3" / "1.2" / "1.2.3-beta" -> [1,2,3] */
    fun parts(v: String): List<Int> =
        v.trim().removePrefix("v").removePrefix("V").substringBefore('-').substringBefore('+')
            .split('.').map { it.filter(Char::isDigit).toIntOrNull() ?: 0 }

    fun isNewer(remote: String, current: String): Boolean {
        val a = parts(remote); val b = parts(current)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }; val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }
}

object ReleaseParser {
    /** GitHub "releases/latest" JSON -> Release (sirf .apk asset ho tabhi). */
    fun parse(json: String): Release? {
        val o = JSONObject(json)
        if (o.optBoolean("draft") || o.optBoolean("prerelease")) return null
        val tag = o.optString("tag_name").ifBlank { return null }
        val assets = o.optJSONArray("assets") ?: return null
        for (i in 0 until assets.length()) {
            val a = assets.getJSONObject(i)
            val name = a.optString("name")
            val url = a.optString("browser_download_url")
            if (name.endsWith(".apk", true) && url.startsWith("https://")) {
                return Release(tag.removePrefix("v"), o.optString("body").trim(), url, a.optLong("size"), o.optString("html_url"))
            }
        }
        return null
    }
}

/** Version history ka ek item (GitHub releases list). */
data class ReleaseInfo(val version: String, val date: String, val notes: String, val url: String)

object ReleaseListParser {
    fun parse(json: String): List<ReleaseInfo> {
        val arr = org.json.JSONArray(json)
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            if (o.optBoolean("draft") || o.optBoolean("prerelease")) return@mapNotNull null
            val tag = o.optString("tag_name").ifBlank { return@mapNotNull null }
            ReleaseInfo(tag.removePrefix("v"), o.optString("published_at").take(10), o.optString("body").trim(), o.optString("html_url"))
        }.sortedWith { a, b -> if (Versions.isNewer(a.version, b.version)) -1 else if (Versions.isNewer(b.version, a.version)) 1 else 0 }
    }
}

sealed interface DlState {
    data object Idle : DlState
    data class Running(val progress: Float) : DlState
    data class Done(val file: File) : DlState
    data class Failed(val why: String) : DlState
}

/** In-app update: GitHub Releases -> DownloadManager -> Android installer. */
class Updater(private val context: Context) {

    suspend fun check(): Release? = withContext(Dispatchers.IO) {
        runCatching {
            val text = Http.getText(Config.LATEST_RELEASE_API, accept = "application/vnd.github+json")
            ReleaseParser.parse(text)?.takeIf { Versions.isNewer(it.version, BuildConfig.VERSION_NAME) }
        }.getOrNull()
    }

    private val historyCache get() = File(context.filesDir, "releases.json")

    /** "Kya naya hai" history: GitHub se, net na ho to last saved copy. */
    suspend fun history(): List<ReleaseInfo> = withContext(Dispatchers.IO) {
        val live = runCatching {
            val text = Http.getText("https://api.github.com/repos/${Config.GITHUB_OWNER}/${Config.GITHUB_REPO}/releases?per_page=20", accept = "application/vnd.github+json")
            ReleaseListParser.parse(text).also { historyCache.writeText(text) }
        }.getOrNull()
        live ?: runCatching { ReleaseListParser.parse(historyCache.readText()) }.getOrDefault(emptyList())
    }

    /** Phone pe abhi install version (PackageManager se — update ke baad apne aap naya dikhega). */
    @Suppress("DEPRECATION")
    fun installed(): Pair<String, Long> = runCatching {
        val p = context.packageManager.getPackageInfo(context.packageName, 0)
        (p.versionName ?: BuildConfig.VERSION_NAME) to (if (Build.VERSION.SDK_INT >= 28) p.longVersionCode else p.versionCode.toLong())
    }.getOrDefault(BuildConfig.VERSION_NAME to BuildConfig.VERSION_CODE.toLong())

    private fun dir(): File = (context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: File(context.cacheDir, "Download")).apply { mkdirs() }
    private fun fileFor(r: Release) = File(dir(), "TechKadak-v${r.version}.apk")

    private fun target(r: Release): File {
        dir().listFiles()?.filter { it.name.endsWith(".apk") }?.forEach { it.delete() }
        return fileFor(r)
    }

    /** Pehle se download hua (aur sahi) APK — app band hone ke baad bhi seedha INSTALL. */
    fun readyFile(r: Release): File? = fileFor(r).takeIf { it.exists() && verify(it) == null }

    /**
     * Download ke baad check: wahi app (package), naya versionCode, aur wahi signing key jisse ye app bana.
     * null = sab theek, warna Hinglish wajah.
     */
    @Suppress("DEPRECATION")
    fun verify(file: File): String? = runCatching {
        val pm = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= 28) android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES else android.content.pm.PackageManager.GET_SIGNATURES
        val info = pm.getPackageArchiveInfo(file.path, flags) ?: return "APK kharab hai (poora download nahi hua)"
        if (info.packageName != context.packageName) return "Ye Tech Kadak ka APK nahi hai"
        val newCode = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
        if (newCode <= BuildConfig.VERSION_CODE) return "Ye version naya nahi hai"
        val mine = pm.getPackageInfo(context.packageName, flags)
        fun certs(p: android.content.pm.PackageInfo): Set<String> = (
            if (Build.VERSION.SDK_INT >= 28) p.signingInfo?.apkContentsSigners?.toList() else p.signatures?.toList()
            ).orEmpty().map { it.toCharsString() }.toSet()
        val a = certs(info); val b = certs(mine)
        if (a.isNotEmpty() && b.isNotEmpty() && a.intersect(b).isEmpty()) return "Signature match nahi hua — ye official update nahi lagta"
        null
    }.getOrElse { null }

    /** Download karta hai aur progress batata hai. */
    suspend fun download(r: Release, onState: (DlState) -> Unit) = withContext(Dispatchers.IO) {
        val file = target(r)
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val req = DownloadManager.Request(Uri.parse(r.apkUrl))
            .setTitle("Tech Kadak v${r.version}")
            .setDescription("Naya update download ho raha hai")
            .setMimeType("application/vnd.android.package-archive")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setDestinationUri(Uri.fromFile(file))
        val id = runCatching { dm.enqueue(req) }.getOrElse { onState(DlState.Failed("Download shuru nahi hua")); return@withContext }
        onState(DlState.Running(0f))
        while (true) {
            delay(400)
            dm.query(DownloadManager.Query().setFilterById(id)).use { c ->
                if (c == null || !c.moveToFirst()) { onState(DlState.Failed("Download cancel ho gaya")); return@withContext }
                val status = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                val done = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val total = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)).takeIf { it > 0 } ?: r.apkSize
                when (status) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        val bad = verify(file)
                        if (bad != null) { file.delete(); onState(DlState.Failed(bad)) }
                        else {
                            onState(DlState.Done(file))
                            // App background me ho to bhi pata chale
                            com.techkadak.app.notify.Notifier.showUpdateReady(context, r.version)
                        }
                        return@withContext
                    }
                    DownloadManager.STATUS_FAILED -> { onState(DlState.Failed("Net check karke dobara try karo")); return@withContext }
                    else -> onState(DlState.Running(if (total > 0) (done.toFloat() / total).coerceIn(0f, 1f) else 0f))
                }
            }
        }
    }

    /** Android 8+ pe "Install unknown apps" ki permission chahiye. */
    fun canInstall(): Boolean = Build.VERSION.SDK_INT < 26 || context.packageManager.canRequestPackageInstalls()

    fun openInstallPermission() {
        if (Build.VERSION.SDK_INT >= 26) {
            context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + context.packageName))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    fun install(file: File) {
        val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
        val i = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(i)
    }
}
