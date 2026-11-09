package com.techkadak.app.browser

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.MutableContextWrapper
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Message
import android.view.View
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.ByteArrayInputStream

/** One browser tab (state is Compose state, the WebView is created lazily and kept while the tab lives). */
class BTab(val id: Int, val incognito: Boolean, startUrl: String?) {
    var url by mutableStateOf(startUrl.orEmpty())
    var title by mutableStateOf("")
    var progress by mutableIntStateOf(0)
    var canBack by mutableStateOf(false)
    var canForward by mutableStateOf(false)
    var desktop by mutableStateOf(false)
    var loading by mutableStateOf(false)
    val isHome: Boolean get() = url.isBlank()
    internal var web: WebView? = null
}

data class DownloadRec(val id: Long, val name: String, val url: String, val at: Long)

class FileChooserRequest(val callback: ValueCallback<Array<Uri>>, val intent: Intent)

/**
 * v1.5 in-app browser: tabs, history, bookmarks, downloads, incognito, desktop mode, find in page, dark mode, ad-block.
 * Only the INTERNET permission is needed (downloads go through the system DownloadManager).
 */
class BrowserController(private val app: Context) {
    private val sp = app.getSharedPreferences("tk_browser", Context.MODE_PRIVATE)
    private val ctxWrapper = MutableContextWrapper(app)
    private var nextId = 1
    val tabs = mutableStateListOf<BTab>()
    var currentId by mutableIntStateOf(-1)
    var history by mutableStateOf(BrowserLists.decodeHistory(sp.getString("history", null)))
    var bookmarks by mutableStateOf(BrowserLists.decodeBookmarks(sp.getString("bookmarks", null)))
    var downloads by mutableStateOf(decodeDownloads(sp.getString("downloads", null)))
    var engine by mutableStateOf(SearchEngine.byId(sp.getString("engine", null)))
    var adBlock by mutableStateOf(sp.getBoolean("adblock", true))
    var dark by mutableStateOf(sp.getBoolean("dark", false))
    var findQuery by mutableStateOf("")
    var findActive by mutableStateOf(false)
    var findCount by mutableIntStateOf(0)
    var findIndex by mutableIntStateOf(0)
    var customView by mutableStateOf<View?>(null)
    private var customCb: WebChromeClient.CustomViewCallback? = null
    var message by mutableStateOf<String?>(null)
    var fileChooser: ((FileChooserRequest) -> Unit)? = null
    var needStoragePermission: ((() -> Unit) -> Unit)? = null
    private val incognitoHosts = mutableSetOf<String>()
    private val normalHosts = mutableSetOf<String>()
    private var defaultUa: String? = null

    val current: BTab? get() = tabs.firstOrNull { it.id == currentId }
    val incognitoCount: Int get() = tabs.count { it.incognito }

    init {
        // restore the normal tabs from last time (lazy: pages load when a tab is shown)
        sp.getString("tabs", null)?.lines()?.filter { it.isNotBlank() }?.take(12)?.forEach { u -> tabs.add(BTab(nextId++, false, if (u == "about:home") "" else u)) }
        if (tabs.isEmpty()) tabs.add(BTab(nextId++, false, null))
        currentId = sp.getInt("current", 0).coerceIn(0, tabs.size - 1).let { tabs[it].id }
    }

    fun attachActivity(c: Context) { ctxWrapper.baseContext = c }
    fun detachActivity() { ctxWrapper.baseContext = app }

    // ── tabs ──
    fun newTab(incognito: Boolean = false, url: String? = null): BTab {
        val t = BTab(nextId++, incognito, url)
        tabs.add(t); currentId = t.id; closeFind(); persistTabs(); return t
    }
    fun select(t: BTab) { if (t.id != currentId) { closeFind(); currentId = t.id; persistTabs() } }
    fun closeTab(t: BTab) {
        val i = tabs.indexOf(t); if (i < 0) return
        destroy(t); tabs.removeAt(i)
        if (tabs.isEmpty()) tabs.add(BTab(nextId++, false, null))
        if (currentId == t.id) currentId = tabs[(i - 1).coerceAtLeast(0).coerceAtMost(tabs.size - 1)].id
        if (t.incognito && incognitoCount == 0) clearIncognito()
        closeFind(); persistTabs()
    }
    fun closeAllIncognito() { tabs.filter { it.incognito }.forEach { closeTab(it) } }

    private fun destroy(t: BTab) {
        t.web?.let { w ->
            runCatching { if (t.incognito) w.clearHistory(); w.stopLoading(); (w.parent as? android.view.ViewGroup)?.removeView(w); w.destroy() }
        }
        t.web = null
    }

    private fun persistTabs() {
        val normal = tabs.filter { !it.incognito }
        val idx = normal.indexOfFirst { it.id == currentId }.coerceAtLeast(0)
        sp.edit().putString("tabs", normal.joinToString("\n") { it.url.ifBlank { "about:home" } }).putInt("current", idx).apply()
    }
    fun saveState() { persistTabs() }

    // ── navigation ──
    fun go(t: BTab, input: String) {
        android.util.Log.d("TkBrowser", "go input=[" + input + "] tab=" + t.id)
        when (val r = UrlInput.resolve(input, engine)) {
            is Resolved.Url -> load(t, r.url)
            is Resolved.Search -> load(t, r.url)
            Resolved.Blocked -> message = "Ye address nahi khul sakta"
        }
    }
    private fun load(t: BTab, url: String) { t.url = url; t.loading = true; webFor(t).loadUrl(url) }
    fun goHome(t: BTab) { t.web?.stopLoading(); t.url = ""; t.title = ""; t.progress = 0; t.loading = false; t.canBack = false; t.canForward = false; persistTabs() }
    fun back(t: BTab): Boolean { val w = t.web; return if (w != null && w.canGoBack()) { w.goBack(); true } else if (!t.isHome && w != null) { goHome(t); true } else false }
    fun forward(t: BTab) { t.web?.takeIf { it.canGoForward() }?.goForward() }
    fun reload(t: BTab) { if (t.loading) t.web?.stopLoading() else t.web?.reload() }
    fun canHandleBack(): Boolean = customView != null || findActive || current?.let { !it.isHome } == true

    // ── bookmarks / history ──
    fun toggleBookmark(t: BTab) {
        if (t.isHome) return
        bookmarks = BrowserLists.toggleBookmark(bookmarks, t.url, t.title, System.currentTimeMillis())
        sp.edit().putString("bookmarks", BrowserLists.encodeBookmarks(bookmarks)).apply()
    }
    fun removeBookmark(b: Bookmark) { bookmarks = bookmarks.filterNot { it.url == b.url }; sp.edit().putString("bookmarks", BrowserLists.encodeBookmarks(bookmarks)).apply() }
    fun isBookmarked(t: BTab) = !t.isHome && BrowserLists.isBookmarked(bookmarks, t.url)
    private fun record(t: BTab, url: String, title: String) {
        val host = UrlInput.hostOf(url)
        if (t.incognito) { if (host.isNotBlank()) incognitoHosts.add(host); return }
        if (host.isNotBlank()) normalHosts.add(host)
        history = BrowserLists.addHistory(history, url, title, System.currentTimeMillis())
        sp.edit().putString("history", BrowserLists.encodeHistory(history)).apply()
    }
    fun removeHistory(e: HistoryEntry) { history = BrowserLists.removeHistory(history, e.url, e.at); sp.edit().putString("history", BrowserLists.encodeHistory(history)).apply() }
    fun clearHistory() { history = emptyList(); sp.edit().remove("history").apply() }
    fun clearData() {
        clearHistory()
        runCatching { CookieManager.getInstance().removeAllCookies(null); CookieManager.getInstance().flush(); WebStorage.getInstance().deleteAllData() }
        tabs.forEach { it.web?.clearCache(true) }
        message = "Browsing data saaf ho gaya"
    }

    /** Closing the last incognito tab expires cookies set for hosts that no normal tab visited. */
    private fun clearIncognito() {
        val cm = CookieManager.getInstance()
        val histHosts = history.map { UrlInput.hostOf(it.url) }
        IncognitoCleaner.hostsToClear(incognitoHosts, normalHosts + histHosts).forEach { h ->
            val header = cm.getCookie("https://$h")
            IncognitoCleaner.expiryCookies(h, header).forEach { c -> cm.setCookie("https://$h", c) }
        }
        runCatching { cm.flush() }
        incognitoHosts.clear()
    }

    // ── settings ──
    fun chooseEngine(e: SearchEngine) { engine = e; sp.edit().putString("engine", e.id).apply() }
    fun chooseAdBlock(on: Boolean) { adBlock = on; sp.edit().putBoolean("adblock", on).apply() }
    fun chooseDark(on: Boolean) {
        dark = on; sp.edit().putBoolean("dark", on).apply()
        tabs.forEach { t -> t.web?.let { it.setBackgroundColor(if (on) 0xFF0A0A0A.toInt() else 0xFFFFFFFF.toInt()); applyDark(it); if (Build.VERSION.SDK_INT < 29) injectDarkCss(it, on) } }
    }
    fun setDesktop(t: BTab, on: Boolean) {
        t.desktop = on
        t.web?.let { w ->
            w.settings.userAgentString = BrowserUa.ua(on, defaultUa ?: "")
            w.settings.useWideViewPort = true; w.settings.loadWithOverviewMode = on
            w.reload()
        }
    }

    @Suppress("DEPRECATION")
    private fun applyDark(w: WebView) {
        val s = w.settings
        if (Build.VERSION.SDK_INT >= 33) s.isAlgorithmicDarkeningAllowed = dark
        else if (Build.VERSION.SDK_INT >= 29) s.forceDark = if (dark) WebSettings.FORCE_DARK_ON else WebSettings.FORCE_DARK_OFF
    }
    private fun injectDarkCss(w: WebView, on: Boolean) {
        val js = if (on) "(function(){if(document.getElementById('tkdark'))return;var s=document.createElement('style');s.id='tkdark';s.textContent='${BrowserUa.DARK_CSS}';document.documentElement.appendChild(s);})()"
        else "(function(){var s=document.getElementById('tkdark');if(s)s.remove();})()"
        w.evaluateJavascript(js, null)
    }

    // ── find in page ──
    fun openFind() { findActive = true }
    fun closeFind() { if (findActive || findQuery.isNotEmpty()) { current?.web?.clearMatches() }; findActive = false; findQuery = ""; findCount = 0; findIndex = 0 }
    fun find(q: String) {
        findQuery = q
        val w = current?.web ?: return
        if (q.isBlank()) { w.clearMatches(); findCount = 0; findIndex = 0 } else w.findAllAsync(q)
    }
    fun findNext(forward: Boolean) { current?.web?.findNext(forward) }

    // ── share / external ──
    fun share(t: BTab) {
        if (t.isHome) return
        val i = Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, t.url).putExtra(Intent.EXTRA_SUBJECT, t.title), "Share karo").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { ctxWrapper.startActivity(i) }
    }
    fun openExternal(t: BTab) {
        if (t.isHome) return
        runCatching { ctxWrapper.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(t.url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.onFailure { message = "Koi browser nahi mila" }
    }

    // ── downloads ──
    fun startDownload(url: String, ua: String?, contentDisposition: String?, mime: String?, incognito: Boolean) {
        if (!(url.startsWith("http://") || url.startsWith("https://"))) { message = "Is link ko download nahi kar sakte"; return }
        val go = { enqueue(url, ua, contentDisposition, mime) }
        // WRITE_EXTERNAL_STORAGE is needed only on Android 9 and older (maxSdk 28); Android 10+ uses scoped storage via DownloadManager
        if (Build.VERSION.SDK_INT <= 28 && app.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            val ask = needStoragePermission
            if (ask == null) message = "Storage permission chahiye" else ask(go)
        } else go()
    }
    @Suppress("DEPRECATION")
    private fun enqueue(url: String, ua: String?, cd: String?, mime: String?) {
        runCatching {
            val name = DownloadNames.fileName(url, cd, mime)
            val req = DownloadManager.Request(Uri.parse(url))
                .setTitle(name).setDescription("Tech Kadak browser")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name)
            mime?.takeIf { it.isNotBlank() && it != "application/octet-stream" }?.let { req.setMimeType(it) }
            ua?.let { req.addRequestHeader("User-Agent", it) }
            CookieManager.getInstance().getCookie(url)?.let { req.addRequestHeader("Cookie", it) }
            val id = (app.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(req)
            downloads = (listOf(DownloadRec(id, name, url, System.currentTimeMillis())) + downloads).take(100)
            saveDownloads()
            message = "Download shuru: $name"
        }.onFailure { message = "Download shuru nahi hua" }
    }
    private fun saveDownloads() { sp.edit().putString("downloads", downloads.joinToString("\n") { "${it.id}\t${it.at}\t${it.name.replace('\t', ' ')}\t${it.url.replace('\t', ' ')}" }).apply() }
    private fun decodeDownloads(s: String?): List<DownloadRec> = s.orEmpty().lines().mapNotNull {
        val p = it.split('\t'); if (p.size < 4) null else p[0].toLongOrNull()?.let { id -> DownloadRec(id, p[2], p[3], p[1].toLongOrNull() ?: 0L) }
    }
    data class DlStatus(val rec: DownloadRec, val state: Int, val done: Long, val total: Long, val uri: Uri?, val mime: String?)
    fun queryDownloads(): List<DlStatus> {
        val dm = app.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        return downloads.map { r ->
            runCatching {
                dm.query(DownloadManager.Query().setFilterById(r.id)).use { c ->
                    if (c != null && c.moveToFirst()) {
                        val st = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                        val done = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                        val tot = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                        val mime = c.getString(c.getColumnIndexOrThrow(DownloadManager.COLUMN_MEDIA_TYPE))
                        DlStatus(r, st, done, tot, if (st == DownloadManager.STATUS_SUCCESSFUL) dm.getUriForDownloadedFile(r.id) else null, mime)
                    } else DlStatus(r, -1, 0, 0, null, null) // removed outside the app
                }
            }.getOrDefault(DlStatus(r, -1, 0, 0, null, null))
        }
    }
    fun openDownload(d: DlStatus) {
        val u = d.uri ?: return
        val i = Intent(Intent.ACTION_VIEW).setDataAndType(u, d.mime ?: "*/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        try { ctxWrapper.startActivity(i) } catch (e: ActivityNotFoundException) { message = "Is file ko kholne wala app nahi mila" } catch (e: Exception) { message = "File nahi khul payi" }
    }
    fun removeDownload(r: DownloadRec) {
        runCatching { (app.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).remove(r.id) }
        downloads = downloads.filterNot { it.id == r.id }; saveDownloads()
    }

    // ── WebView ──
    fun lifecycle(resume: Boolean) {
        tabs.forEach { t -> t.web?.let { if (resume && t.id == currentId) it.onResume() else it.onPause() } }
    }
    fun destroyAll() { tabs.forEach { destroy(it) }; saveState() }

    @SuppressLint("SetJavaScriptEnabled")
    fun webFor(t: BTab): WebView {
        t.web?.let { return it }
        val w = WebView(ctxWrapper)
        t.web = w
        configure(t, w)
        return w
    }

    @Suppress("DEPRECATION")
    @SuppressLint("SetJavaScriptEnabled")
    private fun configure(t: BTab, w: WebView) {
        val s = w.settings
        if (defaultUa == null) defaultUa = s.userAgentString
        s.javaScriptEnabled = true
        s.domStorageEnabled = true
        s.allowFileAccess = false; s.allowContentAccess = false
        s.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        s.setSupportMultipleWindows(true)
        s.javaScriptCanOpenWindowsAutomatically = false
        s.builtInZoomControls = true; s.displayZoomControls = false; s.setSupportZoom(true)
        s.useWideViewPort = true; s.loadWithOverviewMode = t.desktop
        s.mediaPlaybackRequiresUserGesture = true
        s.setGeolocationEnabled(false)
        if (t.incognito) { s.cacheMode = WebSettings.LOAD_NO_CACHE; s.saveFormData = false }
        if (t.desktop) s.userAgentString = BrowserUa.DESKTOP
        applyDark(w)
        w.setBackgroundColor(if (dark) 0xFF0A0A0A.toInt() else 0xFFFFFFFF.toInt())
        w.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val u = request.url; val sch = u.scheme?.lowercase()
                return when (sch) {
                    "http", "https" -> AdBlock.isBlocked(u.host.orEmpty(), adBlock) && request.isForMainFrame.not()
                    "tel", "mailto", "sms", "geo" -> { runCatching { ctxWrapper.startActivity(Intent(Intent.ACTION_VIEW, u).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }; true }
                    "about" -> false
                    else -> true // intent:, file:, javascript:, market:, upi: ... never opened from a web page
                }
            }
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                if (adBlock && AdBlock.isBlocked(request.url.host.orEmpty(), true)) return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
                return null
            }
            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                if (url != null && !url.startsWith("data:") && url != "about:blank") t.url = url
                t.loading = true; t.progress = 5
            }
            override fun onPageFinished(view: WebView, url: String?) {
                t.loading = false; t.progress = 100
                t.canBack = view.canGoBack(); t.canForward = view.canGoForward()
                val u = url ?: return
                if (!u.startsWith("data:") && u != "about:blank") { t.url = u; t.title = view.title.orEmpty(); record(t, u, t.title); persistTabs() }
                if (dark && Build.VERSION.SDK_INT < 29) injectDarkCss(view, true)
            }
            override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) { t.canBack = view.canGoBack(); t.canForward = view.canGoForward() }
            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: android.webkit.WebResourceError) {
                if (!request.isForMainFrame) return
                val failing = request.url.toString().replace("\"", "%22")
                val html = "<html><head><meta name=viewport content='width=device-width,initial-scale=1'></head><body style='background:#0a0a0a;color:#fff;font-family:sans-serif;text-align:center;padding:48px 24px'>" +
                    "<h2 style='color:#9bff2e'>Page nahi khula</h2><p style='color:#aaa'>${if (error.description?.toString()?.contains("CLEARTEXT", true) == true) "Ye site sirf http (secure nahi) hai, isliye block hai." else "Internet check karo ya address galat hai."}</p><p><a href=\"$failing\" style='color:#0a0a0a;background:#9bff2e;padding:12px 22px;border-radius:24px;text-decoration:none;font-weight:bold'>Dobara try karo</a></p></body></html>"
                view.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
                t.loading = false; t.url = request.url.toString()
            }
        }
        w.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) { t.progress = newProgress; t.loading = newProgress < 100 }
            override fun onReceivedTitle(view: WebView, title: String?) { t.title = title.orEmpty() }
            override fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message): Boolean {
                if (!isUserGesture) return false // popup blocker: pages can't open windows on their own
                val nt = BTab(nextId++, t.incognito, null)
                tabs.add(nt); currentId = nt.id; closeFind()
                val nw = webFor(nt)
                (resultMsg.obj as WebView.WebViewTransport).webView = nw
                resultMsg.sendToTarget()
                return true
            }
            override fun onCloseWindow(window: WebView) { tabs.firstOrNull { it.web === window }?.let { closeTab(it) } }
            override fun onShowFileChooser(webView: WebView, cb: ValueCallback<Array<Uri>>, params: FileChooserParams): Boolean {
                val ask = fileChooser ?: run { cb.onReceiveValue(null); return true }
                return runCatching { ask(FileChooserRequest(cb, params.createIntent())); true }.getOrElse { cb.onReceiveValue(null); true }
            }
            override fun onPermissionRequest(request: PermissionRequest) { request.deny() } // no camera/mic/location for web pages
            override fun onGeolocationPermissionsShowPrompt(origin: String?, callback: GeolocationPermissions.Callback?) { callback?.invoke(origin, false, false) }
            override fun onShowCustomView(view: View, callback: CustomViewCallback) { customView = view; customCb = callback }
            override fun onHideCustomView() { customView = null; customCb?.onCustomViewHidden(); customCb = null }
        }
        w.setDownloadListener { url, ua, cd, mime, _ ->
            startDownload(url, ua, cd, mime, t.incognito)
            // a link that is only a file leaves a blank page behind: go back to where the user was (or the start page)
            if (w.canGoBack()) w.goBack() else if (w.url.isNullOrBlank() || w.url == "about:blank" || w.contentHeight == 0) goHome(t)
        }
        w.setFindListener { active, count, done -> if (done) { findIndex = if (count == 0) 0 else active + 1; findCount = count } }
        if (!t.isHome && w.url == null) w.loadUrl(t.url)
    }

    fun hideCustomView() { customView?.let { customView = null; customCb?.onCustomViewHidden(); customCb = null } }
}
