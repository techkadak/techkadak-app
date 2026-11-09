package com.techkadak.app.install

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.techkadak.app.MainActivity
import com.techkadak.app.R
import com.techkadak.app.data.Http
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest

enum class DlStatus { QUEUED, RUNNING, PAUSED, WAITING_NET, DONE, FAILED }

data class DlItem(
    val slug: String,
    val name: String,
    val iconUrl: String?,
    val sourceLabel: String,
    val source: String,
    val packageName: String?,
    val assetHint: String?,
    val status: DlStatus,
    val done: Long = 0,
    val total: Long = -1,
    val url: String? = null,
    val sha256: String? = null,
    val versionName: String? = null,
    val error: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    /** Auto-update ne lagaya (download ke baad khud install). */
    val auto: Boolean = false,
) {
    val progress: Float get() = if (total > 0) (done.toFloat() / total).coerceIn(0f, 1f) else 0f
    val active: Boolean get() = status == DlStatus.QUEUED || status == DlStatus.RUNNING || status == DlStatus.WAITING_NET

    fun toJson() = JSONObject().apply {
        put("slug", slug); put("name", name); put("icon", iconUrl); put("label", sourceLabel); put("source", source)
        put("pkg", packageName); put("hint", assetHint); put("status", status.name); put("done", done); put("total", total)
        put("url", url); put("sha", sha256); put("ver", versionName); put("err", error); put("at", updatedAt); put("auto", auto)
    }

    companion object {
        private fun JSONObject.s(k: String) = if (isNull(k)) null else optString(k).takeIf { it.isNotEmpty() }
        fun fromJson(o: JSONObject) = DlItem(
            o.getString("slug"), o.optString("name"), o.s("icon"), o.optString("label"), o.optString("source"), o.s("pkg"), o.s("hint"),
            runCatching { DlStatus.valueOf(o.optString("status")) }.getOrDefault(DlStatus.PAUSED),
            o.optLong("done"), o.optLong("total", -1), o.s("url"), o.s("sha"), o.s("ver"), o.s("err"), o.optLong("at"), o.optBoolean("auto"),
        )
    }
}

/**
 * Downloads ki list (queue). Disk pe save hoti hai, isliye app band ho / phone restart ho tab bhi yaad rehti hai.
 * Asli download WorkManager worker karta hai (background me, notification ke saath, net jaane pe wahi se resume).
 */
object ApkDownloads {
    private val _items = MutableStateFlow<Map<String, DlItem>>(emptyMap())
    val items: StateFlow<Map<String, DlItem>> = _items
    private var file: File? = null
    private var lastSave = 0L
    const val QUEUE = "tk_apk_queue"
    const val CHANNEL = "downloads"
    const val EXTRA_INSTALL = "tk_install_slug"
    const val EXTRA_DOWNLOADS = "tk_open_downloads"

    @Synchronized
    fun init(context: Context) {
        if (file != null) return
        val f = File(context.filesDir, "downloads.json"); file = f
        val loaded = runCatching {
            val arr = JSONArray(f.readText())
            (0 until arr.length()).map { DlItem.fromJson(arr.getJSONObject(it)) }.associateBy { it.slug }
        }.getOrDefault(emptyMap())
        // App kill hua tha jab RUNNING tha -> worker WorkManager se phir chalega; tab tak QUEUED dikhao
        _items.value = loaded.mapValues { (_, v) -> if (v.status == DlStatus.RUNNING) v.copy(status = DlStatus.QUEUED) else v }
    }

    fun dir(context: Context): File = File(context.getExternalFilesDir(null) ?: context.filesDir, "Download/apps").apply { mkdirs() }
    fun partFile(context: Context, slug: String) = File(dir(context), "$slug.apk.part")
    fun apkFile(context: Context, slug: String) = File(dir(context), "$slug.apk")

    @Synchronized
    fun put(item: DlItem, force: Boolean = true) {
        _items.value = _items.value + (item.slug to item.copy(updatedAt = System.currentTimeMillis()))
        val now = System.currentTimeMillis()
        if (force || now - lastSave > 1500) { lastSave = now; save() }
    }

    @Synchronized
    fun remove(context: Context, slug: String) {
        _items.value = _items.value - slug
        partFile(context, slug).delete(); apkFile(context, slug).delete()
        save()
    }

    private fun save() {
        val f = file ?: return
        runCatching { f.writeText(JSONArray(_items.value.values.map { it.toJson() }).toString()) }
    }

    fun get(slug: String) = _items.value[slug]

    /** Queue me daalo (ek ke baad ek). */
    fun enqueue(context: Context, item: DlItem, wifiOnly: Boolean = false) {
        init(context)
        put(item.copy(status = DlStatus.QUEUED, error = null))
        val req = OneTimeWorkRequestBuilder<ApkDownloadWorker>()
            .setInputData(workDataOf("slug" to item.slug))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED).build())
            .addTag("apk:" + item.slug)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(QUEUE, ExistingWorkPolicy.APPEND_OR_REPLACE, req)
    }

    fun pause(context: Context, slug: String) {
        val it = get(slug) ?: return
        put(it.copy(status = DlStatus.PAUSED))
        WorkManager.getInstance(context).cancelAllWorkByTag("apk:$slug")
    }

    fun resume(context: Context, slug: String) { get(slug)?.let { enqueue(context, it) } }

    fun cancel(context: Context, slug: String) {
        WorkManager.getInstance(context).cancelAllWorkByTag("apk:$slug")
        NotificationManagerCompat.from(context).cancel(notifId(slug))
        remove(context, slug)
    }

    fun notifId(slug: String) = 7000 + (slug.hashCode() and 0xfff)

    fun channel(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel(CHANNEL, "Downloads", NotificationManager.IMPORTANCE_LOW)
            ch.description = "Apps ka download progress"
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    fun openIntent(context: Context, slug: String?, install: Boolean): PendingIntent {
        val i = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_DOWNLOADS, true)
            if (install && slug != null) putExtra(EXTRA_INSTALL, slug)
        }
        return PendingIntent.getActivity(context, ((slug ?: "dl") + install).hashCode(), i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun installIntent(context: Context, slug: String): Intent? {
        val f = apkFile(context, slug).takeIf { it.exists() } ?: return null
        val uri = FileProvider.getUriForFile(context, context.packageName + ".files", f)
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /** System installer kholo (Android "Install" / "Update" tap maangega — wo hata nahi sakte). */
    fun install(context: Context, slug: String): Boolean {
        val i = installIntent(context, slug) ?: return false
        return runCatching { context.startActivity(i); true }.getOrDefault(false)
    }

    fun cancelIntent(context: Context, slug: String): PendingIntent = PendingIntent.getBroadcast(
        context, ("cancel" + slug).hashCode(),
        Intent(context, DownloadActionReceiver::class.java).setAction(ACTION_CANCEL).putExtra("slug", slug),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    const val ACTION_CANCEL = "com.techkadak.app.CANCEL_DOWNLOAD"

    /** Phone pe installed? -> (versionName, versionCode) */
    @Suppress("DEPRECATION")
    fun installed(context: Context, pkg: String?): Pair<String?, Long>? {
        if (pkg.isNullOrBlank()) return null
        return runCatching {
            val p = context.packageManager.getPackageInfo(pkg, 0)
            p.versionName to (if (Build.VERSION.SDK_INT >= 28) p.longVersionCode else p.versionCode.toLong())
        }.getOrNull()
    }

    fun launch(context: Context, pkg: String): Boolean {
        val i = context.packageManager.getLaunchIntentForPackage(pkg) ?: return false
        runCatching { items.value.values.firstOrNull { it.packageName == pkg }?.slug?.let { com.techkadak.app.stats.DeviceRecord.markApp(context, it, "o") } }
        return runCatching { context.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true }.getOrDefault(false)
    }

    fun sha256(f: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        f.inputStream().use { s -> val b = ByteArray(64 * 1024); while (true) { val n = s.read(b); if (n < 0) break; md.update(b, 0, n) } }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}

/** Ek APK download: Range header se resume, foreground notification me %, net gaya to retry (wahi se). */
class ApkDownloadWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    private val c get() = applicationContext

    private fun notif(item: DlItem, indeterminate: Boolean = false): android.app.Notification {
        ApkDownloads.channel(c)
        val pct = (item.progress * 100).toInt()
        return NotificationCompat.Builder(c, ApkDownloads.CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_tk).setColor(0xFF9BFF2E.toInt())
            .setContentTitle("${item.name} download ho raha hai")
            .setContentText(if (indeterminate) "Shuru ho raha hai… • ${item.sourceLabel}" else "$pct% • ${mb(item.done)} / ${mb(item.total)} • ${item.sourceLabel}")
            .setProgress(100, pct, indeterminate || item.total <= 0)
            .setOngoing(true).setOnlyAlertOnce(true)
            .setContentIntent(ApkDownloads.openIntent(c, item.slug, false))
            .addAction(0, "Cancel", ApkDownloads.cancelIntent(c, item.slug))
            .build()
    }

    private suspend fun fg(item: DlItem, indeterminate: Boolean = false) {
        val n = notif(item, indeterminate)
        val info = if (Build.VERSION.SDK_INT >= 29) ForegroundInfo(ApkDownloads.notifId(item.slug), n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        else ForegroundInfo(ApkDownloads.notifId(item.slug), n)
        runCatching { setForeground(info) }
    }

    override suspend fun doWork(): Result {
        ApkDownloads.init(c)
        val slug = inputData.getString("slug") ?: return Result.failure()
        var item = ApkDownloads.get(slug) ?: return Result.success()
        if (item.status == DlStatus.PAUSED || item.status == DlStatus.DONE) return Result.success()
        item = item.copy(status = DlStatus.RUNNING, error = null); ApkDownloads.put(item)
        fg(item, indeterminate = true)
        val part = ApkDownloads.partFile(c, slug)
        try {
            // 1) Official source se live link (pehli baar)
            if (item.url == null) {
                val r = ApkResolver.resolve(ApkSource(ApkSource.Kind.APK, item.source, item.packageName, item.sourceLabel, item.assetHint))
                item = item.copy(url = r.url, total = if (r.size > 0) r.size else item.total, sha256 = r.sha256, versionName = r.versionName)
                part.delete()
                ApkDownloads.put(item)
            }
            // 2) Download (resume)
            var done = if (part.exists()) part.length() else 0L
            val conn = Http.open(item.url!!)
            conn.readTimeout = 30_000
            if (done > 0) conn.setRequestProperty("Range", "bytes=$done-")
            val code = conn.responseCode
            if (code == 416) { conn.disconnect() } else {
                if (code !in 200..299) { conn.disconnect(); throw IOException("HTTP $code") }
                if (code == 200 && done > 0) { done = 0; part.delete() } // server ne resume nahi kiya
                val len = conn.contentLengthLong
                val total = if (len > 0) done + len else item.total
                item = item.copy(done = done, total = total); ApkDownloads.put(item)
                var lastUi = 0L
                conn.inputStream.use { inp ->
                    FileOutputStream(part, true).use { out ->
                        val buf = ByteArray(64 * 1024)
                        while (true) {
                            if (isStopped) return Result.success()
                            val n = inp.read(buf); if (n < 0) break
                            out.write(buf, 0, n); done += n
                            val now = System.currentTimeMillis()
                            if (now - lastUi > 400) {
                                lastUi = now; item = item.copy(done = done); ApkDownloads.put(item, force = false); fg(item)
                            }
                        }
                    }
                }
                conn.disconnect()
            }
            // 3) Check: size, SHA-256 (source de to), package name
            val apk = ApkDownloads.apkFile(c, slug)
            if (item.total > 0 && part.length() != item.total) throw IOException("adhoora")
            item.sha256?.let { want ->
                val got = ApkDownloads.sha256(part)
                if (!got.equals(want, true)) { part.delete(); return fail(item, "SHA-256 match nahi hua — file kharab, dobara try karo") }
            }
            @Suppress("DEPRECATION")
            val info = c.packageManager.getPackageArchiveInfo(part.path, 0)
            if (info == null) { part.delete(); return fail(item, "Ye sahi APK nahi hai") }
            if (item.packageName != null && info.packageName != item.packageName) { part.delete(); return fail(item, "Package match nahi hua (${info.packageName})") }
            apk.delete(); part.renameTo(apk)
            item = item.copy(status = DlStatus.DONE, done = apk.length(), total = apk.length(), versionName = item.versionName ?: info.versionName)
            ApkDownloads.put(item)
            if (item.auto) {
                // Auto-update: Android 12+ pe (jo app humne install kiya) bina tap ke update; warna "Update ready — tap to install"
                if (!ApkInstaller.install(c, slug, background = true)) doneNotif(item)
            } else doneNotif(item)
            return Result.success()
        } catch (e: IOException) {
            // Net gaya: wahi se resume (WorkManager net aane pe phir chalayega)
            if (isStopped) return Result.success()
            return if (runAttemptCount < 8) {
                ApkDownloads.put(item.copy(status = DlStatus.WAITING_NET, done = if (part.exists()) part.length() else 0, error = null)); Result.retry()
            } else fail(item, "Net check karke dobara try karo")
        } catch (e: Exception) {
            return fail(item, e.message ?: "Download nahi hua")
        }
    }

    private fun fail(item: DlItem, why: String): Result {
        ApkDownloads.put(item.copy(status = DlStatus.FAILED, error = why))
        runCatching {
            NotificationManagerCompat.from(c).notify(ApkDownloads.notifId(item.slug), NotificationCompat.Builder(c, ApkDownloads.CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_tk).setColor(0xFF9BFF2E.toInt())
                .setContentTitle("${item.name}: download ruk gaya 😢").setContentText(why).setAutoCancel(true)
                .setContentIntent(ApkDownloads.openIntent(c, item.slug, false)).build())
        }
        return Result.failure()
    }

    private fun doneNotif(item: DlItem) = runCatching {
        NotificationManagerCompat.from(c).notify(ApkDownloads.notifId(item.slug) + 1, NotificationCompat.Builder(c, ApkDownloads.CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_tk).setColor(0xFF9BFF2E.toInt())
            .setContentTitle("Download complete — Install ✅").setContentText("${item.name} ready hai. Tap karo, Android Install poochega.")
            .setAutoCancel(true).setContentIntent(
                // Tap = seedha Android installer
                ApkDownloads.installIntent(c, item.slug)?.let { PendingIntent.getActivity(c, ("inst" + item.slug).hashCode(), it, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE) }
                    ?: ApkDownloads.openIntent(c, item.slug, true),
            ).build())
    }

    companion object { fun mb(b: Long) = if (b <= 0) "?" else "%.1f MB".format(b / 1_048_576f) }
}

/** Notification ka "Cancel" button. */
class DownloadActionReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ApkDownloads.ACTION_CANCEL) {
            ApkDownloads.init(context)
            intent.getStringExtra("slug")?.let { ApkDownloads.cancel(context, it) }
        }
    }
}
