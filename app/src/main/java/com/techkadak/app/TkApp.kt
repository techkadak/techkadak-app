package com.techkadak.app

import android.app.Application
import com.techkadak.app.data.Prefs
import com.techkadak.app.firebase.TkFirebase
import com.techkadak.app.notify.DailyWorker
import com.techkadak.app.notify.Notifier
import kotlinx.coroutines.launch

class TkApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // v1.2.1: sabse pehle crash recorder (agli launch pe "Report copy karo" pop-up + safe mode)
        runCatching { CrashLog.install(this) }
        // Modified / re-signed copy: koi background kaam nahi (MainActivity block screen dikhata hai)
        if (!Integrity.ok(this)) return
        runCatching { Notifier.createChannel(this) }
        runCatching { DailyWorker.schedule(this) }
        runCatching { TkFirebase.setPush(this, Prefs(this).pushNotify) }
        runCatching { com.techkadak.app.install.ApkDownloads.init(this) }
        runCatching { com.techkadak.app.install.ApkDownloads.channel(this) }
        // v1.2: anonymous usage stats (owner dashboard). Foreground = app_open, background = batch flush.
        // Safe mode (pichli launch crash hui thi) me stats us launch ke liye band.
        if (!CrashLog.safeMode) runCatching { com.techkadak.app.stats.Stats.init(this) }
        if (!CrashLog.safeMode) runCatching {
            androidx.lifecycle.ProcessLifecycleOwner.get().lifecycle.addObserver(object : androidx.lifecycle.DefaultLifecycleObserver {
                override fun onStart(owner: androidx.lifecycle.LifecycleOwner) {
                    runCatching { com.techkadak.app.stats.Stats.appOpen(this@TkApp, runCatching { com.techkadak.app.auth.AuthManager(this@TkApp).current()?.provider }.getOrNull()) }
                }
                override fun onStop(owner: androidx.lifecycle.LifecycleOwner) { runCatching { com.techkadak.app.stats.Stats.flush(this@TkApp) } }
            })
        }
        kotlinx.coroutines.MainScope().launch(kotlinx.coroutines.Dispatchers.IO) {
            runCatching { com.techkadak.app.install.AutoUpdateWorker.schedule(this@TkApp, com.techkadak.app.data.SettingsStore(this@TkApp).current().autoUpdate) }
        }
    }
}
