package com.techkadak.app.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.techkadak.app.MainActivity
import com.techkadak.app.R

object Notifier {
    const val CHANNEL = "new_apps"
    const val EXTRA_SLUG = "tk_slug"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel(CHANNEL, "Naye apps", NotificationManager.IMPORTANCE_DEFAULT)
            ch.description = "Roz ka naya free app"
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    const val EXTRA_UPDATE = "tk_update_ready"

    fun showUpdateReady(context: Context, version: String) = show(
        context, "Update ready: Tech Kadak v$version ✅", "Tap karo aur INSTALL dabao — bas ek tap.", null, update = true,
    )

    fun show(context: Context, title: String, body: String, slug: String?, update: Boolean = false) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        createChannel(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (slug != null) putExtra(EXTRA_SLUG, slug)
            if (update) putExtra(EXTRA_UPDATE, true)
        }
        val pi = PendingIntent.getActivity(context, (slug ?: if (update) "update" else "home").hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_tk)
            .setColor(0xFF9BFF2E.toInt())
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify((slug ?: "tk").hashCode(), n) }
    }
}
