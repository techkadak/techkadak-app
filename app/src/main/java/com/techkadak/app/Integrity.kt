package com.techkadak.app

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import java.io.File
import java.security.MessageDigest

/**
 * v1.2.1 anti-tamper: modified / re-signed / fake copy pehchano.
 *
 * 1) APK ka signing certificate (SHA-256) humare release certificate se match hona chahiye.
 *    Fingerprint ek plain string me nahi hai — tukdon me + XOR karke rakha hai.
 * 2) Package name in.techkadak.app hi ho.
 * 3) Release build "debuggable" na ho.
 * Root / Frida / Xposed sirf LOG hote hain (block nahi) — bahut log khud phone root karte hain.
 * Installer source check NAHI (log GitHub/website se sideload karte hain).
 * Debug build (BuildConfig.DEBUG) me check off rehta hai, warna development me app band ho jaata.
 *
 * Imaandari: koi bhi app 100% unhackable nahi hota; ye check casual modders / re-signed copies rokta hai.
 */
object Integrity {
    private const val TAG = "TkInt"

    enum class Result { OK, BAD_SIGNATURE, BAD_PACKAGE, DEBUGGABLE }

    @Volatile private var cached: Result? = null

    private val p2 = intArrayOf(23, 91, 112, 101, 211, 43, 179, 84)
    private val k0 = intArrayOf(83, 243, 39, 102, 167, 13, 19, 211)
    private val p0 = intArrayOf(133, 246, 7, 32, 147, 73, 249, 241)
    private val k3 = intArrayOf(109, 16, 212, 145, 32, 243, 58, 162)
    private val p3 = intArrayOf(2, 70, 232, 77, 198, 159, 121, 73)
    private val k1 = intArrayOf(138, 25, 94, 150, 15, 233, 130, 55)
    private val p1 = intArrayOf(141, 185, 197, 214, 193, 123, 172, 215)
    private val k2 = intArrayOf(10, 23, 112, 108, 18, 62, 24, 142)

    private fun expected(): ByteArray {
        val ps = arrayOf(p0, p1, p2, p3); val ks = arrayOf(k0, k1, k2, k3)
        val out = ByteArray(32)
        for (i in 0 until 4) for (j in 0 until 8) out[i * 8 + j] = (ps[i][j] xor ks[i][j]).toByte()
        return out
    }

    private fun pkgName(): String = String(charArrayOf('i', 'n', '.', 't', 'e', 'c', 'h', 'k', 'a', 'd', 'a', 'k', '.', 'a', 'p', 'p'))

    fun check(ctx: Context): Result {
        cached?.let { return it }
        val r = if (BuildConfig.DEBUG) Result.OK else runCatching { doCheck(ctx) }.getOrElse {
            Log.w(TAG, "check error", it)
            // Signature padh hi nahi paaye (bahut rare). Block mat karo — galti se asli user ko na roko.
            Result.OK
        }
        cached = r
        if (r != Result.OK) Log.w(TAG, "integrity: $r")
        runCatching { Thread { runCatching { softEnvLog(ctx) } }.apply { isDaemon = true }.start() }
        return r
    }

    fun ok(ctx: Context) = check(ctx) == Result.OK

    /** Sirf unit test ke liye: obfuscated fingerprint sahi jud raha hai? */
    internal fun expectedHex(): String = expected().joinToString("") { "%02x".format(it) }

    private fun doCheck(ctx: Context): Result {
        if (ctx.packageName != pkgName()) return Result.BAD_PACKAGE
        if ((ctx.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) return Result.DEBUGGABLE
        val (certs, multi) = signingCerts(ctx)
        if (certs.isEmpty()) return Result.BAD_SIGNATURE
        val want = expected()
        val md = MessageDigest.getInstance("SHA-256")
        // Multiple signers: SAB humare hone chahiye. Ek signer (key-rotation history): humara cert history me ho
        // (humari private key ke bina koi humare cert se rotate nahi kar sakta).
        val good = if (multi) certs.all { MessageDigest.isEqual(md.digest(it), want) } else certs.any { MessageDigest.isEqual(md.digest(it), want) }
        return if (good) Result.OK else Result.BAD_SIGNATURE
    }

    @Suppress("DEPRECATION")
    private fun signingCerts(ctx: Context): Pair<List<ByteArray>, Boolean> {
        val pm = ctx.packageManager
        return if (Build.VERSION.SDK_INT >= 28) {
            val info = if (Build.VERSION.SDK_INT >= 33)
                pm.getPackageInfo(ctx.packageName, PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong()))
            else pm.getPackageInfo(ctx.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            val si = info.signingInfo ?: return emptyList<ByteArray>() to false
            val multi = si.hasMultipleSigners()
            val sigs = if (multi) si.apkContentsSigners else si.signingCertificateHistory
            sigs?.map { it.toByteArray() }.orEmpty() to multi
        } else {
            val sigs = pm.getPackageInfo(ctx.packageName, PackageManager.GET_SIGNATURES).signatures?.map { it.toByteArray() }.orEmpty()
            sigs to (sigs.size > 1)
        }
    }

    /** Sirf log (koi block nahi): root / Frida / Xposed ke nishaan. */
    private fun softEnvLog(ctx: Context) {
        val hints = mutableListOf<String>()
        if (listOf("/system/xbin/su", "/system/bin/su", "/sbin/su", "/data/adb/magisk").any { runCatching { File(it).exists() }.getOrDefault(false) }) hints += "root"
        if (runCatching { File("/proc/self/maps").readText().let { m -> m.contains("frida", true) || m.contains("xposed", true) || m.contains("lsposed", true) } }.getOrDefault(false)) hints += "hook"
        if (runCatching { Class.forName("de.robv.android.xposed.XposedBridge"); true }.getOrDefault(false)) hints += "xposed"
        if (hints.isNotEmpty()) Log.i(TAG, "env: " + hints.joinToString())
    }
}
