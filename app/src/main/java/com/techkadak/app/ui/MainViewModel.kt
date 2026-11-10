package com.techkadak.app.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.techkadak.app.Config
import com.techkadak.app.data.AppItem
import com.techkadak.app.data.AppRepository
import com.techkadak.app.data.DataSource
import com.techkadak.app.data.Prefs
import android.app.Activity
import com.techkadak.app.BuildConfig
import com.techkadak.app.auth.AuthManager
import com.techkadak.app.auth.TkUser
import com.techkadak.app.data.CardStyle
import com.techkadak.app.data.IconLoader
import com.techkadak.app.data.RemoteUi
import com.techkadak.app.data.SectionPref
import com.techkadak.app.data.SettingsStore
import com.techkadak.app.data.SortMode
import com.techkadak.app.data.ThemeMode
import com.techkadak.app.data.UserSettings
import com.techkadak.app.firebase.TkFirebase
import com.techkadak.app.widget.TodayWidget
import com.techkadak.app.update.DlState
import com.techkadak.app.update.Release
import com.techkadak.app.update.Updater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UiData(
    val apps: List<AppItem> = emptyList(),
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val source: DataSource? = null,
    val savedAt: Long = 0L,
    val offlineNote: Boolean = false,
)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = AppRepository(app)
    val prefs = Prefs(app)
    private val updater = Updater(app)

    var data by mutableStateOf(UiData())
        private set
    val store = SettingsStore(app)
    /** null = DataStore abhi padh rahe hain (pehla frame). */
    var settings by mutableStateOf<UserSettings?>(null)
        private set
    var favourites by mutableStateOf<List<String>>(emptyList())
        private set
    var recentViewed by mutableStateOf<List<String>>(emptyList())
        private set
    var remote by mutableStateOf(RemoteUi())
        private set

    // ── Login (optional) ──
    val auth = AuthManager(app)
    var user by mutableStateOf<TkUser?>(null)
        private set
    var authBusy by mutableStateOf(false)
        private set
    var authError by mutableStateOf<String?>(null)
        private set
    var authInfo by mutableStateOf<String?>(null)
        private set
    /** Email verify abhi-abhi hua -> success animation. */
    var justVerified by mutableStateOf(false)
        private set
    var update by mutableStateOf<Release?>(null)
        private set
    var dl by mutableStateOf<DlState>(DlState.Idle)
        private set
    var updateChecking by mutableStateOf(false)
        private set
    var updateMsg by mutableStateOf<String?>(null)
        private set

    /**
     * App start ka kaam. ⚠️ Ye seedha `init` me NAHI hai: Kotlin class ke fields upar se neeche bante hain, aur v1.2.0 me
     * yahan se v1.2 wale fields (statsOn, videos, kits…) unke banne se PEHLE set ho rahe the -> launch pe NullPointerException
     * (har phone pe crash). Ab start() class ke sabse neeche wale init se chalta hai, jab saare fields ban chuke hote hain.
     * Har optional cheez alag try me hai, taaki koi ek fail ho to bhi app khule.
     */
    private fun start(app: Application) {
        val safe = com.techkadak.app.CrashLog.safeMode
        // Safe mode: pichli launch crash hui thi -> Liquid Glass (sabse bhaari theme) hata ke normal theme
        if (safe) viewModelScope.launch { runCatching { if (store.current().theme == ThemeMode.GLASS) store.setTheme(ThemeMode.AMOLED) } }
        viewModelScope.launch {
            try {
                val local = repo.loadLocal()
                data = data.copy(apps = local.apps, source = local.source, savedAt = local.savedAt, loading = local.apps.isEmpty())
            } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Throwable) { android.util.Log.w("TkVM", "loadLocal", e) }
            try { refresh(initial = true) } catch (e: Throwable) { android.util.Log.w("TkVM", "refresh", e) }
        }
        viewModelScope.launch {
            try { store.settings.collect { settings = it } }
            catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Throwable) { android.util.Log.w("TkVM", "settings", e); settings = UserSettings() }
        }
        viewModelScope.launch { try { store.favourites.collect { favourites = it } } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Throwable) { android.util.Log.w("TkVM", "favs", e) } }
        viewModelScope.launch { try { store.recent.collect { recentViewed = it } } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Throwable) { android.util.Log.w("TkVM", "recent", e) } }
        if (!safe) runCatching {
            TkFirebase.fetchRemoteUi(app) { r ->
                runCatching {
                    remote = r
                    app.getSharedPreferences("tk", 0).edit().putString("rc_featured", r.featuredSlug).apply()
                }
            }
        }
        runCatching { autoCheckUpdate() }
        user = runCatching { auth.current() }.getOrNull()
        runCatching { auth.addListener { user = runCatching { auth.current() }.getOrNull() } }
        runCatching { loadV12(remoteToo = !safe) }
    }

    // ───────────── v1.2: Kits, Videos, Stats, Admin ─────────────
    private val ctx12 get() = getApplication<Application>()
    /** v1.5 in-app browser (tabs, history, bookmarks, downloads) lives as long as the app process. */
    val browser by lazy { com.techkadak.app.browser.BrowserController(ctx12) }
    var bundledKits by mutableStateOf<List<com.techkadak.app.data.Kit>>(emptyList())
        private set
    var siteKits by mutableStateOf<List<com.techkadak.app.data.Kit>?>(null)
        private set
    /** Remote Config kits_json > website data/kits.json > bundled kits.json. Sirf catalog ke apps. */
    val kits: List<com.techkadak.app.data.Kit>
        get() {
            val cat = data.apps.takeIf { it.isNotEmpty() }
            val rc = remote.kitsJson?.let { com.techkadak.app.data.Kits.parse(it, cat) }
            val site = siteKits?.let { list -> if (cat == null) list else list.filter { k -> k.apps(cat).size >= 2 } }
            val bundled = if (cat == null) bundledKits else bundledKits.filter { it.apps(cat).size >= 2 }
            return com.techkadak.app.data.Kits.choose(rc?.takeIf { it.isNotEmpty() } ?: site, bundled)
        }
    var videos by mutableStateOf<List<com.techkadak.app.data.TkVideo>>(emptyList())
        private set
    var statsOn by mutableStateOf(true)
        private set

    private fun loadV12(remoteToo: Boolean = true) {
        statsOn = runCatching { com.techkadak.app.stats.Stats.enabled(ctx12) }.getOrDefault(true)
        viewModelScope.launch {
            try {
                val (k, cached) = withContext(Dispatchers.IO) {
                    val kj = runCatching { ctx12.assets.open("kits.json").bufferedReader().use { it.readText() } }.getOrNull()
                    val vf = java.io.File(ctx12.filesDir, "videos.xml")
                    val vx = runCatching { if (vf.exists()) vf.readText() else ctx12.assets.open("videos.xml").bufferedReader().use { it.readText() } }.getOrNull()
                    com.techkadak.app.data.Kits.parse(kj) to runCatching { com.techkadak.app.data.Videos.parseFeed(vx) }.getOrDefault(emptyList())
                }
                bundledKits = k
                if (cached.isNotEmpty()) videos = cached
            } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Throwable) { android.util.Log.w("TkVM", "v12 local", e) }
            if (remoteToo) refreshV12Remote()
        }
    }

    private fun refreshV12Remote() = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            runCatching {
                val xml = com.techkadak.app.data.Http.getText(com.techkadak.app.data.Videos.FEED_URL, "application/atom+xml")
                val list = com.techkadak.app.data.Videos.parseFeed(xml)
                if (list.isNotEmpty()) { java.io.File(ctx12.filesDir, "videos.xml").writeText(xml); list } else null
            }.getOrNull()
        }?.let { videos = it }
        withContext(Dispatchers.IO) {
            runCatching { com.techkadak.app.data.Kits.parse(com.techkadak.app.data.Http.getText(Config.SITE_BASE + "/data/kits.json", "application/json")) }.getOrNull()
        }?.takeIf { it.isNotEmpty() }?.let { siteKits = it }
    }

    /** Kit ke saare installable (APK) apps download queue me. Kitne queue hue wo return. */
    fun installKit(kit: com.techkadak.app.data.Kit): Int {
        val apps = kit.apps(data.apps).filter { a ->
            val src = sourceFor(a)
            src.kind == com.techkadak.app.install.ApkSource.Kind.APK && src.minSdk <= android.os.Build.VERSION.SDK_INT &&
                com.techkadak.app.install.ApkDownloads.installed(ctx, src.packageName) == null
        }
        apps.forEach { downloadApp(it) }
        if (apps.isNotEmpty()) com.techkadak.app.stats.Stats.event(ctx12, com.techkadak.app.stats.Stats.KIT_INSTALL, kit = kit.id)
        return apps.size
    }

    fun isAppInstalled(a: AppItem): Boolean { @Suppress("UNUSED_VARIABLE") val t = installTick; return com.techkadak.app.install.ApkDownloads.installed(ctx, sourceFor(a).packageName) != null }

    fun setStats(on: Boolean) { statsOn = on; com.techkadak.app.stats.Stats.setEnabled(ctx12, on) }
    fun trackPosterShare(a: AppItem) = com.techkadak.app.stats.Stats.event(ctx12, com.techkadak.app.stats.Stats.POSTER_SHARE, slug = a.slug)
    fun trackVideo(v: com.techkadak.app.data.TkVideo) = com.techkadak.app.stats.Stats.event(ctx12, com.techkadak.app.stats.Stats.VIDEO_OPEN)

    // Admin login: 5 galat try = 5 min lock (phone pe yaad rehta hai)
    var adminLockedSec by mutableStateOf(0)
        private set
    var dash by mutableStateOf<com.techkadak.app.stats.DashData?>(null)
        private set
    var dashLoading by mutableStateOf(false)
        private set
    var dashError by mutableStateOf<String?>(null)
        private set
    private val adminSp get() = ctx12.getSharedPreferences("tk_admin", 0)

    fun refreshAdminLock(): Int {
        val left = ((adminSp.getLong("lock_until", 0L) - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)
        adminLockedSec = left
        return left
    }

    fun adminLogin(email: String, pass: String) {
        if (refreshAdminLock() > 0) return
        if (!com.techkadak.app.stats.OwnerStats.isOwner(email)) {
            adminFail(); authError = com.techkadak.app.ui.tr("Ye owner ka Gmail nahi hai — Access denied", "That's not the owner's Gmail — access denied"); return
        }
        authRun({ auth.signIn(email, pass) }) {
            adminSp.edit().putInt("fails", 0).putLong("lock_until", 0).apply(); setLoginSeen()
            // Console me bana password account "unverified" hota hai. Ek baar verify link bhejo — verify ke baad
            // "Google se login" karne pe bhi password wala login safe rehta hai.
            val u = auth.current()
            if (u != null && !u.emailVerified && !adminSp.getBoolean("verify_sent", false)) {
                adminSp.edit().putBoolean("verify_sent", true).apply()
                viewModelScope.launch {
                    if (auth.resendVerification().isSuccess) authInfo = com.techkadak.app.ui.tr(
                        "Ek verify link tere Gmail pe bheja hai — ek baar tap kar dena 💚",
                        "A verify link was sent to your Gmail — tap it once 💚")
                }
            }
        }
        viewModelScope.launch {
            while (authBusy) kotlinx.coroutines.delay(100)
            if (authError != null && user == null) adminFail()
        }
    }

    private fun adminFail() {
        val f = adminSp.getInt("fails", 0) + 1
        if (f >= 5) adminSp.edit().putInt("fails", 0).putLong("lock_until", System.currentTimeMillis() + 5 * 60 * 1000L).apply()
        else adminSp.edit().putInt("fails", f).apply()
        refreshAdminLock()
    }

    fun adminForgot() = forgot(com.techkadak.app.stats.OwnerStats.OWNER_EMAIL)

    fun loadDash() {
        if (dashLoading) return
        dashLoading = true; dashError = null
        viewModelScope.launch {
            runCatching {
                com.techkadak.app.stats.OwnerStats.load(BuildConfig.VERSION_CODE, mapOf(10200 to "1.2.0", BuildConfig.VERSION_CODE to BuildConfig.VERSION_NAME))
            }.onSuccess { dash = it }.onFailure { e ->
                dashError = if (e.message?.contains("PERMISSION_DENIED", true) == true) com.techkadak.app.ui.tr("Access denied — ye account owner nahi hai", "Access denied — this account isn't the owner")
                else com.techkadak.app.ui.tr("Data load nahi hua: ", "Couldn't load data: ") + (e.message ?: "").take(120)
            }
            dashLoading = false
        }
    }

    private fun authRun(block: suspend () -> Result<*>, ok: (Any?) -> Unit = {}) {
        if (authBusy) return
        authBusy = true; authError = null; authInfo = null
        viewModelScope.launch {
            val r = block()
            authBusy = false
            val before = user
            user = auth.current()
            r.onSuccess { ok(it) }.onFailure { authError = it.message }
            val u = user
            if (r.isSuccess && u != null && before?.uid != u.uid) {
                com.techkadak.app.stats.Stats.event(getApplication(), com.techkadak.app.stats.Stats.LOGIN, loginType = com.techkadak.app.stats.Stats.normLogin(u.provider))
                com.techkadak.app.stats.Stats.flush(getApplication(), loginType = u.provider, force = true)
            }
        }
    }

    fun googleSignIn(activity: Activity) = authRun({ auth.signInWithGoogle(activity) }) {
        setLoginSeen()
        // v1.5: welcome card only for a brand-new Google account (once per uid)
        if (auth.takeNew()) auth.current()?.let { offerWelcome(it.uid, it.displayName.takeIf { n -> n != "Tech Kadak user" } ?: "") }
    }
    fun signUp(name: String, email: String, pass: String) = authRun({ auth.signUp(name, email, pass) }) {
        setLoginSeen(); authInfo = com.techkadak.app.ui.tr("Verification mail bhej diya 📩", "Verification mail sent 📩")
        offerWelcome(auth.current()?.uid, name)
    }

    /** v1.5 Welcome member card. Key = uid (signed in) or "guest_<install id>". One card per key, ever. */
    var welcome by mutableStateOf<WelcomeInfo?>(null)
    override fun onCleared() { runCatching { browser.destroyAll() }; super.onCleared() }
    private val memberSp get() = ctx12.getSharedPreferences("tk_member", android.content.Context.MODE_PRIVATE)
    private fun installId(): String = memberSp.getString("install_id", null) ?: java.util.UUID.randomUUID().toString().also { memberSp.edit().putString("install_id", it).apply() }
    private fun todayLabel() = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.ENGLISH).format(java.util.Date())
    private fun shownKeys(): Set<String> = memberSp.all.entries.filter { it.key.startsWith("welcome_shown_") && it.value == true }.map { it.key.removePrefix("welcome_shown_") }.toSet()
    /** Key of whoever is using the app right now. */
    private fun memberKey(): String = user?.uid ?: com.techkadak.app.member.WelcomePolicy.guestKey(installId())
    private fun memberName(): String = user?.displayName?.takeIf { it != "Tech Kadak user" } ?: "Dost"

    /** New account (email / Google / phone) just created. */
    fun offerWelcome(uid: String?, name: String) {
        val u = uid ?: return
        if (!com.techkadak.app.member.WelcomePolicy.shouldOffer(true, memberSp.getBoolean("welcome_shown_$u", false))) return
        showCard(u, u, name.trim().ifBlank { "Dost" })
    }

    private var launchChecked = false
    /** First launch after the update (or first run): existing users and guests get the card once too. */
    fun offerLaunchWelcome() {
        if (launchChecked || welcome != null) return
        launchChecked = true
        when (val d = com.techkadak.app.member.WelcomePolicy.onLaunch(user?.uid, installId(), shownKeys())) {
            is com.techkadak.app.member.WelcomePolicy.Launch.Show -> showCard(d.key, if (d.signedIn) d.key else null, memberName())
            is com.techkadak.app.member.WelcomePolicy.Launch.MarkOnly -> memberSp.edit().putBoolean("welcome_shown_${d.key}", true).apply()
            com.techkadak.app.member.WelcomePolicy.Launch.Skip -> Unit
        }
    }

    private fun showCard(key: String, uid: String?, display: String) {
        val sp = memberSp
        viewModelScope.launch {
            val install = installId()
            // sequential number from the counter for signed-in users; install-id number for guests / offline / rules not live. A number already shown never changes.
            val server = uid?.let { com.techkadak.app.member.MemberNumber.claim(ctx12, it, display) }
            val shown = sp.getLong("shown_n_$key", -1L).takeIf { it > 0L }
            val number = com.techkadak.app.member.MemberColor.resolve(shown, server, com.techkadak.app.member.MemberColor.offlineNumber(install))
            val since = sp.getString("since_$key", null) ?: com.techkadak.app.member.WelcomePolicy.memberSince(
                ctx12.getSharedPreferences("tk_stats", android.content.Context.MODE_PRIVATE).getString("first", null), todayLabel())
            sp.edit().putLong("shown_n_$key", number).putString("since_$key", since).apply()
            welcome = WelcomeInfo(key, display, number, since)
        }
    }

    /** v1.5: Profile card replay. Uses the number already shown for this key; never claims a new one. */
    fun replayWelcome() {
        val key = memberKey(); val sp = memberSp
        val number = sp.getLong("shown_n_$key", -1L).takeIf { it > 0L }
            ?: com.techkadak.app.member.MemberColor.offlineNumber(installId()).also { sp.edit().putLong("shown_n_$key", it).apply() }
        val since = sp.getString("since_$key", null) ?: todayLabel().also { sp.edit().putString("since_$key", it).apply() }
        welcome = WelcomeInfo(key, memberName(), number, since)
    }
    fun memberInfo(): Triple<String, Long, String>? {
        val key = memberKey()
        val n = memberSp.getLong("shown_n_$key", -1L).takeIf { it > 0L } ?: return null
        return Triple(memberName(), n, memberSp.getString("since_$key", null) ?: "")
    }
    /** True when this user (or guest) has a member card so Profile can show it. */
    fun hasMemberCard(): Boolean = memberSp.getLong("shown_n_${memberKey()}", -1L) > 0L
    fun dismissWelcome() {
        val w = welcome ?: return
        memberSp.edit().putBoolean("welcome_shown_${w.uid}", true).apply()
        welcome = null
    }
    data class WelcomeInfo(val uid: String, val name: String, val number: Long, val since: String)
    fun login(email: String, pass: String) = authRun({ auth.signIn(email, pass) }) { setLoginSeen() }
    fun forgot(email: String) = authRun({ auth.resetPassword(email) }) {
        authInfo = com.techkadak.app.ui.tr("Password reset link $email pe bhej diya", "Password reset link sent to $email")
    }
    fun resendVerification() = authRun({ auth.resendVerification() }) { authInfo = com.techkadak.app.ui.tr("Mail dobara bhej diya ✅", "Mail sent again ✅") }
    fun checkVerified() = authRun({ auth.reload() }) {
        val u = auth.current()
        if (u != null && u.emailVerified) justVerified = true
        else authError = com.techkadak.app.ui.tr("Abhi verify nahi hua — mail ka link tap karo, phir dobara dabao", "Not verified yet — tap the link in the mail, then try again")
    }
    fun finishVerified() { justVerified = false; authInfo = null }
    fun logout() = viewModelScope.launch { auth.signOut(); user = null; authError = null; authInfo = null }
    fun deleteAccount() = authRun({ auth.deleteAccount() }) { authInfo = com.techkadak.app.ui.tr("Account delete ho gaya", "Account deleted") }
    fun clearAuthMsgs() { authError = null; authInfo = null }

    // ── Phone OTP ──
    var phoneNumber by mutableStateOf("")
        private set
    var phoneCodeSent by mutableStateOf(false)
        private set
    var phoneAutoCode by mutableStateOf<String?>(null)
        private set
    var phoneVerified by mutableStateOf(false)
        private set
    private var verificationId: String? = null
    private var resendToken: com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken? = null

    fun sendOtp(activity: Activity, raw: String, resend: Boolean = false) {
        val p = AuthManager.normalizeIndianPhone(raw) ?: run { authError = com.techkadak.app.ui.tr("10 digit ka mobile number daalo", "Enter a 10-digit mobile number"); return }
        phoneNumber = p; authBusy = true; authError = null; authInfo = null
        auth.startPhone(activity, p, if (resend) resendToken else null) { ev ->
            when (ev) {
                is AuthManager.PhoneEvent.CodeSent -> { verificationId = ev.verificationId; resendToken = ev.token; phoneCodeSent = true; authBusy = false
                    authInfo = com.techkadak.app.ui.tr("OTP bhej diya 📩", "OTP sent 📩") }
                is AuthManager.PhoneEvent.AutoCode -> { phoneCodeSent = true; phoneAutoCode = ev.code; authBusy = true
                    viewModelScope.launch { finishPhone(auth.signInPhone(ev.credential)) } }
                is AuthManager.PhoneEvent.Failed -> { authBusy = false; authError = ev.message }
            }
        }
    }

    fun verifyOtp(code: String) {
        val id = verificationId ?: run { authError = com.techkadak.app.ui.tr("Pehle OTP bhejo", "Send the OTP first"); return }
        if (authBusy) return
        authBusy = true; authError = null
        viewModelScope.launch { finishPhone(auth.signInPhone(id, code)) }
    }

    private fun finishPhone(r: Result<TkUser>) {
        authBusy = false
        r.onSuccess {
            user = it; phoneVerified = true; setLoginSeen()
            if (auth.takeNew()) offerWelcome(it.uid, it.name ?: "")
        }.onFailure { authError = it.message }
    }

    fun resetPhone() { phoneCodeSent = false; phoneAutoCode = null; phoneVerified = false; verificationId = null; authError = null; authInfo = null }
    fun setLoginSeen() = viewModelScope.launch { store.setLoginSeen(true) }
    fun viewModelScopeReplayOnboarding() = viewModelScope.launch { store.setOnboarded(false) }

    /** Pull-to-refresh: same app-list refresh as before + Kits / Videos feeds. Never throws. */
    fun pullRefresh() {
        refresh()
        runCatching { refreshV12Remote() }
    }

    fun refresh(initial: Boolean = false) {
        if (data.refreshing) return
        data = data.copy(refreshing = !initial)
        viewModelScope.launch {
            val r = repo.fetchRemote()
            data = if (r != null) {
                markSeen(r.apps)
                runCatching { TodayWidget.refreshAll(getApplication()) }
                data.copy(apps = r.apps, source = r.source, savedAt = r.savedAt, loading = false, refreshing = false, offlineNote = false)
            } else data.copy(loading = false, refreshing = false, offlineNote = true)
        }
    }

    private fun markSeen(apps: List<AppItem>) {
        // Pehli baar app khole to aaj tak ke sab "seen"; baad me naye apps pe NEW badge.
        if (prefs.seenSlugs.isEmpty()) prefs.seenSlugs = apps.drop(1).map { it.slug }.toSet()
    }

    fun isNew(a: AppItem) = a.slug !in prefs.seenSlugs

    fun opened(a: AppItem) {
        if (isNew(a)) prefs.seenSlugs = prefs.seenSlugs + a.slug
        viewModelScope.launch { store.addRecent(a.slug) }
    }

    fun isFav(slug: String) = slug in favourites
    fun toggleFav(slug: String) {
        favourites = if (slug in favourites) favourites - slug else listOf(slug) + favourites
        viewModelScope.launch { store.toggleFavourite(slug) }
    }
    fun clearRecent() = viewModelScope.launch { store.clearRecent() }

    /** Remote Config se min_version_code isse bada = update zaroori. */
    val forceUpdate: Boolean get() = remote.needsForceUpdate(BuildConfig.VERSION_CODE)

    // ── Settings ──
    fun setTheme(v: ThemeMode) = viewModelScope.launch { store.setTheme(v) }
    fun setAccent(v: Long?) = viewModelScope.launch { store.setAccent(v) }
    fun setFontScale(v: Float) = viewModelScope.launch { store.setFontScale(v) }
    fun setCardStyle(v: CardStyle) = viewModelScope.launch { store.setCardStyle(v) }
    fun setSections(v: List<SectionPref>?) = viewModelScope.launch { store.setSections(v) }
    fun setEnglish(v: Boolean) = viewModelScope.launch { store.setEnglish(v) }
    fun setInAppBrowser(v: Boolean) = viewModelScope.launch { store.setInAppBrowser(v) }
    fun setRefreshAnim(v: Boolean) = viewModelScope.launch { store.setRefreshAnim(v) }
    fun setOpeningAnim(v: Boolean) = viewModelScope.launch { store.setOpeningAnim(v) }
    fun setOnboarded() = viewModelScope.launch {
        store.setOnboarded(true)
        com.techkadak.app.stats.Stats.event(getApplication(), com.techkadak.app.stats.Stats.ONBOARDING_DONE)
    }
    fun setSort(v: SortMode) = viewModelScope.launch { store.setSort(v) }
    fun resetLook() = viewModelScope.launch { store.resetLook() }

    /** Cache saaf: icons + saved list + recent searches. Favourites nahi hat-te. Returns MB saaf hua. */
    fun clearCache(onDone: (String) -> Unit) = viewModelScope.launch {
        val bytes = withContext(Dispatchers.IO) {
            val app = getApplication<Application>()
            val dirs = listOf(app.cacheDir)
            val size = dirs.sumOf { d -> d.walkTopDown().filter { it.isFile }.sumOf { it.length() } }
            dirs.forEach { d -> d.listFiles()?.forEach { it.deleteRecursively() } }
            size
        }
        IconLoader.clearMemory()
        prefs.recentSearches = emptyList()
        onDone("%.1f MB".format(bytes / 1_048_576f))
        refresh()
    }

    fun find(slug: String) = data.apps.firstOrNull { it.slug == slug }

    /** v1.8: Remote Config latest_version_code + apk_url. Naya ho to wahi Release (update card wahi). */
    private fun remoteRelease(): com.techkadak.app.update.Release? {
        val code = remote.latestVersionCode
        val url = remote.apkUrl ?: return null
        if (code <= BuildConfig.VERSION_CODE) return null
        val name = "${code / 10000}.${code / 100 % 100}.${code % 100}"
        return com.techkadak.app.update.Release(name, "", url, 0L, url)
    }

    /** v1.2: har app open pe check (koi throttle nahi). Offline = kuch nahi. */
    private fun autoCheckUpdate() {
        viewModelScope.launch {
            val r = updater.check() ?: remoteRelease()
            prefs.lastUpdateCheck = System.currentTimeMillis()
            if (r != null) { update = r; updater.readyFile(r)?.let { dl = DlState.Done(it) } }
        }
    }

    private var backgroundAt = 0L
    fun onAppBackground() { backgroundAt = System.currentTimeMillis() }
    /** 30+ min baad wapas aaye = naya open: pop-up phir dikhega + dobara check. */
    fun onAppForeground() {
        if (backgroundAt > 0 && System.currentTimeMillis() - backgroundAt >= com.techkadak.app.update.UpdatePolicy.NEW_OPEN_AFTER_MS) {
            dismissedThisOpen = false
            autoCheckUpdate()
        }
        backgroundAt = 0L
    }

    fun checkUpdateNow() {
        if (updateChecking) return
        updateChecking = true; updateMsg = null
        viewModelScope.launch {
            val r = updater.check() ?: remoteRelease()
            prefs.lastUpdateCheck = System.currentTimeMillis()
            updateChecking = false
            if (r != null) { update = r; updater.readyFile(r)?.let { dl = DlState.Done(it) } } else updateMsg = com.techkadak.app.ui.tr("Tu latest version pe hai ✅", "You're on the latest version ✅")
        }
    }

    fun dismissUpdate(skip: Boolean) {
        if (skip) prefs.skippedVersion = update?.version
        update = null; dl = DlState.Idle
    }

    /** v1.2: "Baad mein" = sirf is app-open ke liye band; agli baar app khole to pop-up phir. */
    var dismissedThisOpen by mutableStateOf(false)
        private set
    val updatePromptDue: Boolean get() = com.techkadak.app.update.UpdatePolicy.decide(
        update, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE, dismissedThisOpen, remote.minVersionCode,
    ) != com.techkadak.app.update.UpdatePolicy.Decision.NONE
    fun remindLater() { dismissedThisOpen = true }
    fun remindNow() { dismissedThisOpen = false }

    var history by mutableStateOf<List<com.techkadak.app.update.ReleaseInfo>>(emptyList())
        private set
    val installed: Pair<String, Long> by lazy { updater.installed() }
    fun loadHistory() = viewModelScope.launch { history = updater.history() }


    // ── In-app install (Play Store jaisa) ──
    private val ctx get() = getApplication<Application>()
    private val bundledSources by lazy { com.techkadak.app.install.ApkSource.loadBundled(ctx) }
    fun sourceFor(a: AppItem) = com.techkadak.app.install.AppSources.sourceFor(a, bundledSources)

    data class Preview(val r: com.techkadak.app.install.ResolvedApk? = null, val loading: Boolean = false, val error: String? = null, val at: Long = 0)
    val previews = androidx.compose.runtime.mutableStateMapOf<String, Preview>()
    /** onResume pe badhta hai -> installed status dobara padho (install ke baad "Open"). */
    var installTick by androidx.compose.runtime.mutableIntStateOf(0)
        private set
    fun refreshInstalled() { installTick++ }
    var openDownloadsTick by androidx.compose.runtime.mutableIntStateOf(0)
        private set
    fun openDownloads() { openDownloadsTick++ }

    /** Top bar ka quick toggle: Light <-> (AMOLED) dark */
    fun toggleLight() = setTheme(if (settings?.theme == ThemeMode.LIGHT) ThemeMode.AMOLED else ThemeMode.LIGHT)


    fun preview(a: AppItem) {
        val src = sourceFor(a)
        if (src.kind != com.techkadak.app.install.ApkSource.Kind.APK) return
        val p = previews[a.slug]
        if (p != null && (p.loading || System.currentTimeMillis() - p.at < 6 * 3600_000L)) return
        if (src.minSdk > android.os.Build.VERSION.SDK_INT) {
            previews[a.slug] = Preview(error = com.techkadak.app.ui.tr("Is app ko Android ${sdkName(src.minSdk)}+ chahiye — tumhare phone pe nahi chalega", "Needs Android ${sdkName(src.minSdk)}+"), at = System.currentTimeMillis()); return
        }
        previews[a.slug] = Preview(loading = true)
        viewModelScope.launch {
            val r = runCatching { com.techkadak.app.install.ApkResolver.resolve(src) }
            previews[a.slug] = Preview(r.getOrNull(), false, r.exceptionOrNull()?.let { com.techkadak.app.ui.tr("Source abhi nahi khula — INSTALL dabao, dobara try karenge", "Couldn't reach the source — tap INSTALL to retry") }, System.currentTimeMillis())
        }
    }

    private fun sdkName(sdk: Int) = when (sdk) { 26 -> "8"; 27 -> "8.1"; 28 -> "9"; 29 -> "10"; 30 -> "11"; 31 -> "12"; 32 -> "12L"; 33 -> "13"; 34 -> "14"; 35 -> "15"; else -> "SDK $sdk" }

    fun installState(a: AppItem, dl: com.techkadak.app.install.DlItem?): InstallUiState {
        @Suppress("UNUSED_VARIABLE") val tick = installTick
        val src = sourceFor(a)
        val inst = com.techkadak.app.install.ApkDownloads.installed(ctx, src.packageName)
        val p = previews[a.slug]
        val upd = inst != null && p?.r != null && com.techkadak.app.install.ApkResolver.isUpdate(inst.first, inst.second, p.r)
        return InstallUiState(src, p?.r, p?.loading == true, p?.error, dl, inst?.first, inst != null, upd)
    }

    fun downloadApp(a: AppItem) {
        val src = sourceFor(a)
        if (src.kind != com.techkadak.app.install.ApkSource.Kind.APK) return
        if (src.minSdk > android.os.Build.VERSION.SDK_INT) return
        val item = com.techkadak.app.install.AppSources.newItem(a, src).copy(total = previews[a.slug]?.r?.size ?: -1)
        com.techkadak.app.install.ApkDownloads.enqueue(ctx, item)
        com.techkadak.app.stats.Stats.event(ctx, com.techkadak.app.stats.Stats.INSTALL_CLICK, slug = a.slug)
    }
    fun pauseDl(slug: String) = com.techkadak.app.install.ApkDownloads.pause(ctx, slug)
    fun resumeDl(slug: String) = com.techkadak.app.install.ApkDownloads.resume(ctx, slug)
    fun cancelDl(slug: String) = com.techkadak.app.install.ApkDownloads.cancel(ctx, slug)
    fun removeDl(slug: String) = com.techkadak.app.install.ApkDownloads.remove(ctx, slug)
    fun installDl(slug: String) { com.techkadak.app.install.ApkInstaller.install(ctx, slug, background = false) }
    fun openPkg(pkg: String) { com.techkadak.app.install.ApkDownloads.launch(ctx, pkg) }
    fun isInstalled(d: com.techkadak.app.install.DlItem): Boolean { @Suppress("UNUSED_VARIABLE") val t = installTick; return com.techkadak.app.install.ApkDownloads.installed(ctx, d.packageName) != null }

    // ── Updates (catalog ke installed apps) ──
    data class PendingUpdate(val app: AppItem, val from: String?, val to: String?)
    var updates by mutableStateOf<List<PendingUpdate>>(emptyList())
        private set
    var updatesChecking by mutableStateOf(false)
        private set
    fun checkAppUpdates() {
        if (updatesChecking) return
        updatesChecking = true
        viewModelScope.launch {
            val out = mutableListOf<PendingUpdate>()
            for (a in data.apps) {
                val src = sourceFor(a)
                if (src.kind != com.techkadak.app.install.ApkSource.Kind.APK) continue
                val inst = com.techkadak.app.install.ApkDownloads.installed(ctx, src.packageName) ?: continue
                val r = runCatching { com.techkadak.app.install.ApkResolver.resolve(src) }.getOrNull() ?: continue
                previews[a.slug] = Preview(r, at = System.currentTimeMillis())
                if (com.techkadak.app.install.ApkResolver.isUpdate(inst.first, inst.second, r)) out += PendingUpdate(a, inst.first, r.versionName)
            }
            updates = out; updatesChecking = false
        }
    }
    fun updateAll() { updates.forEach { downloadApp(it.app) } }

    fun setAutoUpdate(v: com.techkadak.app.data.AutoUpdateMode) = viewModelScope.launch {
        store.setAutoUpdate(v); com.techkadak.app.install.AutoUpdateWorker.schedule(ctx, v)
    }

    fun canInstall() = updater.canInstall()
    fun openInstallPermission() = updater.openInstallPermission()

    fun startDownload() {
        val r = update ?: return
        if (dl is DlState.Running) return
        viewModelScope.launch { updater.download(r) { dl = it } }
    }

    fun install() {
        val d = dl as? DlState.Done ?: return
        runCatching { updater.install(d.file) }.onFailure { dl = DlState.Failed("Installer nahi khula") }
    }

    fun setDaily(on: Boolean) { prefs.dailyNotify = on }
    fun setPush(on: Boolean) { prefs.pushNotify = on; TkFirebase.setPush(getApplication(), on) }

    // ⚠️ YE INIT CLASS KE SABSE NEECHE HI RAHEGA — saare fields ban jaane ke baad hi start() chale (v1.2.0 launch crash ka fix).
    init {
        runCatching { com.techkadak.app.install.ApkDownloads.init(app) }
        start(app)
    }
}
