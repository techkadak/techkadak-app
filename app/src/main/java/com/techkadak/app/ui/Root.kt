package com.techkadak.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.techkadak.app.Config
import com.techkadak.app.data.AppItem
import com.techkadak.app.data.Catalog
import com.techkadak.app.data.UserSettings
import com.techkadak.app.firebase.TkFirebase
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp

enum class TabId(val hi: String, val en: String, val on: ImageVector, val off: ImageVector) {
    Home("Home", "Home", Icons.Rounded.Home, Icons.Outlined.Home),
    Categories("Categories", "Categories", Icons.Rounded.GridView, Icons.Outlined.GridView),
    Search("Search", "Search", Icons.Rounded.Search, Icons.Outlined.Search),
    Browser("Browser", "Browser", Icons.Rounded.Public, Icons.Outlined.Public),
    Favs("Pasand", "Saved", Icons.Rounded.Favorite, Icons.Outlined.FavoriteBorder),
    Profile("Profile", "Profile", Icons.Rounded.Person, Icons.Outlined.Person);

    val label: String get() = tr(hi, en)
}

sealed interface Route {
    data class Detail(val slug: String) : Route
    data class Category(val name: String) : Route
    data class AllApps(val category: String? = null) : Route
    data class Web(val url: String) : Route
    data object Search : Route
    data object Settings : Route
    data object About : Route
    data object NewApps : Route
    data object Licenses : Route
    data object Downloads : Route
    data object Kits : Route
    data class Kit(val id: String) : Route
    data object Videos : Route
    data object Admin : Route
    data object Feedback : Route
}

private enum class Overlay { Login, Verify, Phone }

@Composable
fun BottomBar(tab: TabId, newCount: Int, favCount: Int = 0, onTab: (TabId) -> Unit) {
    NavigationBar(containerColor = Tk.Surface, tonalElevation = 0.dp) {
        TabId.entries.forEach { t ->
            val sel = t == tab
            NavigationBarItem(
                selected = sel, onClick = { onTab(t) },
                icon = {
                    BadgedBox(badge = {
                        if (t == TabId.Home && newCount > 0) Badge(containerColor = Tk.Lime, contentColor = Tk.OnLime) { Text("$newCount") }
                    }) { Icon(if (sel) t.on else t.off, t.label) }
                },
                label = { Text(t.label, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, maxLines = 1, softWrap = false) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Tk.OnLime, indicatorColor = Tk.Lime, selectedTextColor = Tk.Lime,
                    unselectedIconColor = Tk.Muted, unselectedTextColor = Tk.Muted,
                ),
            )
        }
    }
}

/** Callbacks jo Activity deti hai (browser, share, Google login…). */
class Host(
    val onLink: (String) -> Unit = {},
    val onShare: (AppItem) -> Unit = {},
    val onShareApp: () -> Unit = {},
    val onOpenMail: () -> Unit = {},
    val onGoogle: () -> Unit = {},
    val onSendOtp: (String, Boolean) -> Unit = { _, _ -> },
    /** Play Store app (market://) — package ya https link */
    val onMarket: (String?, String?) -> Unit = { _, _ -> },
    /** Android 13+: pehle download pe notification permission (progress dikhane ke liye) */
    val onAskNotif: () -> Unit = {},
    /** v1.2: poster PNG share sheet (WhatsApp / Insta status) */
    val onSharePoster: (AppItem, PosterFormat, android.graphics.Bitmap) -> Unit = { _, _, _ -> },
)

/** Poori app ka UI (MainActivity aur screenshot tests dono yahi use karte hain). */
@Composable
fun TechKadakApp(vm: MainViewModel, openSlug: String?, host: Host, showSplash: Boolean = true) {
    // v1.5: Settings → "Opening animation" off = no intro (settings load a moment after the first frame, so this is also checked as they arrive)
    val openingOn = vm.settings?.openingAnim != false
    var splashDone by rememberSaveable { mutableStateOf(!showSplash || !openingOn) }
    LaunchedEffect(openingOn) { if (!openingOn) splashDone = true }
    val splash = if (splashDone) null else rememberSplash { splashDone = true }
    SplashHost(splash, onSkip = { splashDone = true }) {
        if (Tk.isGlass) GlassBackdrop { AppContent(vm, openSlug, host, splashDone) } else AppContent(vm, openSlug, host, splashDone)
    }
}

@Composable
private fun AppContent(vm: MainViewModel, openSlug: String?, host: Host, splashGone: Boolean = true) {
    val s = vm.settings ?: run { Box(Modifier.fillMaxSize().background(Tk.Black)); return }
    if (!s.onboarded) { OnboardingScreen(vm.data.apps) { vm.setOnboarded() }; return }
    val shareCtx = androidx.compose.ui.platform.LocalContext.current
    var askShare by remember { mutableStateOf(!com.techkadak.app.stats.DeviceShare.asked(shareCtx)) }
    if (askShare) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { },
            title = { Text(tr("Owner ko device dikhe?", "Show your device to the owner?")) },
            text = {
                Text(tr("Allow karoge to Tech Kadak ke owner ko is phone ka naam aur installed apps ke NAAM dikhenge, taaki behtar app suggestions banein. Messages, photos, passwords kabhi nahi. Settings se kabhi bhi band kar sakte ho.",
                    "If you allow, the Tech Kadak owner sees this phone's name and the NAMES of installed apps, to make better suggestions. Never messages, photos or passwords. You can turn it off in Settings any time."))
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    com.techkadak.app.stats.DeviceShare.set(shareCtx, true); askShare = false
                    Thread { com.techkadak.app.stats.Stats.flush(shareCtx, force = true) }.start()
                }) { Text(tr("Allow", "Allow")) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { com.techkadak.app.stats.DeviceShare.set(shareCtx, false); askShare = false }) { Text(tr("Abhi nahi", "Not now")) }
            },
        )
    }

    // v1.7: event pop-up (website events.json), once per event, never over other dialogs
    val evCtx = androidx.compose.ui.platform.LocalContext.current
    var evQueue by remember { mutableStateOf<List<com.techkadak.app.data.TkEvent>>(emptyList()) }
    val pendingEvent = evQueue.firstOrNull()
    LaunchedEffect(splashGone) {
        if (!splashGone) return@LaunchedEffect
        val seen = com.techkadak.app.data.Events.seen(evCtx)
        evQueue = com.techkadak.app.data.Events.fetch().filter { it.repeat || it.id !in seen }
    }
    pendingEvent?.let { ev ->
        if (!askShare && splashGone && !(vm.update != null && vm.updatePromptDue)) {
            EventPopup(ev, onClose = { com.techkadak.app.data.Events.markSeen(evCtx, ev.id); evQueue = evQueue.drop(1) },
                onOpen = { com.techkadak.app.data.Events.markSeen(evCtx, ev.id); evQueue = evQueue.drop(1); ev.url?.let { host.onLink(it) } })
        }
    }

    var tab by rememberSaveable { mutableStateOf(TabId.Home) }
    val stack = remember { mutableStateListOf<Route>() }
    val dls by com.techkadak.app.install.ApkDownloads.items.collectAsState()
    // Notification se "Downloads" khulna / Install
    LaunchedEffect(vm.openDownloadsTick) { if (vm.openDownloadsTick > 0 && stack.lastOrNull() != Route.Downloads) stack.add(Route.Downloads) }
    var searchFocus by remember { mutableStateOf(false) }
    var showUpdate by remember { mutableStateOf(true) }
    // v1.5: existing users + guests get the member card once on the first launch after the update (after the opening animation; give the update check a moment so its card goes first)
    LaunchedEffect(splashGone, vm.user?.uid) {
        if (!splashGone) return@LaunchedEffect
        var waited = 0
        while (vm.updateChecking && waited < 4000) { kotlinx.coroutines.delay(200); waited += 200 }
        vm.offerLaunchWelcome()
    }
    var recentQ by remember { mutableStateOf(vm.prefs.recentSearches) }
    var daily by remember { mutableStateOf(vm.prefs.dailyNotify) }
    var push by remember { mutableStateOf(vm.prefs.pushNotify) }
    var overlay by remember { mutableStateOf<Overlay?>(null) }
    var posterFor by remember { mutableStateOf<AppItem?>(null) }
    val openVideo: (com.techkadak.app.data.TkVideo) -> Unit = { v -> vm.trackVideo(v); host.onLink(v.url) }
    val data = vm.data
    val isNew: (AppItem) -> Boolean = { vm.isNew(it) }
    val user = vm.user
    val loginGate = !s.loginSeen && vm.auth.enabled && user == null

    // Login ke baad: email wale ko verify screen, Google wale seedha app me.
    LaunchedEffect(user?.uid, user?.emailVerified) {
        if (user != null && (overlay == Overlay.Login || (loginGate && overlay != Overlay.Phone))) overlay = if (user.needsVerification) Overlay.Verify else null
    }
    LaunchedEffect(openSlug, data.apps.size) {
        if (openSlug != null && vm.find(openSlug) != null && stack.lastOrNull() != Route.Detail(openSlug)) stack.add(Route.Detail(openSlug))
    }
    val siteHost = remember { Uri.parse(Config.SITE_BASE).host }
    val onWeb: (String) -> Unit = { url ->
        if (s.inAppBrowser && Uri.parse(url).host == siteHost) stack.add(Route.Web(url)) else host.onLink(url)
    }
    val open: (AppItem) -> Unit = { vm.opened(it); stack.add(Route.Detail(it.slug)) }
    val openCat: (String) -> Unit = { stack.add(Route.Category(it)) }
    val back: () -> Unit = { stack.removeLastOrNull() }
    val updateCard: @Composable () -> Unit = {
        UpdateCard(vm.updateChecking, vm.updateMsg, vm.update, onCheck = { vm.checkUpdateNow() }, onShow = { showUpdate = true; vm.remindNow() })
    }
    val newCount = remember(data.apps) { data.apps.count { vm.isNew(it) } }.coerceAtMost(9)
    val favApps = remember(data.apps, vm.favourites) { Catalog.bySlugs(data.apps, vm.favourites) }
    val recentApps = remember(data.apps, vm.recentViewed) { Catalog.bySlugs(data.apps, vm.recentViewed) }

    // ── Login / verify (optional, skip ho sakta hai) ──
    val ov = overlay ?: if (loginGate) Overlay.Login else null
    if (ov != null) {
        BackHandler { if (ov == Overlay.Phone) { vm.resetPhone(); overlay = Overlay.Login } else { if (loginGate) vm.setLoginSeen(); overlay = null; vm.clearAuthMsgs() } }
        when (ov) {
            Overlay.Login -> LoginScreen(
                vm.auth.enabled, vm.authBusy, vm.authError, vm.authInfo,
                AuthActions(
                    onGoogle = host.onGoogle, onSignUp = vm::signUp, onLogin = vm::login, onForgot = vm::forgot,
                    onSkip = { vm.setLoginSeen(); overlay = null; vm.clearAuthMsgs() },
                    onPhone = { vm.resetPhone(); overlay = Overlay.Phone },
                ),
                onBack = if (loginGate) null else ({ overlay = null; vm.clearAuthMsgs() }),
            )
            Overlay.Verify -> VerifyEmailScreen(
                user?.email ?: "", vm.authBusy, vm.authError, vm.authInfo, vm.justVerified,
                onResend = vm::resendVerification, onCheck = vm::checkVerified,
                onWrongEmail = { vm.deleteAccount(); overlay = Overlay.Login },
                onSkip = { overlay = null; vm.clearAuthMsgs() }, onOpenMail = host.onOpenMail,
                onContinue = { vm.finishVerified(); overlay = null },
            )
            Overlay.Phone -> PhoneLoginScreen(
                vm.phoneCodeSent, vm.phoneNumber, vm.authBusy, vm.authError, vm.authInfo, vm.phoneAutoCode, vm.phoneVerified,
                onSend = host.onSendOtp, onVerify = vm::verifyOtp,
                onBack = { vm.resetPhone(); overlay = Overlay.Login }, onSkip = { vm.setLoginSeen(); vm.resetPhone(); overlay = null },
                onContinue = { vm.resetPhone(); overlay = null },
            )
        }
        return
    }

    BackHandler(enabled = stack.isNotEmpty() || tab != TabId.Home) {
        if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex) else tab = TabId.Home
    }
    Scaffold(
        containerColor = Tk.Black,
        bottomBar = { if (stack.isEmpty()) BottomBar(tab, newCount, favApps.size) { t -> searchFocus = t == TabId.Search; tab = t } },
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad).background(Tk.Black)) {
            val top: Any = stack.lastOrNull() ?: tab
            AnimatedContent(
                targetState = top,
                transitionSpec = {
                    if (targetState is Route) (slideInHorizontally { it / 3 } + fadeIn()) togetherWith (fadeOut())
                    else fadeIn() togetherWith fadeOut()
                },
                label = "nav",
            ) { r ->
                when (r) {
                    is Route.Detail -> {
                        val a = vm.find(r.slug)
                        if (a == null) Empty(tr("Ye app ab list me nahi hai", "This app is no longer listed"))
                        else {
                            LaunchedEffect(a.slug) { vm.preview(a) }
                            val src = vm.sourceFor(a)
                            DetailScreen(
                                a, data.apps, isNew, onBack = back, onLink = host.onLink, onShare = host.onShare, onOpen = open,
                                isFav = vm.isFav(a.slug), onFav = { vm.toggleFav(a.slug) }, onWeb = onWeb,
                                install = vm.installState(a, dls[a.slug]),
                                installActions = InstallActions(
                                    onDownload = { host.onAskNotif(); vm.downloadApp(a) }, onPause = { vm.pauseDl(a.slug) }, onResume = { vm.resumeDl(a.slug) },
                                    onCancel = { vm.cancelDl(a.slug) }, onInstall = { vm.installDl(a.slug) },
                                    onOpen = { src.packageName?.let { vm.openPkg(it) } }, onPlay = { host.onMarket(src.packageName, src.link) },
                                    onLink = host.onLink, onDownloads = { stack.add(Route.Downloads) },
                                ),
                                videos = vm.videos, onVideo = openVideo, onPoster = { posterFor = a },
                                user = user, onLogin = { overlay = Overlay.Login },
                            )
                        }
                    }
                    is Route.Category -> CategoryScreen(r.name, data.apps, isNew, onBack = back, onOpen = open, sort = s.sort, onSort = { vm.setSort(it) })
                    is Route.AllApps -> TkRefreshBox(data.refreshing, { vm.pullRefresh() }) { AllAppsScreen(data.apps, isNew, back, open, s.sort, { vm.setSort(it) }, r.category) }
                    is Route.Web -> WebScreen(r.url, onBack = back, onExternal = host.onLink)
                    Route.Search -> SearchScreen(
                        data.apps, recentQ, "", true, isNew,
                        onQueryDone = { vm.prefs.addRecent(it); recentQ = vm.prefs.recentSearches },
                        onClearRecent = { vm.prefs.recentSearches = emptyList(); recentQ = emptyList() },
                        onOpen = open, onCategory = openCat, onBack = back, sort = s.sort, onSort = { vm.setSort(it) },
                    )
                    Route.Settings -> SettingsScreen(
                        s, s.sectionPrefs(vm.remote.homeSectionsOrder), push, daily, Config.FIREBASE_ON,
                        SettingsActions(
                            onTheme = { vm.setTheme(it) }, onAccent = { vm.setAccent(it) }, onFont = { vm.setFontScale(it) },
                            onCard = { vm.setCardStyle(it) }, onSections = { vm.setSections(it) }, onEnglish = { vm.setEnglish(it) },
                            onInApp = { vm.setInAppBrowser(it) }, onRefreshAnim = { vm.setRefreshAnim(it) }, onOpeningAnim = { vm.setOpeningAnim(it) }, onPush = { push = it; vm.setPush(it) }, onDaily = { daily = it; vm.setDaily(it) },
                            onClearCache = { cb -> vm.clearCache(cb) }, onReset = { vm.resetLook() },
                            onReplayOnboarding = { vm.viewModelScopeReplayOnboarding() }, onAbout = { stack.add(Route.About) },
                            onAutoUpdate = { vm.setAutoUpdate(it) }, onDownloads = { stack.add(Route.Downloads) },
                            onStats = { vm.setStats(it) }, onAdmin = { vm.clearAuthMsgs(); stack.add(Route.Admin) },
                        ),
                        onBack = back,
                        statsOn = vm.statsOn,
                    )
                    Route.Kits -> TkRefreshBox(data.refreshing, { vm.pullRefresh() }) { KitsListScreen(vm.kits, data.apps, onKit = { stack.add(Route.Kit(it.id)) }, onBack = back) }
                    is Route.Kit -> {
                        val k = vm.kits.firstOrNull { it.id == r.id }
                        if (k == null) Empty(tr("Ye kit ab nahi hai", "This kit is gone"))
                        else KitScreen(
                            k, data.apps, sourceFor = { vm.sourceFor(it) }, isInstalled = { vm.isAppInstalled(it) }, isNew = isNew,
                            onInstallAll = { host.onAskNotif(); vm.installKit(k) }, onOpen = open,
                            onPlay = { a -> val src = vm.sourceFor(a); host.onMarket(src.packageName, src.link) },
                            onLink = host.onLink, onDownloads = { stack.add(Route.Downloads) }, onBack = back,
                        )
                    }
                    Route.Videos -> TkRefreshBox(data.refreshing, { vm.pullRefresh() }) { VideosScreen(vm.videos, openVideo, onChannel = { host.onLink(Config.YOUTUBE) }, onBack = back) }
                    Route.Admin -> {
                        var lock by remember { mutableStateOf(vm.refreshAdminLock()) }
                        LaunchedEffect(lock) { if (lock > 0) { kotlinx.coroutines.delay(1000); lock = vm.refreshAdminLock() } }
                        LaunchedEffect(vm.adminLockedSec) { lock = vm.adminLockedSec }
                        AdminScreen(
                            user, vm.authBusy, vm.authError, vm.authInfo, lock, vm.dash, vm.dashLoading, vm.dashError, data.apps,
                            AdminActions(
                                onLogin = { e, p -> vm.adminLogin(e, p) }, onGoogle = host.onGoogle, onForgot = { vm.adminForgot() },
                                onLogout = { vm.logout() }, onRefresh = { vm.loadDash() },
                            ),
                            onBack = back,
                        )
                    }
                    Route.Downloads -> {
                        LaunchedEffect(Unit) { vm.refreshInstalled(); vm.checkAppUpdates() }
                        DownloadsScreen(
                            dls.values.toList(), { vm.isInstalled(it) }, back,
                            DownloadsActions(
                                onPause = vm::pauseDl, onResume = vm::resumeDl, onCancel = vm::cancelDl, onInstall = vm::installDl,
                                onOpenApp = vm::openPkg, onRemove = vm::removeDl,
                            ),
                            apps = data.apps,
                            updatesBlock = { UpdatesBlock(vm.updates, vm.updatesChecking, onUpdateAll = { host.onAskNotif(); vm.updateAll() }, onOpen = open) },
                        )
                    }
                    Route.About -> {
                        LaunchedEffect(Unit) { vm.loadHistory() }
                        AboutScreen(data.apps.size, back, host.onLink, onWeb, host.onShareApp, onLicenses = { stack.add(Route.Licenses) }, updateCard = updateCard,
                            installed = vm.installed, updateAvailable = vm.update?.version, history = vm.history)
                    }
                    Route.Licenses -> LicensesScreen(back, host.onLink)
                    Route.Feedback -> FeedbackScreen(back, user?.displayName?.takeIf { it != "Tech Kadak user" }.orEmpty())
                    Route.NewApps -> TkRefreshBox(data.refreshing, { vm.pullRefresh() }) { NewScreen(data.apps, isNew, updateCard, open, onBack = back) }
                    TabId.Home -> HomeScreen(
                        data, vm.remote, s.sectionPrefs(vm.remote.homeSectionsOrder), favApps, recentApps, isNew, open, openCat,
                        onSearch = { stack.add(Route.Search) }, onRefresh = { vm.pullRefresh() }, onLink = { u -> onWeb(u) },
                        onAll = { stack.add(Route.AllApps()) }, onNewList = { stack.add(Route.NewApps) }, onFavs = { tab = TabId.Favs },
                        onToggleTheme = { vm.toggleLight() }, onDownloads = { stack.add(Route.Downloads) }, onFeedback = { stack.add(Route.Feedback) }, activeDownloads = dls.values.count { it.active },
                        kits = vm.kits, videos = vm.videos, onKit = { stack.add(Route.Kit(it.id)) }, onKits = { stack.add(Route.Kits) },
                        onVideo = openVideo, onVideos = { stack.add(Route.Videos) },
                    )
                    TabId.Categories -> CategoriesScreen(data.apps, openCat)
                    TabId.Search -> SearchScreen(
                        data.apps, recentQ, "", searchFocus, isNew,
                        onQueryDone = { vm.prefs.addRecent(it); recentQ = vm.prefs.recentSearches },
                        onClearRecent = { vm.prefs.recentSearches = emptyList(); recentQ = emptyList() },
                        onOpen = open, onCategory = openCat, sort = s.sort, onSort = { vm.setSort(it) },
                    )
                    TabId.Favs -> FavouritesScreen(favApps, recentApps, isNew, open, onClearRecent = { vm.clearRecent() }, onExplore = { tab = TabId.Home })
                    TabId.Profile -> ProfileScreen(
                        data.apps.size, data.source, user, vm.auth.enabled, favApps.size,
                        ProfileActions(
                            onLogin = { overlay = Overlay.Login }, onVerify = { overlay = Overlay.Verify }, onLogout = { vm.logout() }, onDelete = { vm.deleteAccount() },
                            onSettings = { stack.add(Route.Settings) }, onAbout = { stack.add(Route.About) }, onShareApp = host.onShareApp,
                            onRequest = { host.onLink(Config.APP_REQUEST) }, onFavs = { tab = TabId.Favs }, onNew = { stack.add(Route.NewApps) }, onLink = host.onLink,
                        ),
                        member = if (vm.hasMemberCard()) vm.memberInfo() else null,
                        onMemberReplay = { vm.replayWelcome() },
                        updateCard = updateCard,
                    )
                    TabId.Browser -> BrowserScreen(vm.browser)
                    else -> {}
                }
            }
        }
    }
    posterFor?.let { pa ->
        PosterDialog(pa, onShare = { f, b -> vm.trackPosterShare(pa); host.onSharePoster(pa, f, b) }, onDismiss = { posterFor = null })
    }
    val up = vm.update
    if (vm.forceUpdate && up == null) {
        ForceUpdateDialog(vm.remote.minVersionCode) {
            vm.checkUpdateNow(); host.onLink(Config.RELEASES_PAGE)
        }
    }
    // v1.5: welcome member card, once per new account, after the opening animation
    // queue: the update card goes first; the member card follows once it is dismissed
    vm.welcome?.let { w ->
        if (splashGone && !(up != null && vm.updatePromptDue)) WelcomeMemberCard(w.name, w.number, w.since, onDone = { vm.dismissWelcome() })
    }
    // v1.5: the update card comes up after the opening animation, never under it
    if (up != null && vm.updatePromptDue && splashGone) {
        UpdateDialog(
            up, vm.dl, canInstall = { vm.canInstall() },
            onDownload = { vm.startDownload() }, onInstall = { vm.install() }, onPermission = { vm.openInstallPermission() },
            onLater = { showUpdate = false; vm.remindLater() }, onSkip = { vm.dismissUpdate(skip = true) },
            force = vm.forceUpdate,
        )
    }
}

/** Settings + Remote Config ke hisaab se theme lagao. */
@Composable
fun TkThemed(vm: MainViewModel, content: @Composable () -> Unit) {
    val rc = vm.remote.defaultAccent?.let { androidx.compose.ui.graphics.Color(it) }
    TkTheme(vm.settings ?: UserSettings(), rc, content)
}

/** v1.7.1: event pop-up — chhota card (poori screen nahi), upar ✕ cancel, banner image. */
@Composable
private fun EventPopup(ev: com.techkadak.app.data.TkEvent, onClose: () -> Unit, onOpen: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var bmp by remember(ev.id) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(ev.image) { ev.image?.let { bmp = com.techkadak.app.data.IconLoader.load(ctx, it, 1080) } }
    androidx.compose.ui.window.Dialog(onDismissRequest = onClose) {
        androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth()) {
            androidx.compose.foundation.layout.Column(
                Modifier.fillMaxWidth().padding(top = 14.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(22.dp)).background(Tk.Black),
            ) {
                bmp?.let {
                    androidx.compose.foundation.Image(it.asImageBitmap(), null, Modifier.fillMaxWidth(), contentScale = androidx.compose.ui.layout.ContentScale.FillWidth)
                }
                if (bmp == null || ev.image == null) {
                    Text(ev.emoji + " " + ev.title, Modifier.padding(16.dp, 22.dp, 16.dp, 4.dp), color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.titleLarge)
                    if (ev.body.isNotEmpty()) Text(ev.body, Modifier.padding(16.dp, 0.dp, 16.dp, 8.dp), color = androidx.compose.ui.graphics.Color(0xFFCCCCCC))
                }
                androidx.compose.material3.Button(
                    onClick = onOpen, modifier = Modifier.fillMaxWidth().padding(14.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF9BFF2E), contentColor = androidx.compose.ui.graphics.Color.Black),
                ) { Text(if (ev.url != null) ev.button else "OK") }
            }
            androidx.compose.material3.IconButton(
                onClick = onClose,
                modifier = Modifier.align(androidx.compose.ui.Alignment.TopEnd).size(40.dp).clip(androidx.compose.foundation.shape.CircleShape).background(androidx.compose.ui.graphics.Color(0xFFFFFFFF)),
            ) { Text("\u2715", color = androidx.compose.ui.graphics.Color.Black) }
        }
    }
}
