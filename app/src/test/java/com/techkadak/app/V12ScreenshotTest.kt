package com.techkadak.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.techkadak.app.auth.TkUser
import com.techkadak.app.data.*
import com.techkadak.app.install.ApkSource
import com.techkadak.app.stats.DashData
import com.techkadak.app.stats.DayPoint
import com.techkadak.app.stats.OwnerStats
import com.techkadak.app.ui.*
import androidx.compose.material.icons.rounded.AutoAwesome
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config as RConfig
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** v1.2 screenshots: Kits, Poster, Videos, Admin / Owner dashboard, Liquid Glass. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@RConfig(sdk = [34], qualifiers = "w393dp-h852dp-xxhdpi", application = android.app.Application::class)
class V12ScreenshotTest {
    @get:Rule val rule = createComposeRule()
    private lateinit var apps: List<AppItem>
    private lateinit var kits: List<Kit>
    private lateinit var videos: List<TkVideo>
    private lateinit var sources: Map<String, ApkSource>
    private val out = System.getProperty("tk.shotsDir") ?: "build/screens"
    private val isNew: (AppItem) -> Boolean = { false }

    @Before fun setUp() {
        apps = AppsParser.parse(File("src/main/assets/apps.json").readText())
        kits = Kits.parse(File("src/main/assets/kits.json").readText(), apps)
        videos = Videos.parseFeed(File("src/main/assets/videos.xml").readText())
        sources = org.json.JSONObject(File("src/main/assets/apk_sources.json").readText()).getJSONObject("apps").let { o ->
            o.keys().asSequence().mapNotNull { k -> ApkSource.fromJson(o.getJSONObject(k))?.let { k to it } }.toMap()
        }
        val dir = File(System.getProperty("tk.iconsDir") ?: "build/test-icons")
        apps.forEach { a ->
            val f = File(dir, a.slug + ".png")
            val url = a.iconUrl(Config.SITE_BASE) ?: return@forEach
            if (f.exists()) BitmapFactory.decodeFile(f.path)?.let { IconLoader.put(url, it) }
        }
        // YouTube thumbnails (test me pehle se download kiye hue, agar hain)
        videos.forEach { v ->
            val f = File(dir, "yt-" + v.id + ".jpg")
            if (f.exists()) BitmapFactory.decodeFile(f.path)?.let { IconLoader.put(v.thumb + "#480", it) }
        }
    }

    private fun src(a: AppItem) = com.techkadak.app.install.AppSources.sourceFor(a, sources)

    private fun shot(name: String, tab: TabId?, settings: UserSettings = UserSettings(onboarded = true, loginSeen = true), content: @Composable () -> Unit) {
        rule.setContent {
            TkTheme(settings) {
                val body = @Composable {
                    Scaffold(containerColor = Tk.Black, bottomBar = { if (tab != null) BottomBar(tab, 2) {} }) { p ->
                        Box(Modifier.fillMaxSize().padding(p)) { content() }
                    }
                }
                if (Tk.isGlass) GlassBackdrop(animate = false) { body() } else body()
            }
        }
        rule.mainClock.advanceTimeBy(1500)
        rule.waitForIdle()
        captureScreenRoboImage("$out/$name.png")
    }

    private val data get() = UiData(apps = apps, loading = false, source = DataSource.LIVE)
    private val sectionsKitsFirst = listOf(HomeSection.KITS, HomeSection.VIDEOS, HomeSection.FEATURED, HomeSection.NEW).map { SectionPref(it, true) }

    @Test fun v01_home_kits_videos() = shot("30-home-kits-videos", TabId.Home) {
        HomeScreen(data, RemoteUi(), sectionsKitsFirst, emptyList(), emptyList(), isNew, {}, {}, {}, {}, {}, kits = kits, videos = videos)
    }

    @Test fun v02_kits_list() = shot("31-kits-list", null) { KitsListScreen(kits, apps, {}, {}) }

    @Test fun v03_kit_page() = shot("32-kit-student", null) {
        val k = kits.first { it.id == "student" }
        KitScreen(k, apps, { src(it) }, { it.slug == "markor" }, isNew, { 4 }, {}, {}, {}, {}, {})
    }

    @Test fun v03b_kit_creator() = shot("32b-kit-creator", null) {
        KitScreen(kits.first { it.id == "creator" }, apps, { src(it) }, { false }, isNew, { 4 }, {}, {}, {}, {}, {})
    }

    @Test fun v04_videos() = shot("33-videos", null) { VideosScreen(videos, {}, {}, {}) }

    @Test fun v05_detail_video_poster() = shot("34-detail-video-poster", null) {
        val a = apps.first { it.slug == "image-toolbox" }
        Column(Modifier.fillMaxSize()) {
            // detail page ka neeche wala hissa (video + poster button) dikhane ke liye seedha woh blocks
            ScreenTitle(a.name, "Detail page — naye v1.2 blocks", {})
            DetailVideos(a, videos) {}
            Box(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) { LimeButton("Poster share karo", Modifier.fillMaxWidth(), androidx.compose.material.icons.Icons.Rounded.AutoAwesome) {} }
        }
    }

    @Test fun v06_poster_dialog() = shot("35-poster-dialog", null) {
        PosterDialog(apps.first { it.slug == "image-toolbox" }, { _, _ -> }, {})
    }

    @Test fun v07_admin_login() = shot("36-admin-login", null) {
        AdminScreen(null, false, null, null, 0, null, false, null, apps, AdminActions(), {})
    }

    @Test fun v07b_admin_denied() = shot("36b-admin-denied", null) {
        AdminScreen(TkUser("u1", "Kisi aur ka", "someone@gmail.com", null, true, "google"), false, null, null, 0, null, false, null, apps, AdminActions(), {})
    }

    @Test fun v07c_admin_locked() = shot("36c-admin-locked", null) {
        AdminScreen(null, false, "Gmail ya password galat hai", null, 263, null, false, null, apps, AdminActions(), {})
    }

    private val owner = TkUser("o1", "Rohit Saini", OwnerStats.OWNER_EMAIL, null, true, "password")

    @Test fun v08_dashboard_today() = shot("37-dashboard-empty", null) {
        // Jaisa abhi (v1.2 release ke din) dikhega: sab 0 se shuru
        val days = OwnerStats.lastDays(30).map { DayPoint(it, 0, 0) }
        AdminScreen(owner, false, null, null, 0,
            DashData(days.last().date, 0, 0, days, 0, 0, 0, 0, emptyList(), emptyList(), emptyList(), emptyList(), 0, emptyList(), emptyMap(), emptyList()),
            false, null, apps, AdminActions(), {})
    }

    @Test fun v08b_dashboard_demo() = shot("37b-dashboard-DEMO-numbers", null) {
        // SIRF design preview: numbers DEMO hain (asli data release ke baad aayega). Upar DEMO banner.
        val days = OwnerStats.lastDays(30).mapIndexed { i, d -> DayPoint(d, if (i < 22) 0 else (i - 21) * 9L + (i % 3) * 4, if (i < 22) 0 else (6 + i % 4).toLong()) }
        Box {
            AdminScreen(owner, false, null, null, 0,
                DashData(days.last().date, days.last().active, days.last().new, days, 188, 141, 47, 9,
                    listOf("1.2.0" to 170L, "purane / dusre" to 18L), listOf("vlc" to 31L, "seal" to 24L, "image-toolbox" to 19L, "localsend" to 12L),
                    listOf("vlc" to 40L, "seal" to 33L, "newpipe" to 21L), listOf("student" to 14L, "creator" to 9L, "privacy" to 5L), 23,
                    listOf("image-toolbox" to 9L, "vlc" to 6L), emptyMap(), listOf("none" to 120L, "google" to 52L, "email" to 10L, "phone" to 6L)),
                false, null, apps, AdminActions(), {})
            Text("DEMO NUMBERS — design preview", Modifier.align(Alignment.TopCenter).padding(top = 4.dp).background(Color(0xFFFF4D4D)).padding(horizontal = 10.dp, vertical = 3.dp), color = Color.White, fontSize = 11.sp)
        }
    }

    @Test fun v09_settings_privacy_admin() = shot("38-settings-glass-theme", null, UserSettings(theme = ThemeMode.GLASS, onboarded = true)) {
        SettingsScreen(UserSettings(theme = ThemeMode.GLASS, onboarded = true), HomeSection.DEFAULT.map { SectionPref(it, true) }, true, true, true, SettingsActions(), {})
    }

    private val glass = UserSettings(theme = ThemeMode.GLASS, onboarded = true, loginSeen = true)

    @Test fun g01_glass_home() = shot("40-glass-home", TabId.Home, glass) {
        HomeScreen(data, RemoteUi(), HomeSection.DEFAULT.map { SectionPref(it, true) }, emptyList(), emptyList(), isNew, {}, {}, {}, {}, {}, kits = kits, videos = videos)
    }

    @Test fun g02_glass_detail() = shot("41-glass-detail", null, glass) {
        DetailScreen(apps.first { it.slug == "vlc" }, apps, isNew, {}, {}, {}, {}, isFav = true, videos = videos, onPoster = {})
    }

    @Test fun g03_glass_kit() = shot("42-glass-kit", TabId.Home, glass) {
        KitScreen(kits.first { it.id == "privacy" }, apps, { src(it) }, { false }, isNew, { 6 }, {}, {}, {}, {}, {})
    }

    @Test fun g04_glass_categories() = shot("43-glass-categories", TabId.Categories, glass) { CategoriesScreen(apps) {} }

    @Test fun u01_update_popup() = shot("39-update-popup", TabId.Home) {
        HomeScreen(data, RemoteUi(), sectionsKitsFirst, emptyList(), emptyList(), isNew, {}, {}, {}, {}, {}, kits = kits, videos = videos)
        UpdateDialog(
            com.techkadak.app.update.Release("1.3.0", "## Kya naya hai\n- Naye kits\n- Poster me naye design\n- Bug fixes", "https://github.com/techkadak/techkadak-app/releases/download/v1.3.0/TechKadak-v1.3.apk", 5_400_000, "https://github.com/techkadak/techkadak-app/releases/tag/v1.3.0"),
            com.techkadak.app.update.DlState.Idle, { true }, {}, {}, {}, {}, {},
        )
    }

    @Test fun u02_update_popup_glass() = shot("39b-update-popup-glass", TabId.Home, glass) {
        HomeScreen(data, RemoteUi(), sectionsKitsFirst, emptyList(), emptyList(), isNew, {}, {}, {}, {}, {}, kits = kits, videos = videos)
        UpdateDialog(
            com.techkadak.app.update.Release("1.3.0", "Naye kits, poster me naye design, bug fixes", "https://github.com/techkadak/techkadak-app/releases/download/v1.3.0/TechKadak-v1.3.apk", 5_400_000, "https://github.com/techkadak/techkadak-app/releases/tag/v1.3.0"),
            com.techkadak.app.update.DlState.Running(0.42f), { true }, {}, {}, {}, {}, {},
        )
    }

    /** Poster PNGs (1080x1920 + 1080x1080) — wahi jo phone pe banta hai. */
    @Test fun p01_posters() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val dir = File(System.getProperty("tk.shotsDir") ?: "build/screens").parentFile!!.resolve("posters").apply { mkdirs() }
        for (slug in listOf("image-toolbox", "vlc", "localsend", "davinci-resolve")) {
            val a = apps.first { it.slug == slug }
            val icon = a.iconUrl(Config.SITE_BASE)?.let { IconLoader.peek(it) }
            for (f in PosterFormat.entries) {
                val b = Poster.render(ctx, a, icon, f)
                assert(b.width == f.w && b.height == f.h)
                File(dir, "poster-$slug-${f.name.lowercase()}.png").outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
            }
        }
    }
}
