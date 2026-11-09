package com.techkadak.app.install

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.techkadak.app.R
import com.techkadak.app.data.AppItem
import com.techkadak.app.data.AppRepository
import com.techkadak.app.data.AutoUpdateMode
import com.techkadak.app.data.SettingsStore
import com.techkadak.app.update.Updater
import java.util.concurrent.TimeUnit

/**
 * Install PackageInstaller session se — taaki Tech Kadak "installer" bane aur Android 12+ pe
 * apne install kiye apps ko auto-update bina tap ke kar sake (USER_ACTION_NOT_REQUIRED).
 * Pehli baar install pe Android hamesha ek "Install" tap maangta hai.
 */
object ApkInstaller {
    const val ACTION_STATUS = "com.techkadak.app.INSTALL_STATUS"

    fun install(context: Context, slug: String, background: Boolean): Boolean {
        val f = ApkDownloads.apkFile(context, slug).takeIf { it.exists() } ?: return false
        return runCatching {
            val pi = context.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            params.setSize(f.length())
            ApkDownloads.get(slug)?.packageName?.let { params.setAppPackageName(it) }
            if (Build.VERSION.SDK_INT >= 31) params.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            val id = pi.createSession(params)
            pi.openSession(id).use { s ->
                s.openWrite("app.apk", 0, f.length()).use { out -> f.inputStream().use { it.copyTo(out) }; s.fsync(out) }
                val cb = Intent(context, InstallStatusReceiver::class.java).setAction(ACTION_STATUS)
                    .putExtra("slug", slug).putExtra("bg", background)
                val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0)
                s.commit(PendingIntent.getBroadcast(context, id, cb, flags).intentSender)
            }
            true
        }.getOrElse { if (!background) ApkDownloads.install(context, slug) else false }
    }

    fun notify(context: Context, slug: String, title: String, body: String, pi: PendingIntent?) = runCatching {
        ApkDownloads.channel(context)
        NotificationManagerCompat.from(context).notify(ApkDownloads.notifId(slug) + 2, NotificationCompat.Builder(context, ApkDownloads.CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_tk).setColor(0xFF9BFF2E.toInt()).setContentTitle(title).setContentText(body)
            .setAutoCancel(true).apply { if (pi != null) setContentIntent(pi) }.build())
    }
}

class InstallStatusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ApkDownloads.init(context)
        val slug = intent.getStringExtra("slug") ?: return
        val bg = intent.getBooleanExtra("bg", false)
        val item = ApkDownloads.get(slug)
        val name = item?.name ?: slug
        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                @Suppress("DEPRECATION")
                val confirm = (if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java) else intent.getParcelableExtra(Intent.EXTRA_INTENT)) ?: return
                confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                val started = !bg && runCatching { context.startActivity(confirm); true }.getOrDefault(false)
                if (!started) ApkInstaller.notify(context, slug, "Update ready — tap to install", "$name ka naya version download ho gaya",
                    PendingIntent.getActivity(context, ("confirm$slug").hashCode(), confirm, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            }
            PackageInstaller.STATUS_SUCCESS -> {
                ApkDownloads.apkFile(context, slug).delete()
                item?.let { ApkDownloads.put(it.copy(status = DlStatus.DONE, error = null)) }
                com.techkadak.app.stats.Stats.event(context, com.techkadak.app.stats.Stats.INSTALL_DONE, slug = slug)
                runCatching { com.techkadak.app.stats.DeviceRecord.markApp(context, slug, "i") }
                if (bg) ApkInstaller.notify(context, slug, "$name update ho gaya ✅", "Kadak Bot ne background me update kar diya", null)
            }
            PackageInstaller.STATUS_FAILURE_ABORTED -> Unit // user ne cancel kiya
            else -> {
                val msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: ""
                val why = if (msg.contains("INCOMPATIBLE", true) || msg.contains("signature", true) || intent.getIntExtra(PackageInstaller.EXTRA_STATUS, 0) == PackageInstaller.STATUS_FAILURE_CONFLICT)
                    "Phone wala version dusri jagah (Play Store) se hai — signature alag. Play Store se update karo."
                else "Install nahi hua ${msg.take(80)}".trim()
                item?.let { ApkDownloads.put(it.copy(status = DlStatus.FAILED, error = why)) }
                if (bg) ApkInstaller.notify(context, slug, "$name update nahi hua", why, ApkDownloads.openIntent(context, slug, false))
            }
        }
    }
}

/** Roz 2 baar: Tech Kadak ka apna update pehle, phir catalog ke installed apps ke naye version (background me download + update). */
class AutoUpdateWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val c = applicationContext
        ApkDownloads.init(c)
        val mode = SettingsStore(c).current().autoUpdate
        if (mode == AutoUpdateMode.OFF) return Result.success()
        // 1) Tech Kadak khud — sabse pehle
        Updater(c).check()?.let { r ->
            com.techkadak.app.notify.Notifier.show(c, "Tech Kadak v${r.version} aa gaya 🔥", "Pehle ye update karo — tap karo.", null, update = true)
            return Result.success()
        }
        // 2) Baaki apps
        val apps = runCatching { AppRepository(c).loadLocal().apps }.getOrDefault(emptyList())
        val bundled = ApkSource.loadBundled(c)
        for (a in apps) {
            val src = AppSources.sourceFor(a, bundled)
            if (src.kind != ApkSource.Kind.APK) continue
            val inst = ApkDownloads.installed(c, src.packageName) ?: continue
            val cur = ApkDownloads.get(a.slug)
            if (cur != null && cur.active) continue
            val r = runCatching { ApkResolver.resolve(src) }.getOrNull() ?: continue
            if (!ApkResolver.isUpdate(inst.first, inst.second, r)) continue
            ApkDownloads.enqueue(c, AppSources.newItem(a, src).copy(auto = true), wifiOnly = mode == AutoUpdateMode.WIFI)
        }
        return Result.success()
    }

    companion object {
        fun schedule(context: Context, mode: AutoUpdateMode) {
            val wm = WorkManager.getInstance(context)
            if (mode == AutoUpdateMode.OFF) { wm.cancelUniqueWork("tk_auto_update"); return }
            val req = PeriodicWorkRequestBuilder<AutoUpdateWorker>(12, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(if (mode == AutoUpdateMode.WIFI) NetworkType.UNMETERED else NetworkType.CONNECTED).setRequiresBatteryNotLow(true).build())
                .build()
            wm.enqueueUniquePeriodicWork("tk_auto_update", ExistingPeriodicWorkPolicy.UPDATE, req)
        }
    }
}

/** App -> source (apps.json ke fields > bundled mapping > links se andaaza). */
object AppSources {
    fun sourceFor(a: AppItem, bundled: Map<String, ApkSource>): ApkSource {
        val b = bundled[a.slug]
        val src = a.apkSource?.takeIf { ApkSource.valid(it) }
        if (src != null) return ApkSource(ApkSource.Kind.APK, src, a.packageName ?: b?.packageName, "", b?.assetHint, b?.link, b?.minSdk ?: 0, b?.assetFallback)
        return b ?: ApkSource.guess(a)
    }

    fun newItem(a: AppItem, src: ApkSource) = DlItem(
        a.slug, a.name, a.iconUrl(com.techkadak.app.Config.SITE_BASE), src.sourceLabel, src.source ?: "", src.packageName,
        listOfNotNull(src.assetHint, src.assetFallback).joinToString("\n").ifBlank { null }, DlStatus.QUEUED,
    )
}
