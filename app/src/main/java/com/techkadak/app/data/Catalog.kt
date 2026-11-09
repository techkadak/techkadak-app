package com.techkadak.app.data

import java.net.URI

/** Home page ki rows aur chips — sab apps.json se apne aap banta hai (naya category aaye to wo bhi dikh jaye). */
object Catalog {
    /** Category ka chhota Hinglish tagline. */
    fun tagline(cat: String): String = when (cat) {
        "Android Apps" -> "Phone ke liye kadak apps"
        "AI Tools" -> "AI se kaam fatafat"
        "Open Source/GitHub" -> "Code khula, data tera"
        "Paid ka FREE Jawab" -> "Paise bachao, same kaam"
        "Student Tech" -> "Padhai ke liye best"
        else -> "Kaam ke apps"
    }

    fun shortName(cat: String): String = when (cat) {
        "Open Source/GitHub" -> "Open Source"
        "Paid ka FREE Jawab" -> "Paid ka FREE Jawab"
        else -> cat
    }

    /** Categories: pehle jaani-pehchani order me, phir koi nayi ho to count ke hisaab se. */
    fun categories(apps: List<AppItem>, known: List<String>): List<Pair<String, Int>> {
        val counts = LinkedHashMap<String, Int>()
        apps.forEach { a -> a.categories.forEach { counts[it] = (counts[it] ?: 0) + 1 } }
        val first = known.filter { counts.containsKey(it) }
        val rest = counts.keys.filter { it !in known }.sortedByDescending { counts[it] }
        return (first + rest).map { it to (counts[it] ?: 0) }
    }

    fun inCategory(apps: List<AppItem>, cat: String) = apps.filter { cat in it.categories }

    /** "Aaj ka App": Remote Config ka featured_slug, warna sabse naya app. */
    fun featured(apps: List<AppItem>, featuredSlug: String?): AppItem? =
        featuredSlug?.let { s -> apps.firstOrNull { it.slug == s } } ?: apps.firstOrNull()

    /** Top picks: jinpe Instagram post bana + jo kisi paid app ki jagah lete hain; phir baaki. */
    fun topPicks(apps: List<AppItem>, limit: Int = 12): List<AppItem> =
        apps.sortedByDescending { score(it) }.take(limit)

    private fun score(a: AppItem): Int =
        (if (a.posts.isNotEmpty()) 4 else 0) + (if (a.replaces != null) 2 else 0) +
            (if (a.pricing.equals("Free", true)) 1 else 0) + (if (a.icon != null) 1 else 0) + a.categories.size

    /** Developer / source line jaise Play me hota hai: GitHub owner ya website ka domain. */
    fun developer(a: AppItem): String {
        a.links.firstOrNull { it.url.contains("github.com/") }?.let { l ->
            val parts = l.url.substringAfter("github.com/").split('/').filter { it.isNotBlank() }
            if (parts.isNotEmpty()) return "github.com/" + parts[0]
        }
        val l = a.links.firstOrNull { it.label.equals("Website", true) } ?: a.links.firstOrNull() ?: return "Tech Kadak"
        return runCatching { URI(l.url).host.removePrefix("www.") }.getOrNull() ?: "Tech Kadak"
    }

    fun isOpenSource(a: AppItem): Boolean =
        a.categories.any { it.contains("Open Source", true) } ||
            a.links.any { it.label.equals("F-Droid", true) || it.label.equals("GitHub", true) } ||
            (a.priceNote?.contains("open source", true) == true)

    /** Naye apps din ke hisaab se group (Updates tab). */
    fun byDay(apps: List<AppItem>): List<Pair<String, List<AppItem>>> =
        apps.groupBy { it.added.take(10) }.toList().sortedByDescending { it.first }

    fun suggestions(apps: List<AppItem>, q: String, limit: Int = 6): List<AppItem> {
        val t = q.trim().lowercase(); if (t.isEmpty()) return emptyList()
        val starts = apps.filter { it.name.lowercase().startsWith(t) }
        val contains = apps.filter { it !in starts && it.matches(t) }
        return (starts + contains).take(limit)
    }

    /** Download button ka text: link kis jagah khulega. */
    fun downloadLabel(l: AppLink): String = when (l.label.lowercase().trim()) {
        "f-droid" -> "F-Droid se"
        "github", "github releases" -> "GitHub se"
        "play store", "google play" -> "Play Store se"
        "download" -> "Official site se"
        "website", "web app" -> "Website pe"
        else -> l.label
    }

    /** Sort: Naya pehle (website ka order) ya A-Z. */
    fun sort(apps: List<AppItem>, mode: SortMode): List<AppItem> = when (mode) {
        SortMode.NEW -> apps.sortedByDescending { it.added.take(10) }
        SortMode.AZ -> apps.sortedBy { it.name.lowercase() }
    }

    /** Filter: category (null = sab) + price/source type. */
    fun filter(apps: List<AppItem>, category: String?, price: PriceFilter): List<AppItem> =
        apps.filter { a -> (category == null || category in a.categories) && price.accepts(a) }

    /** Slugs ki list ko apps me badlo (order wahi), jo app list se hat gaye unhe chhod do. */
    fun bySlugs(apps: List<AppItem>, slugs: List<String>): List<AppItem> {
        val m = apps.associateBy { it.slug }
        return slugs.mapNotNull { m[it] }
    }
}

enum class PriceFilter(val hi: String, val en: String) {
    ALL("Sab", "All"), FREE("Free", "Free"), FREEMIUM("Freemium", "Freemium"), OPEN_SOURCE("Open-source", "Open-source");

    fun accepts(a: AppItem): Boolean = when (this) {
        ALL -> true
        FREE -> a.pricing.equals("Free", true)
        FREEMIUM -> a.pricing.equals("Freemium", true)
        OPEN_SOURCE -> Catalog.isOpenSource(a)
    }
}
