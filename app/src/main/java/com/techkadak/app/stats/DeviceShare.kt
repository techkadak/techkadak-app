package com.techkadak.app.stats

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.provider.Settings
import com.google.firebase.firestore.FieldValue

/**
 * v1.6 OPT-IN: the user can choose to show the owner (admin panel) this device's name and the list of installed apps.
 * Default is OFF. Nothing is read or sent until the user taps Allow (first-run card or Settings). Turning it off removes both fields again.
 */
object DeviceShare {
    private const val PREF = "tk_share"
    const val MAX_APPS = 150
    private fun sp(ctx: Context) = ctx.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun asked(ctx: Context) = sp(ctx).getBoolean("asked", false)
    fun enabled(ctx: Context) = sp(ctx).getBoolean("on", false)
    fun set(ctx: Context, on: Boolean) { sp(ctx).edit().putBoolean("on", on).putBoolean("asked", true).apply() }

    fun deviceName(ctx: Context): String {
        val cr = ctx.contentResolver
        val n = runCatching { Settings.Global.getString(cr, "device_name") }.getOrNull()
            ?: runCatching { Settings.Secure.getString(cr, "bluetooth_name") }.getOrNull()
        return DeviceRecord.clean(n?.takeIf { it.isNotBlank() } ?: DeviceRecord.modelName(), 40)
    }

    /** Names of the apps the user installed (system apps skipped, except updated ones). Sorted, capped. */
    fun installedLabels(ctx: Context): List<String> = runCatching {
        val pm = ctx.packageManager
        pm.getInstalledApplications(0)
            .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 || (it.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0 }
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .map { DeviceRecord.clean(pm.getApplicationLabel(it).toString(), 40) }
            .filter { it.isNotEmpty() }.distinct().sortedBy { it.lowercase() }.take(MAX_APPS)
    }.getOrDefault(emptyList())

    /** Extra fields merged into the devices/{id} doc. OFF => the fields are deleted. */
    fun fields(ctx: Context): Map<String, Any> =
        if (!enabled(ctx)) mapOf("sh" to false, "dn" to FieldValue.delete(), "ia" to FieldValue.delete())
        else mapOf("sh" to true, "dn" to deviceName(ctx), "ia" to installedLabels(ctx))
}
