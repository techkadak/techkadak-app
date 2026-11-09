package com.techkadak.app.firebase

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.techkadak.app.data.Prefs
import com.techkadak.app.notify.Notifier

/** FCM: "Naya app aaya" push. Data me slug ho to tap karte hi wahi app khulta hai. */
class PushService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        val prefs = Prefs(this)
        if (!prefs.pushNotify) return
        val slug = message.data["slug"]
        val title = message.notification?.title ?: message.data["title"] ?: "Naya app aaya 🔥"
        val body = message.notification?.body ?: message.data["body"] ?: "Tech Kadak me aaj ka naya app dekho"
        if (slug != null) {
            if (prefs.lastNotifiedSlug == slug) return
            prefs.lastNotifiedSlug = slug
        }
        Notifier.show(this, title, body, slug)
    }

    override fun onNewToken(token: String) {
        // Topic subscription token badalne pe bhi chalti rahe.
        TkFirebase.setPush(this, Prefs(this).pushNotify)
    }
}
