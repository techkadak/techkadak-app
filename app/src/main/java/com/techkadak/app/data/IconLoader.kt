package com.techkadak.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/** Chhota sa icon loader (memory + disk cache) — Coil/Glide ke bina, APK chhota rakhne ke liye. */
object IconLoader {
    private val mem = LruCache<String, Bitmap>(150)
    private val gate = Semaphore(4)
    private val failed = HashSet<String>()

    fun peek(url: String): Bitmap? = mem.get(url)
    fun put(url: String, bmp: Bitmap) { mem.put(url, bmp) }
    fun clearMemory() { mem.evictAll(); synchronized(failed) { failed.clear() } }

    suspend fun load(context: Context, url: String, target: Int = 192): Bitmap? {
        val key = if (target == 192) url else "$url#$target"
        mem.get(key)?.let { return it }
        if (synchronized(failed) { url in failed }) return null
        return withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "icons").apply { mkdirs() }
            val file = File(dir, sha1(url))
            val bmp = runCatching {
                if (file.exists()) decode(file.readBytes(), target)
                else gate.withPermit {
                    val bytes = Http.getBytes(url)
                    decode(bytes, target)?.also { file.writeBytes(bytes) }
                }
            }.getOrNull()
            if (bmp != null) mem.put(key, bmp) else synchronized(failed) { failed += url }
            bmp
        }
    }

    fun decode(bytes: ByteArray, target: Int = 192): Bitmap? {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, o)
        var s = 1
        while (o.outWidth / (s * 2) >= target && o.outHeight / (s * 2) >= target) s *= 2
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = s })
    }

    private fun sha1(s: String) = MessageDigest.getInstance("SHA-1").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }
}
