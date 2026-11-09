package com.techkadak.app.data

import android.content.Context
import com.techkadak.app.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

enum class DataSource { LIVE, CACHE, BUNDLED }

data class AppsResult(val apps: List<AppItem>, val source: DataSource, val savedAt: Long)

/**
 * Apps website ke data/apps.json se aate hain (no Firebase, no server).
 * Offline: last saved copy, aur pehli baar ke liye APK ke andar ek bundled copy.
 */
class AppRepository(private val context: Context) {
    private val cacheFile get() = File(context.filesDir, "apps.json")

    suspend fun loadLocal(): AppsResult = withContext(Dispatchers.IO) {
        val f = cacheFile
        if (f.exists()) {
            runCatching { AppsResult(AppsParser.parse(f.readText()), DataSource.CACHE, f.lastModified()) }
                .getOrNull()?.takeIf { it.apps.isNotEmpty() }?.let { return@withContext it }
        }
        val text = context.assets.open("apps.json").bufferedReader().use { it.readText() }
        AppsResult(AppsParser.parse(text), DataSource.BUNDLED, 0L)
    }

    /** Website se taaza list. Fail ho to null (purani list hi dikhegi). */
    suspend fun fetchRemote(): AppsResult? = withContext(Dispatchers.IO) {
        runCatching {
            val text = Http.getText(Config.APPS_JSON_URL + "?t=" + (System.currentTimeMillis() / 600_000))
            val apps = AppsParser.parse(text)
            if (apps.isEmpty()) return@runCatching null
            val tmp = File(context.filesDir, "apps.json.tmp")
            tmp.writeText(text)
            tmp.renameTo(cacheFile)
            AppsResult(apps, DataSource.LIVE, System.currentTimeMillis())
        }.getOrNull()
    }
}

object Http {
    const val UA = "TechKadakApp (Android)"

    fun open(url: String, accept: String? = null): HttpURLConnection {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 15_000
        c.readTimeout = 20_000
        c.instanceFollowRedirects = true
        c.setRequestProperty("User-Agent", UA)
        if (accept != null) c.setRequestProperty("Accept", accept)
        return c
    }

    fun getText(url: String, accept: String? = null): String {
        val c = open(url, accept)
        try {
            val code = c.responseCode
            if (code !in 200..299) throw HttpError(code)
            return c.inputStream.bufferedReader().use { it.readText() }
        } finally {
            c.disconnect()
        }
    }

    fun getBytes(url: String, max: Int = 2_000_000): ByteArray {
        val c = open(url)
        try {
            if (c.responseCode !in 200..299) throw HttpError(c.responseCode)
            val bytes = c.inputStream.use { it.readBytes() }
            if (bytes.size > max) throw HttpError(413)
            return bytes
        } finally {
            c.disconnect()
        }
    }
}

class HttpError(val code: Int) : Exception("HTTP $code")
