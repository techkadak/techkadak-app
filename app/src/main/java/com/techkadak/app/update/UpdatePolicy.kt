package com.techkadak.app.update

/**
 * v1.2 update pop-up (ROHIT 3:18 AM): har app open pe, agar GitHub pe naya release hai -> pop-up
 * ("Abhi update karo" / "Baad mein"). "Baad mein" sirf is baar ke liye chhupata hai — agli baar app khole to phir aayega.
 * Net / GitHub na mile to kuch nahi (app normal khulta hai).
 * Remote Config min_version_code = emergency switch (zaroori update, cancel nahi hota) — normally 0 / off.
 */
object UpdatePolicy {
    enum class Decision { NONE, POPUP, FORCED }

    /**
     * @param latest GitHub /releases/latest (null = offline / GitHub nahi mila / koi apk nahi)
     * @param dismissedThisOpen user ne is app-open me "Baad mein" dabaya
     * @param remoteMinCode Remote Config min_version_code (0 = off)
     */
    fun decide(latest: Release?, currentName: String, currentCode: Int, dismissedThisOpen: Boolean, remoteMinCode: Long = 0): Decision {
        val newer = latest != null && Versions.isNewer(latest.version, currentName)
        return when {
            remoteMinCode > currentCode -> Decision.FORCED
            newer && !dismissedThisOpen -> Decision.POPUP
            else -> Decision.NONE
        }
    }

    /** App background me itni der raha to wapas aana = "naya open" (popup phir, update dobara check). */
    const val NEW_OPEN_AFTER_MS = 30L * 60 * 1000

    /** Release notes ko phone pe padhne layak: markdown ke #, ** hatao. */
    fun cleanNotes(notes: String): String = notes
        .lines().filterNot { it.trim().trimStart('#').trim().trimEnd(':', '✨', ' ').lowercase() in setOf("kya naya hai", "what's new", "whats new", "changelog") }
        .joinToString("\n") { it.trim().trimStart('#').trim().replace("**", "").replace(Regex("^[-*] "), "• ") }
        .replace(Regex("\n{3,}"), "\n\n").trim()
}
