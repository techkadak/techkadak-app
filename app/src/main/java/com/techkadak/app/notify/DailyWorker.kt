package com.techkadak.app.notify

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.techkadak.app.data.AppRepository
import com.techkadak.app.data.Prefs
import java.util.concurrent.TimeUnit

/** Har ~6 ghante website check: bot ne naya app daala ho to local notification "Aaj ka naya app: X". */
class DailyWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val prefs = Prefs(applicationContext)
        val res = AppRepository(applicationContext).fetchRemote() ?: return Result.retry()
        runCatching { com.techkadak.app.widget.TodayWidget.refreshAll(applicationContext) }
        val newest = res.apps.firstOrNull() ?: return Result.success()
        val last = prefs.lastNotifiedSlug
        if (last == null) { prefs.lastNotifiedSlug = newest.slug; return Result.success() }
        if (last != newest.slug) {
            // v1.6: apps are sorted newest first; everything before the last notified one is new (max 5 shown by name).
            val idx = res.apps.indexOfFirst { it.slug == last }
            val fresh = (if (idx > 0) res.apps.take(idx) else listOf(newest))
            prefs.lastNotifiedSlug = newest.slug
            if (prefs.dailyNotify) {
                if (fresh.size == 1) Notifier.show(applicationContext, "Naya app aaya: ${newest.name} \uD83D\uDD25", newest.what, newest.slug)
                else Notifier.show(applicationContext, "${fresh.size} naye apps aaye \uD83D\uDD25",
                    fresh.take(5).joinToString(", ") { it.name } + (if (fresh.size > 5) " aur ${fresh.size - 5} aur" else ""), newest.slug)
            }
        }
        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            val req = PeriodicWorkRequestBuilder<DailyWorker>(6, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork("tk_daily", ExistingPeriodicWorkPolicy.KEEP, req)
        }
    }
}
