package com.techkadak.app

import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.techkadak.app.data.AppItem
import com.techkadak.app.data.AppsParser
import com.techkadak.app.data.DataSource
import com.techkadak.app.data.IconLoader
import com.techkadak.app.update.DlState
import com.techkadak.app.update.Release
import com.techkadak.app.ui.*
import com.techkadak.app.auth.TkUser
import com.techkadak.app.data.CardStyle
import com.techkadak.app.data.HomeSection
import com.techkadak.app.data.RemoteUi
import com.techkadak.app.data.SectionPref
import com.techkadak.app.data.SortMode
import com.techkadak.app.data.ThemeMode
import com.techkadak.app.data.UserSettings
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.LaunchedEffect
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config as RConfig
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Phone jaise screenshots (Roborazzi) — files/techkadak-app/screens/ me copy hote hain. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@RConfig(sdk = [34], qualifiers = "w393dp-h852dp-xxhdpi", application = android.app.Application::class)
class ScreenshotTest {
    @get:Rule val rule = createComposeRule()
    private lateinit var apps: List<AppItem>
    private val out = System.getProperty("tk.shotsDir") ?: "build/screens"
    private val newSlugs = mutableSetOf<String>()
    private val isNew: (AppItem) -> Boolean = { it.slug in newSlugs }

    @Before fun setUp() {
        apps = AppsParser.parse(File("src/main/assets/apps.json").readText())
        newSlugs += apps.take(2).map { it.slug }
        val dir = File(System.getProperty("tk.iconsDir") ?: "build/test-icons")
        apps.forEach { a ->
            val f = File(dir, a.slug + ".png")
            val url = a.iconUrl(Config.SITE_BASE) ?: return@forEach
            if (f.exists()) BitmapFactory.decodeFile(f.path)?.let { IconLoader.put(url, it) }
        }
    }

    private fun shot(name: String, tab: TabId?, settings: UserSettings = UserSettings(onboarded = true, loginSeen = true), content: @Composable () -> Unit) {
        rule.setContent {
            TkTheme(settings) {
                Scaffold(containerColor = Tk.Black, bottomBar = { if (tab != null) BottomBar(tab, 2) {} }) { p ->
                    Box(Modifier.fillMaxSize().padding(p)) { content() }
                }
            }
        }
        rule.mainClock.advanceTimeBy(1500)
        rule.waitForIdle()
        captureScreenRoboImage("$out/$name.png")
    }

    private val data get() = UiData(apps = apps, loading = false, source = DataSource.LIVE)
    private val sections = HomeSection.DEFAULT.map { SectionPref(it, true) }
    private val favs get() = listOf("vlc", apps[1].slug, apps[4].slug, apps[7].slug, apps[9].slug).mapNotNull { s -> apps.firstOrNull { it.slug == s } }
    private val recent get() = apps.drop(10).take(6)
    @Composable private fun home(remote: RemoteUi = RemoteUi()) = HomeScreen(data, remote, sections, favs, recent, isNew, {}, {}, {}, {}, {})

    @Test fun s01_home() = shot("01-home", TabId.Home) { home() }

    @Test fun s01b_home_light() = shot("01b-home-light", TabId.Home, UserSettings(theme = ThemeMode.LIGHT, onboarded = true)) {
        home(RemoteUi(announcementText = "Naya: Tech Kadak app v1.1 aa gaya 🎉"))
    }

    @Test fun s01c_home_orange_list() = shot("01c-home-dark-orange-list", TabId.Home,
        UserSettings(theme = ThemeMode.DARK, accent = 0xFFFF8A1F, cardStyle = CardStyle.LIST, onboarded = true,
            sections = listOf(SectionPref(HomeSection.FAVOURITES, true), SectionPref(HomeSection.NEW, true), SectionPref(HomeSection.FEATURED, false)) +
                HomeSection.DEFAULT.drop(1).filter { it != HomeSection.FAVOURITES && it != HomeSection.NEW }.map { SectionPref(it, true) })) {
        HomeScreen(data, RemoteUi(), UserSettings.decodeSections("favourites:1,new:1,featured:0")!!, favs, recent, isNew, {}, {}, {}, {}, {})
    }

    @Test fun s01d_home_compact() = shot("01d-home-compact-mint", TabId.Home, UserSettings(accent = 0xFF2EFFB0, cardStyle = CardStyle.COMPACT, onboarded = true)) { home() }

    @Test fun s02_categories() = shot("02-categories", TabId.Categories) { CategoriesScreen(apps) {} }

    @Test fun s03_category() = shot("03-category-ai-tools", null) { CategoryScreen("AI Tools", apps, isNew, {}, {}) }

    @Test fun s03b_all_apps_az() = shot("03b-all-apps-az-free", null) { AllAppsScreen(apps, isNew, {}, {}, SortMode.AZ, {}) }

    @Test fun s04_detail() = shot("04-app-detail", null) {
        val a = apps.first { it.slug == "vlc" }
        DetailScreen(a, apps, isNew, {}, {}, {}, {}, isFav = true)
    }

    @Test fun s04b_detail_limit() = shot("04b-app-detail-2", null) {
        val a = apps.firstOrNull { it.limit != null && it.replaces != null && it.icon != null } ?: apps[1]
        DetailScreen(a, apps, isNew, {}, {}, {}, {})
    }

    @Test fun s05_search() = shot("05-search", TabId.Search) {
        SearchScreen(apps, listOf("pdf", "photo editor"), "", false, isNew, {}, {}, {}, {})
    }

    @Test fun s05b_search_typed() = shot("05b-search-results", TabId.Search) {
        SearchScreen(apps, emptyList(), "pdf", false, isNew, {}, {}, {}, {}, initialSubmitted = true)
    }

    @Test fun s06_favs() = shot("06-favourites", TabId.Favs) { FavouritesScreen(favs, recent, isNew, {}, {}, {}) }

    @Test fun s06b_favs_empty() = shot("06b-favourites-empty", TabId.Favs) { FavouritesScreen(emptyList(), emptyList(), isNew, {}, {}, {}) }

    @Test fun s07_profile() = shot("07-profile", TabId.Profile) {
        ProfileScreen(apps.size, DataSource.LIVE, null, true, 5, ProfileActions()) { UpdateCard(false, null, null, {}, {}) }
    }

    @Test fun s07b_profile_logged_in() = shot("07b-profile-logged-in", TabId.Profile) {
        ProfileScreen(apps.size, DataSource.LIVE, TkUser("u1", "Rohit Saini", "ridhirohit0@gmail.com", null, true, "google"), true, 5, ProfileActions()) { UpdateCard(false, null, null, {}, {}) }
    }

    @Test fun s08_settings() = shot("08-settings", null) {
        val s = UserSettings(onboarded = true)
        SettingsScreen(s, s.sectionPrefs(null), true, true, true, SettingsActions(), {})
    }

    @Test fun s08b_settings_light() = shot("08b-settings-light", null, UserSettings(theme = ThemeMode.LIGHT, fontScale = 1.1f, onboarded = true)) {
        val s = UserSettings(theme = ThemeMode.LIGHT, fontScale = 1.1f, onboarded = true)
        SettingsScreen(s, s.sectionPrefs(null), true, true, true, SettingsActions(), {})
    }

    @Test fun s08c_color_picker() = shot("08c-color-picker", null) {
        val s = UserSettings(onboarded = true)
        SettingsScreen(s, s.sectionPrefs(null), true, true, true, SettingsActions(), {})
        ColorPickerDialog(0xFFFF8A1F, {}, {})
    }

    @Test fun s09_about() = shot("09-about", null) {
        AboutScreen(apps.size, {}, {}, {}, {}, updateCard = { UpdateCard(false, null, null, {}, {}) },
            history = listOf(com.techkadak.app.update.ReleaseInfo("1.1.0", "2026-11-04", "• Customise: theme, colour, font\n• Favourites ❤️\n• Widget + login", ""), com.techkadak.app.update.ReleaseInfo("1.0.0", "2026-11-04", "• Pehla version", "")))
    }

    @Test fun s09b_licenses() = shot("09b-licenses", null) { LicensesScreen({}, {}) }

    @Test fun s10a_onboarding() = shot("10-onboarding-1", null) { OnboardingScreen(apps, pageOverride = 0, appearOverride = 1f) {} }
    @Test fun s10b_onboarding() = shot("10-onboarding-2", null) { OnboardingScreen(apps, pageOverride = 1, appearOverride = 1f) {} }
    @Test fun s10c_onboarding() = shot("10-onboarding-3", null) { OnboardingScreen(apps, pageOverride = 2, appearOverride = 1f) {} }
    @Test fun s10d_onboarding() = shot("10-onboarding-4", null) { OnboardingScreen(apps, pageOverride = 3, appearOverride = 1f) {} }

    private fun splash(i: Int, p: Float) = shot("17-splash-$i", null) { SplashHost(rememberSplash(p) {}, {}) { home() } }
    @Test fun s17a() = splash(0, 0.07f)
    @Test fun s17b() = splash(1, 0.24f)
    @Test fun s17c() = splash(2, 0.40f)
    @Test fun s17d() = splash(3, 0.53f)
    @Test fun s17e() = splash(4, 0.66f)
    @Test fun s17f() = splash(5, 0.77f)
    @Test fun s17g() = splash(6, 0.88f)

    private fun phone(name: String, sent: Boolean, code: String, success: Float?) = shot(name, null) {
        PhoneLoginScreen(sent, "+91 98765 43210", false, null, null, if (sent && success == null) "471932" else null, success != null,
            { _, _ -> }, {}, {}, {}, {}, codeOverride = code, successOverride = success)
    }
    @Test fun s18a() = phone("18-phone-0", false, "", null)
    @Test fun s18b() = phone("18-phone-1", true, "471", null)
    @Test fun s18c() = phone("18-phone-2", true, "471932", 0.4f)
    @Test fun s18d() = phone("18-phone-3", true, "471932", 1f)

    @Test fun s11_login() = shot("11-login", null) { LoginScreen(true, false, null, null, AuthActions()) }

    @Test fun s11b_login_email() = shot("11b-login-email", null) { LoginScreen(true, false, null, null, AuthActions(), startWithEmail = true) }

    private fun verifyFrame(name: String, success: Float?, focus: Int) = shot(name, null) {
        VerifyEmailScreen("ridhirohit0@gmail.com", false, null, null, success != null, {}, {}, {}, {}, {}, {},
            cooldownStart = 22, successOverride = success, focusOverride = focus)
    }
    @Test fun s12a_verify() = verifyFrame("12-verify-0", null, 1)
    @Test fun s12b_verify() = verifyFrame("12-verify-1", 0.18f, 0)
    @Test fun s12c_verify() = verifyFrame("12-verify-2", 0.42f, 0)
    @Test fun s12d_verify() = verifyFrame("12-verify-3", 0.62f, 0)
    @Test fun s12e_verify() = verifyFrame("12-verify-4", 0.8f, 0)
    @Test fun s12f_verify() = verifyFrame("12-verify-5", 1f, 0)

    @Test fun s13_update_dialog() = shot("13-update-dialog", TabId.Home) {
        home()
        UpdateDialog(
            Release("1.2.0", "• Naya design\n• Search aur fast\n• Bug fixes", "https://github.com/x.apk", 6_300_000, ""),
            DlState.Running(0.42f), { true }, {}, {}, {}, {}, {},
        )
    }

    @Test fun s14_force_update() = shot("14-force-update", TabId.Home) { home(RemoteUi(maintenanceMessage = "Server update chal raha hai, 10 min me sab normal ✌️")); ForceUpdateDialog(10200) {} }

    @Test fun s15_loading() = shot("15-loading-shimmer", TabId.Home) {
        HomeScreen(UiData(), RemoteUi(), sections, emptyList(), emptyList(), isNew, {}, {}, {}, {}, {})
    }

    @OptIn(androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi::class)
    @Test fun s16_widget() {
        val ctx = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val app = apps.first()
        val icon = app.iconUrl(Config.SITE_BASE)?.let { IconLoader.peek(it) }
        val rv = kotlinx.coroutines.runBlocking {
            androidx.glance.appwidget.GlanceRemoteViews().compose(ctx, androidx.compose.ui.unit.DpSize(320.dp, 150.dp)) {
                com.techkadak.app.widget.TodayContent(app, icon)
            }.remoteViews
        }
        val frame = android.widget.FrameLayout(ctx)
        val v = rv.apply(ctx, frame)
        val dm = ctx.resources.displayMetrics
        val w = (320 * dm.density).toInt(); val h = (150 * dm.density).toInt()
        v.measure(android.view.View.MeasureSpec.makeMeasureSpec(w, android.view.View.MeasureSpec.EXACTLY), android.view.View.MeasureSpec.makeMeasureSpec(h, android.view.View.MeasureSpec.EXACTLY))
        v.layout(0, 0, w, h)
        val bmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
        v.draw(android.graphics.Canvas(bmp))
        File(out).mkdirs()
        File("$out/16-widget.png").outputStream().use { bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }

    // ── In-app install ──
    private val vlcSrc = com.techkadak.app.install.ApkSource(com.techkadak.app.install.ApkSource.Kind.APK, "fdroid:org.videolan.vlc", "org.videolan.vlc", "F-Droid")
    private val vlcPrev = com.techkadak.app.install.ResolvedApk("https://f-droid.org/repo/org.videolan.vlc_13070208.apk", 51460280, "3.7.0", 13070208, null, "vlc.apk")
    private fun dlItem(slug: String, st: com.techkadak.app.install.DlStatus, done: Long, total: Long, label: String = "F-Droid", err: String? = null): com.techkadak.app.install.DlItem {
        val a = apps.firstOrNull { it.slug == slug } ?: apps[0]
        return com.techkadak.app.install.DlItem(a.slug, a.name, null, label, "fdroid:x", null, null, st, done, total, error = err)
    }
    private fun detail(name: String, st: InstallUiState, slug: String = "vlc", clock: Float = 0.3f) = shot(name, null) {
        val a = apps.first { it.slug == slug }
        DetailScreen(a, apps, isNew, {}, {}, {}, {}, install = st, botClock = clock)
    }
    @Test fun s19a() = detail("19-install-ready", InstallUiState(vlcSrc, vlcPrev))
    @Test fun s19b() = detail("19b-install-downloading", InstallUiState(vlcSrc, vlcPrev, dl = dlItem("vlc", com.techkadak.app.install.DlStatus.RUNNING, 21_600_000, 51_460_280)))
    @Test fun s19c() = detail("19c-install-done", InstallUiState(vlcSrc, vlcPrev, dl = dlItem("vlc", com.techkadak.app.install.DlStatus.DONE, 51_460_280, 51_460_280)))
    @Test fun s19d() = detail("19d-install-failed", InstallUiState(vlcSrc, vlcPrev, dl = dlItem("vlc", com.techkadak.app.install.DlStatus.FAILED, 9_000_000, 51_460_280, err = "Net check karke dobara try karo")))
    @Test fun s19e() = detail("19e-installed-update", InstallUiState(vlcSrc, vlcPrev, installedVersion = "3.6.4", installed = true, hasUpdate = true))
    @Test fun s19f() = detail("19f-play-only", InstallUiState(com.techkadak.app.install.ApkSource(com.techkadak.app.install.ApkSource.Kind.PLAY, null, "org.zotero.android", "Play Store")), slug = apps.first().slug)
    @Test fun s19g() = detail("19g-pc-web", InstallUiState(com.techkadak.app.install.ApkSource(com.techkadak.app.install.ApkSource.Kind.WEB, null, null, "PC / Web", link = "https://obsproject.com/")), slug = apps.first().slug)

    private fun bot(name: String, mood: BotMood, p: Float, clock: Float) = shot(name, null) {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
            KadakBot(mood, p, Modifier.fillMaxWidth().height(200.dp), clock = clock)
        }
    }
    @Test fun s20a() = bot("20-bot-working", BotMood.WORKING, 0.45f, 0.25f)
    @Test fun s20b() = bot("20b-bot-happy", BotMood.HAPPY, 1f, 0.5f)
    @Test fun s20c() = bot("20c-bot-sad", BotMood.SAD, 0.3f, 0.4f)
    @Test fun s20d() = bot("20d-bot-sleepy", BotMood.SLEEPY, 0.6f, 0.3f)

    @Test fun s21_downloads() = shot("21-downloads", null) {
        val s = apps.map { it.slug }
        DownloadsScreen(
            listOf(
                dlItem("vlc", com.techkadak.app.install.DlStatus.RUNNING, 21_600_000, 51_460_280),
                dlItem(s[1], com.techkadak.app.install.DlStatus.QUEUED, 0, 18_200_000, "Official GitHub"),
                dlItem(s[2], com.techkadak.app.install.DlStatus.PAUSED, 6_100_000, 14_700_000, "Official GitHub"),
                dlItem(s[3], com.techkadak.app.install.DlStatus.DONE, 9_400_000, 9_400_000, "F-Droid"),
            ),
            { false }, {}, DownloadsActions(), apps, botClock = 0.3f,
            updatesBlock = { UpdatesBlock(listOf(MainViewModel.PendingUpdate(apps[5], "1.4.0", "1.5.2"), MainViewModel.PendingUpdate(apps[6], "2.0", "2.1")), false, {}, {}) },
        )
    }
    @Test fun s21b_downloads_empty() = shot("21b-downloads-empty", null) { DownloadsScreen(emptyList(), { false }, {}, DownloadsActions(), apps, botClock = 0.3f) }
    @Test fun s22_home_toggle() = shot("22-home-topbar", TabId.Home) {
        HomeScreen(data, RemoteUi(), sections, favs, recent, isNew, {}, {}, {}, {}, {}, onToggleTheme = {}, onDownloads = {}, activeDownloads = 2)
    }
    @Test fun s22b_home_light() = shot("22b-home-light-topbar", TabId.Home, UserSettings(onboarded = true, loginSeen = true, theme = com.techkadak.app.data.ThemeMode.LIGHT)) {
        HomeScreen(data, RemoteUi(), sections, favs, recent, isNew, {}, {}, {}, {}, {}, onToggleTheme = {}, onDownloads = {}, activeDownloads = 1)
    }
    @Test fun s19h_detail_light() = shot("19h-install-downloading-light", null, UserSettings(onboarded = true, loginSeen = true, theme = com.techkadak.app.data.ThemeMode.LIGHT)) {
        val a = apps.first { it.slug == "vlc" }
        DetailScreen(a, apps, isNew, {}, {}, {}, {}, install = InstallUiState(vlcSrc, vlcPrev, dl = dlItem("vlc", com.techkadak.app.install.DlStatus.RUNNING, 30_000_000, 51_460_280)), botClock = 0.6f)
    }
    @org.robolectric.annotation.Config(qualifiers = "w411dp-h2400dp")
    @Test fun s08d_settings_autoupdate() = shot("08d-settings-full", null) {
        SettingsScreen(UserSettings(onboarded = true, loginSeen = true), sections, true, true, true, SettingsActions(), null)
    }
}
