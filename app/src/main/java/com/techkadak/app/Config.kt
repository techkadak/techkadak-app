package com.techkadak.app

/**
 * Saari settings ek jagah. Values gradle.properties se aati hain (TK_SITE_BASE, TK_GITHUB_OWNER, TK_GITHUB_REPO),
 * taaki GitHub username badle to sirf wahi ek file badalni pade.
 */
object Config {
    /** Website jahan data/apps.json hai — wahi file jo website ka daily bot roz update karta hai. */
    val SITE_BASE: String = BuildConfig.SITE_BASE.trimEnd('/')
    val APPS_JSON_URL: String get() = "$SITE_BASE/data/apps.json"

    /** GitHub repo jiske Releases me naya APK aata hai (in-app update). */
    val GITHUB_OWNER: String = BuildConfig.GITHUB_OWNER
    val GITHUB_REPO: String = BuildConfig.GITHUB_REPO
    val LATEST_RELEASE_API: String get() = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
    val RELEASES_PAGE: String get() = "https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"

    const val INSTAGRAM = "https://www.instagram.com/techkadak/"
    const val INSTAGRAM_DM = "https://ig.me/m/techkadak"
    const val YOUTUBE = "https://www.youtube.com/channel/UCpxdwzHB-66cPyssRH5IaTA"
    const val TELEGRAM = "https://t.me/techkadak"
    const val PINTEREST = "https://www.pinterest.com/techkadak_in/"
    /** "App request karo" yahi khulta hai. */
    const val APP_REQUEST = TELEGRAM
    /** Share app: ye link + text jaata hai. */
    val SHARE_APP_URL: String get() = SITE_BASE
    val SHARE_APP_TEXT: String get() = "Tech Kadak app 🔥 roz ek kadak FREE / open-source app, category wise, official download links ke saath.\n\nYahan se lo: $SHARE_APP_URL\nInstagram: $INSTAGRAM"

    /** Website wali hi 5 categories. */
    val CATEGORIES = listOf("Android Apps", "AI Tools", "Open Source/GitHub", "Paid ka FREE Jawab", "Student Tech")

    /** Firebase Cloud Messaging topic jispe "naya app aaya" push jaata hai. */
    const val FCM_TOPIC = "new_apps"
    val FIREBASE_ON: Boolean = BuildConfig.FIREBASE_ENABLED

    /** Update check kitni der me ek baar (launch pe), taaki GitHub API limit na lage. */
    const val UPDATE_CHECK_EVERY_MS = 60L * 60 * 1000

    /** Remote Config kitni der me ek baar taaza (seconds). */
    const val REMOTE_FETCH_EVERY_S = 30L * 60
}
