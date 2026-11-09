package com.techkadak.app.firebase

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.techkadak.app.BuildConfig
import com.techkadak.app.Config
import com.techkadak.app.data.RemoteUi

/**
 * Firebase sirf tab chalta hai jab build me google-services.json tha (BuildConfig.FIREBASE_ENABLED).
 * Na ho to sab functions chup-chaap kuch nahi karte — app phir bhi poora chalta hai.
 */
object TkFirebase {
    private const val TAG = "TkFirebase"

    fun available(context: Context): Boolean =
        BuildConfig.FIREBASE_ENABLED && runCatching { FirebaseApp.getApps(context).isNotEmpty() || FirebaseApp.initializeApp(context) != null }.getOrDefault(false)

    /** "new_apps" topic: website ka bot naya app daale to sabko push notification. */
    fun setPush(context: Context, on: Boolean) {
        if (!available(context)) return
        runCatching {
            val fm = FirebaseMessaging.getInstance()
            (if (on) fm.subscribeToTopic(Config.FCM_TOPIC) else fm.unsubscribeFromTopic(Config.FCM_TOPIC))
                .addOnCompleteListener { Log.i(TAG, "topic ${Config.FCM_TOPIC} on=$on ok=${it.isSuccessful}") }
        }.onFailure { Log.w(TAG, "push setup failed", it) }
    }

    /**
     * Remote Config: pehle last activated values turant (offline bhi), phir fetch karke taaza.
     * Keys: announcement_text, announcement_url, featured_slug, home_sections_order, default_accent,
     * maintenance_message, min_version_code.
     */
    fun fetchRemoteUi(context: Context, onResult: (RemoteUi) -> Unit) {
        if (!available(context)) return
        runCatching {
            val rc = FirebaseRemoteConfig.getInstance()
            rc.setConfigSettingsAsync(FirebaseRemoteConfigSettings.Builder().setMinimumFetchIntervalInSeconds(Config.REMOTE_FETCH_EVERY_S).build())
            rc.setDefaultsAsync(RemoteUi.KEYS)
            onResult(RemoteUi.from { rc.getString(it) })
            rc.fetchAndActivate().addOnCompleteListener {
                runCatching { onResult(RemoteUi.from { k -> rc.getString(k) }) }
            }
        }.onFailure { Log.w(TAG, "remote config failed", it) }
    }
}
