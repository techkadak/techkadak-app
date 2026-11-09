package com.techkadak.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class ThemeMode(val hi: String, val en: String) { AMOLED("AMOLED Black", "AMOLED Black"), DARK("Dark", "Dark"), LIGHT("Light", "Light"), GLASS("Liquid Glass ✨", "Liquid Glass ✨"),
    GLOSSY_DARK("Glossy Dark", "Glossy Dark"), IOS_LIGHT("iOS Light", "iOS Light"), IOS_DARK("iOS Dark", "iOS Dark"), MATTE_BLACK("Matte Black", "Matte Black"), MATTE_WHITE("Matte White", "Matte White"), GLOSSY_PURPLE("Glossy Purple", "Glossy Purple"), LIQUID_GLASS("Liquid Glass", "Liquid Glass"), NEON_GLOW("Neon Glow", "Neon Glow"), AURORA_GRADIENT("Aurora Gradient", "Aurora Gradient"), SUNSET_GRADIENT("Sunset Gradient", "Sunset Gradient"), OCEAN_BLUE("Ocean Blue", "Ocean Blue"), FOREST_GREEN("Forest Green", "Forest Green"), ROYAL_GOLD("Royal Gold", "Royal Gold"), ROSE_PASTEL("Rose Pastel", "Rose Pastel"), MINT_FRESH("Mint Fresh", "Mint Fresh"), PEACH_SOFT("Peach Soft", "Peach Soft"), CYBERPUNK("Cyberpunk", "Cyberpunk"), RETRO_TERMINAL("Retro Terminal", "Retro Terminal"), PAPER_INK("Paper Ink", "Paper Ink"), EARTHY_NATURE("Earthy Nature", "Earthy Nature"), CARBON_FIBER("Carbon Fiber", "Carbon Fiber"), MIDNIGHT_BLUE("Midnight Blue", "Midnight Blue"), CHERRY_RED("Cherry Red", "Cherry Red"), LAVENDER_DREAM("Lavender Dream", "Lavender Dream"), MONOCHROME("Monochrome", "Monochrome"), HIGH_CONTRAST("High Contrast", "High Contrast"), SAKURA_PINK("Sakura Pink", "Sakura Pink"), ARCTIC_FROST("Arctic Frost", "Arctic Frost"), COFFEE_BROWN("Coffee Brown", "Coffee Brown"), EMERALD_LUXE("Emerald Luxe", "Emerald Luxe");

    /** Light background wali looks: text/ink dark side se, Palette.isLight yahin se padhta hai. */
    val isLightLook: Boolean get() = this == LIGHT || this in LIGHT_LOOKS
    companion object {
        private val LIGHT_LOOKS = setOf(IOS_LIGHT, MATTE_WHITE, ROSE_PASTEL, MINT_FRESH, PEACH_SOFT, PAPER_INK, EARTHY_NATURE, LAVENDER_DREAM, SAKURA_PINK, ARCTIC_FROST)
    }
}
enum class CardStyle(val hi: String, val en: String) { GRID("Grid", "Grid"), LIST("List", "List"), COMPACT("Compact", "Compact") }
enum class SortMode(val hi: String, val en: String) { NEW("Naya pehle", "Newest"), AZ("A-Z", "A-Z") }

/** Home page ke sections — order + on/off Settings me, default order Remote Config se bhi aa sakta hai. */
enum class HomeSection(val id: String, val hi: String, val en: String) {
    FEATURED("featured", "Aaj ka App (banner)", "App of the day (banner)"),
    NEW("new", "Naye Apps", "New apps"),
    KITS("kits", "Kits 🎒", "Kits 🎒"),
    VIDEOS("videos", "Tech Kadak Videos ▶️", "Tech Kadak Videos ▶️"),
    FAVOURITES("favourites", "Meri Pasand ❤️", "My favourites ❤️"),
    RECENT("recent", "Haal me dekhe", "Recently viewed"),
    TOP("top", "Top picks", "Top picks"),
    CATEGORIES("categories", "Category rows", "Category rows"),
    CHART("chart", "Sabse zyada kaam ke", "Most useful chart");

    companion object {
        val DEFAULT = listOf(FEATURED, NEW, KITS, VIDEOS, FAVOURITES, RECENT, TOP, CATEGORIES, CHART)
        fun byId(id: String) = entries.firstOrNull { it.id == id.trim().lowercase() }

        /** "featured,new,top" -> order; jo naam nahi aaye wo baad me apne default order me. */
        fun parseOrder(csv: String?): List<HomeSection> {
            val given = csv.orEmpty().split(',').mapNotNull { byId(it) }.distinct()
            return given + DEFAULT.filter { it !in given }
        }
    }
}

data class SectionPref(val section: HomeSection, val visible: Boolean)

enum class AutoUpdateMode { OFF, WIFI, ALWAYS }

data class UserSettings(
    val theme: ThemeMode = ThemeMode.AMOLED,
    /** null = default (Remote Config ka default_accent, warna lime). ARGB. */
    val accent: Long? = null,
    val fontScale: Float = 1f,
    val cardStyle: CardStyle = CardStyle.GRID,
    /** null = user ne kabhi order nahi badla -> Remote Config / default order. */
    val sections: List<SectionPref>? = null,
    val english: Boolean = false,
    val inAppBrowser: Boolean = true,
    val onboarded: Boolean = false,
    /** Login screen ek baar dikh gaya (login kiya ya skip). */
    val loginSeen: Boolean = false,
    val sort: SortMode = SortMode.NEW,
    /** Apps auto-update: OFF / WIFI (sirf Wi-Fi pe) / ALWAYS */
    val autoUpdate: AutoUpdateMode = AutoUpdateMode.WIFI,
    /** v1.3: pull-to-refresh with the mascot hand (off = plain Material pull-to-refresh). */
    val refreshAnim: Boolean = true,
    /** v1.5: logo intro when the app opens (tap = skip). Off = straight to the app. */
    val openingAnim: Boolean = true,
) {
    fun sectionPrefs(remoteOrder: String?): List<SectionPref> =
        sections ?: HomeSection.parseOrder(remoteOrder).map { SectionPref(it, true) }

    companion object {
        fun encodeSections(list: List<SectionPref>): String = list.joinToString(",") { it.section.id + ":" + (if (it.visible) 1 else 0) }
        fun decodeSections(s: String?): List<SectionPref>? {
            if (s.isNullOrBlank()) return null
            val parsed = s.split(',').mapNotNull { part ->
                val (id, v) = (part.split(':') + "1").take(2)
                HomeSection.byId(id)?.let { SectionPref(it, v != "0") }
            }.distinctBy { it.section }
            return parsed + HomeSection.DEFAULT.filter { d -> parsed.none { it.section == d } }.map { SectionPref(it, true) }
        }
    }
}

private val Context.tkStore: DataStore<Preferences> by preferencesDataStore(name = "tk_settings")

/** DataStore: settings + favourites + recently viewed. */
class SettingsStore(context: Context) {
    private val ds = context.applicationContext.tkStore

    private object K {
        val theme = stringPreferencesKey("theme")
        val accent = longPreferencesKey("accent")
        val font = floatPreferencesKey("font_scale")
        val card = stringPreferencesKey("card_style")
        val sections = stringPreferencesKey("home_sections")
        val english = booleanPreferencesKey("english")
        val inApp = booleanPreferencesKey("in_app_browser")
        val onboarded = booleanPreferencesKey("onboarded")
        val loginSeen = booleanPreferencesKey("login_seen")
        val sort = stringPreferencesKey("sort")
        val autoUpdate = stringPreferencesKey("auto_update")
        val refreshAnim = booleanPreferencesKey("refresh_anim")
        val openingAnim = booleanPreferencesKey("opening_anim")
        val favs = stringPreferencesKey("favourites")
        val recent = stringPreferencesKey("recent_viewed")
    }

    val settings: Flow<UserSettings> = ds.data.map { p ->
        UserSettings(
            theme = p[K.theme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.AMOLED,
            accent = p[K.accent],
            fontScale = (p[K.font] ?: 1f).coerceIn(0.8f, 1.4f),
            cardStyle = p[K.card]?.let { runCatching { CardStyle.valueOf(it) }.getOrNull() } ?: CardStyle.GRID,
            sections = UserSettings.decodeSections(p[K.sections]),
            english = p[K.english] ?: false,
            inAppBrowser = p[K.inApp] ?: true,
            onboarded = p[K.onboarded] ?: false,
            loginSeen = p[K.loginSeen] ?: false,
            sort = p[K.sort]?.let { runCatching { SortMode.valueOf(it) }.getOrNull() } ?: SortMode.NEW,
            autoUpdate = p[K.autoUpdate]?.let { runCatching { AutoUpdateMode.valueOf(it) }.getOrNull() } ?: AutoUpdateMode.WIFI,
            refreshAnim = p[K.refreshAnim] ?: true,
            openingAnim = p[K.openingAnim] ?: true,
        )
    }

    /** Favourites: naya pehle. */
    val favourites: Flow<List<String>> = ds.data.map { split(it[K.favs]) }
    val recent: Flow<List<String>> = ds.data.map { split(it[K.recent]) }

    suspend fun setAutoUpdate(v: AutoUpdateMode) { ds.edit { it[K.autoUpdate] = v.name } }

    suspend fun current(): UserSettings = settings.first()
    suspend fun favouritesNow(): List<String> = favourites.first()

    suspend fun setTheme(v: ThemeMode) = ds.edit { it[K.theme] = v.name }
    suspend fun setAccent(v: Long?) = ds.edit { if (v == null) it.remove(K.accent) else it[K.accent] = v }
    suspend fun setFontScale(v: Float) = ds.edit { it[K.font] = v.coerceIn(0.8f, 1.4f) }
    suspend fun setCardStyle(v: CardStyle) = ds.edit { it[K.card] = v.name }
    suspend fun setSections(v: List<SectionPref>?) = ds.edit { if (v == null) it.remove(K.sections) else it[K.sections] = UserSettings.encodeSections(v) }
    suspend fun setEnglish(v: Boolean) = ds.edit { it[K.english] = v }
    suspend fun setInAppBrowser(v: Boolean) = ds.edit { it[K.inApp] = v }
    suspend fun setRefreshAnim(v: Boolean) = ds.edit { it[K.refreshAnim] = v }
    suspend fun setOpeningAnim(v: Boolean) = ds.edit { it[K.openingAnim] = v }
    suspend fun setOnboarded(v: Boolean) = ds.edit { it[K.onboarded] = v }
    suspend fun setLoginSeen(v: Boolean) = ds.edit { it[K.loginSeen] = v }
    suspend fun setSort(v: SortMode) = ds.edit { it[K.sort] = v.name }

    suspend fun toggleFavourite(slug: String) = ds.edit {
        val cur = split(it[K.favs])
        it[K.favs] = join(if (slug in cur) cur - slug else listOf(slug) + cur)
    }

    suspend fun addRecent(slug: String) = ds.edit {
        it[K.recent] = join((listOf(slug) + split(it[K.recent]).filter { s -> s != slug }).take(MAX_RECENT))
    }

    suspend fun clearRecent() = ds.edit { it.remove(K.recent) }

    /** Saari customise settings default pe (favourites nahi hatate). */
    suspend fun resetLook() = ds.edit {
        listOf(K.theme, K.accent, K.font, K.card, K.sections, K.english, K.sort).forEach { k -> it.remove(k) }
    }

    companion object {
        const val MAX_RECENT = 20
        fun split(s: String?): List<String> = s.orEmpty().split('\n').filter { it.isNotBlank() }
        fun join(l: List<String>): String = l.joinToString("\n")
    }
}
