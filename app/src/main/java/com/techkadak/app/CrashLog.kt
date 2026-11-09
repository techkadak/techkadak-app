package com.techkadak.app

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Process
import android.util.Log
import android.widget.Toast
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * v1.2.1: Crash ka record. App kabhi crash ho to stacktrace filesDir/last_crash.txt me save hota hai,
 * agli baar app khulte hi ek chhota pop-up dikhata hai ("Report copy karo" -> clipboard -> WhatsApp pe bhej do).
 *
 * Safe mode: agar app khulne ke 15 sec ke andar crash hua (launch crash), to agli launch "safe mode" me hoti hai:
 * optional cheezein (stats, remote config, videos/kits network, Liquid Glass wallpaper) us launch me band.
 */
object CrashLog {
    private const val TAG = "TkCrash"
    private const val FILE = "last_crash.txt"
    private const val SP = "tk_crash"
    @Volatile private var startedAt = 0L
    @Volatile var safeMode = false
        private set

    fun install(ctx: Context) {
        startedAt = System.currentTimeMillis()
        val app = ctx.applicationContext
        try {
            val sp = app.getSharedPreferences(SP, Context.MODE_PRIVATE)
            // Pichli launch jaldi crash hui thi? to is baar safe mode.
            val launchCrashes = sp.getInt("launch_crashes", 0)
            safeMode = launchCrashes > 0
            // Is launch ko "abhi chal rahi" mark karo; 15 sec tak bina crash = counter reset (Handler se, MainActivity/TkApp).
        } catch (_: Throwable) {}
        val prev = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try { save(app, t, e) } catch (_: Throwable) {}
            when {
                prev != null -> prev.uncaughtException(t, e) // Android ka apna handler: "keeps stopping" + process band
                isAndroidRuntime() -> { Process.killProcess(Process.myPid()); System.exit(10) }
                else -> Log.e(TAG, "uncaught (no default handler)", e) // JVM unit tests: process mat maaro
            }
        }
        try {
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ markLaunchOk(app) }, 15_000)
        } catch (_: Throwable) {}
    }

    private fun isAndroidRuntime(): Boolean =
        (System.getProperty("java.vm.name") ?: "").let { it.contains("Dalvik", true) || it.contains("ART", false) }

    private fun markLaunchOk(ctx: Context) {
        try { ctx.getSharedPreferences(SP, Context.MODE_PRIVATE).edit().putInt("launch_crashes", 0).apply() } catch (_: Throwable) {}
    }

    fun save(ctx: Context, t: Thread?, e: Throwable) {
        val sw = StringWriter()
        e.printStackTrace(PrintWriter(sw))
        val early = System.currentTimeMillis() - startedAt < 15_000
        val head = buildString {
            append("Tech Kadak crash report\n")
            append("Time: ").append(SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US).format(Date())).append('\n')
            append("App: ").append(BuildConfig.VERSION_NAME).append(" (").append(BuildConfig.VERSION_CODE).append(")\n")
            append("Phone: ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL).append(" | Android ").append(Build.VERSION.RELEASE).append(" (SDK ").append(Build.VERSION.SDK_INT).append(")\n")
            append("Thread: ").append(t?.name ?: "?").append(if (early) " | launch crash" else "").append("\n\n")
        }
        val text = (head + sw.toString()).take(24_000)
        File(ctx.filesDir, FILE).writeText(text)
        val sp = ctx.getSharedPreferences(SP, Context.MODE_PRIVATE)
        sp.edit().putInt("launch_crashes", if (early) sp.getInt("launch_crashes", 0) + 1 else 0).commit()
        Log.e(TAG, text)
    }

    fun pending(ctx: Context): String? = try {
        File(ctx.filesDir, FILE).takeIf { it.exists() }?.readText()?.takeIf { it.isNotBlank() }
    } catch (_: Throwable) { null }

    fun clear(ctx: Context) { try { File(ctx.filesDir, FILE).delete() } catch (_: Throwable) {} }

    /** Pichla crash ho to plain Android dialog (Compose se alag, taaki Compose me dikkat ho tab bhi dikhe). */
    fun showIfAny(a: Activity) {
        val text = pending(a) ?: return
        try {
            AlertDialog.Builder(a)
                .setTitle("Pichli baar app band ho gaya tha 😔")
                .setMessage("Maafi! Neeche wala report copy karke Tech Kadak ko WhatsApp pe bhej do, hum turant theek karenge.\n\n" + text.take(1500))
                .setPositiveButton("Report copy karo") { _, _ ->
                    try {
                        val cm = a.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("Tech Kadak crash report", text))
                        Toast.makeText(a, "Report copy ho gaya ✅", Toast.LENGTH_SHORT).show()
                    } catch (_: Throwable) {}
                    clear(a)
                }
                .setNegativeButton("Theek hai") { _, _ -> clear(a) }
                .setOnCancelListener { clear(a) }
                .show()
        } catch (e: Throwable) { Log.w(TAG, "dialog", e); clear(a) }
    }
}
