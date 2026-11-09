package com.techkadak.app.member

import java.text.SimpleDateFormat
import java.util.Locale

/** v1.5: when the welcome member card shows. */
object WelcomePolicy {
    /** A NEW account (email, Google or phone) gets the card once per uid. */
    fun shouldOffer(isNewAccount: Boolean, alreadyShownForUid: Boolean): Boolean = isNewAccount && !alreadyShownForUid

    fun guestKey(installId: String) = "guest_$installId"

    sealed interface Launch {
        data class Show(val key: String, val signedIn: Boolean) : Launch
        /** Nothing to show, but remember it so the card never appears for this key later. */
        data class MarkOnly(val key: String) : Launch
        data object Skip : Launch
    }

    /**
     * First launch after updating to v1.5.0 (or first run): existing users get the card once too.
     * Signed-in users are keyed by uid, guests by install id. An existing account that logs in on an install whose
     * guest card was already shown does not get a second card. Already seen = never again.
     */
    fun onLaunch(uid: String?, installId: String, shownKeys: Set<String>): Launch {
        val key = uid ?: guestKey(installId)
        if (key in shownKeys) return Launch.Skip
        if (uid != null && guestKey(installId) in shownKeys) return Launch.MarkOnly(key)
        return Launch.Show(key, uid != null)
    }

    /** "Member since": the day this install was first counted (stats first-seen, yyyy-MM-dd) if known, else today. */
    fun memberSince(firstSeenIso: String?, todayLabel: String): String {
        val iso = firstSeenIso?.takeIf { Regex("^20\\d\\d-[01]\\d-[0-3]\\d$").matches(it) } ?: return todayLabel
        return runCatching {
            val d = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(iso)!!
            SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(d)
        }.getOrDefault(todayLabel)
    }
}
