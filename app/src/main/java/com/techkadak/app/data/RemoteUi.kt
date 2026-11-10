package com.techkadak.app.data

/**
 * Firebase Remote Config se aane wala "remote control" — bina naya APK daale app badlo.
 * Har key ka matlab README-HINDI.md me hai.
 */
data class RemoteUi(
    val announcementText: String = "",
    val announcementUrl: String? = null,
    val featuredSlug: String? = null,
    val homeSectionsOrder: String? = null,
    val defaultAccent: Long? = null,
    val maintenanceMessage: String? = null,
    val minVersionCode: Long = 0,
    /** v1.2: Kits ko bina naye APK ke badlo — kits.json jaisa JSON string (khaali = bundled / website wale). */
    val kitsJson: String? = null,
    /** v1.8: Remote Config latest_version_code + apk_url. Naya ho to "update available" card. */
    val latestVersionCode: Long = 0,
    val apkUrl: String? = null,
) {
    val hasAnnouncement get() = announcementText.isNotBlank()

    fun needsForceUpdate(currentVersionCode: Int): Boolean = minVersionCode > currentVersionCode

    companion object {
        val KEYS = mapOf(
            "announcement_text" to "", "announcement_url" to "", "featured_slug" to "",
            "home_sections_order" to "", "default_accent" to "", "maintenance_message" to "", "min_version_code" to "0", "kits_json" to "", "latest_version_code" to "0", "apk_url" to "",
        )

        /** Raw strings (Remote Config getString) se safe values. Galat value = ignore. */
        fun from(get: (String) -> String): RemoteUi = RemoteUi(
            announcementText = get("announcement_text").trim(),
            announcementUrl = get("announcement_url").trim().takeIf { it.startsWith("https://") },
            featuredSlug = get("featured_slug").trim().ifEmpty { null },
            homeSectionsOrder = get("home_sections_order").trim().ifEmpty { null },
            defaultAccent = parseColor(get("default_accent")),
            maintenanceMessage = get("maintenance_message").trim().ifEmpty { null },
            minVersionCode = get("min_version_code").trim().toDoubleOrNull()?.toLong()?.coerceAtLeast(0) ?: 0,
            kitsJson = get("kits_json").trim().takeIf { it.startsWith("{") },
            latestVersionCode = get("latest_version_code").trim().toDoubleOrNull()?.toLong()?.coerceAtLeast(0) ?: 0,
            apkUrl = get("apk_url").trim().takeIf { it.startsWith("https://") },
        )

        /** "#9BFF2E" / "9bff2e" / "#FF9BFF2E" -> ARGB long. Kuch aur = null. */
        fun parseColor(raw: String?): Long? {
            val h = raw?.trim()?.removePrefix("#") ?: return null
            if (!h.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) return null
            return when (h.length) {
                6 -> 0xFF000000L or h.toLong(16)
                8 -> h.toLong(16)
                else -> null
            }
        }
    }
}
