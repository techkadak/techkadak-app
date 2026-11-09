package com.techkadak.app.ui

import android.Manifest
import android.app.Activity
import android.app.DownloadManager
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.techkadak.app.browser.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class Panel { None, Tabs, History, Bookmarks, Downloads }

/** v1.5 in-app browser tab. All state lives in [BrowserController] so tabs survive switching to another bottom tab. */
@Composable
fun BrowserScreen(ctl: BrowserController) {
    val ctx = LocalContext.current
    var panel by remember { mutableStateOf(Panel.None) }
    val tab = ctl.current ?: return
    var menu by remember { mutableStateOf(false) }

    // activity glue: context for dialogs, storage permission (Android 9 and older), file picker
    var pendingGo by remember { mutableStateOf<(() -> Unit)?>(null) }
    val permL = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) pendingGo?.invoke() else ctl.message = tr("Storage permission nahi mili", "Storage permission denied")
        pendingGo = null
    }
    var chooser by remember { mutableStateOf<FileChooserRequest?>(null) }
    val fileL = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val c = chooser; chooser = null
        c?.callback?.onReceiveValue(if (r.resultCode == Activity.RESULT_OK) WebChromeClient.FileChooserParams.parseResult(r.resultCode, r.data) else null)
    }
    DisposableEffect(ctl) {
        (ctx as? Activity)?.let { ctl.attachActivity(it) }
        ctl.needStoragePermission = { go -> pendingGo = go; permL.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE) }
        ctl.fileChooser = { req -> chooser?.callback?.onReceiveValue(null); chooser = req; runCatching { fileL.launch(req.intent) }.onFailure { req.callback.onReceiveValue(null); chooser = null } }
        ctl.lifecycle(true)
        onDispose { ctl.lifecycle(false); ctl.fileChooser = null; ctl.needStoragePermission = null; ctl.saveState(); ctl.detachActivity() }
    }
    LaunchedEffect(ctl.message) { ctl.message?.let { Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show(); ctl.message = null } }
    LaunchedEffect(tab.id) { ctl.lifecycle(true) }

    BackHandler(enabled = panel != Panel.None || ctl.canHandleBack()) {
        when {
            panel != Panel.None -> panel = Panel.None
            ctl.customView != null -> ctl.hideCustomView()
            ctl.findActive -> ctl.closeFind()
            else -> ctl.back(tab)
        }
    }

    Box(Modifier.fillMaxSize().background(Tk.Black).testTag("browser_root")) {
        Column(Modifier.fillMaxSize()) {
            AddressBar(ctl, tab, menu, { menu = it }, { panel = it })
            if (tab.loading && tab.progress in 1..99) LinearProgressIndicator({ tab.progress / 100f }, Modifier.fillMaxWidth().height(2.dp), color = Tk.Lime, trackColor = Tk.Surface)
            else Spacer(Modifier.height(2.dp))
            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (tab.isHome) StartPage(ctl, tab)
                else AndroidView(
                    factory = { FrameLayout(it) },
                    update = { fl ->
                        val w = ctl.webFor(tab)
                        if (fl.childCount != 1 || fl.getChildAt(0) !== w) {
                            fl.removeAllViews(); (w.parent as? ViewGroup)?.removeView(w)
                            fl.addView(w, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            if (ctl.findActive) FindBar(ctl) else NavBar(ctl, tab) { panel = it }
        }
        // full-screen video from a web page
        ctl.customView?.let { v -> AndroidView(factory = { v.apply { (parent as? ViewGroup)?.removeView(this) } }, modifier = Modifier.fillMaxSize().background(Color.Black)) }
        when (panel) {
            Panel.Tabs -> TabsPanel(ctl) { panel = Panel.None }
            Panel.History -> HistoryPanel(ctl, { panel = Panel.None }) { url -> panel = Panel.None; ctl.go(tab, url) }
            Panel.Bookmarks -> BookmarksPanel(ctl, { panel = Panel.None }) { url -> panel = Panel.None; ctl.go(tab, url) }
            Panel.Downloads -> DownloadsPanel(ctl) { panel = Panel.None }
            Panel.None -> {}
        }
    }
}

@Composable
private fun AddressBar(ctl: BrowserController, tab: BTab, menu: Boolean, setMenu: (Boolean) -> Unit, open: (Panel) -> Unit) {
    val focus = LocalFocusManager.current
    var focused by remember { mutableStateOf(false) }
    var tfv by remember(tab.id) { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(if (tab.isHome) "" else tab.url)) }
    val text = tfv.text
    // the tap that focuses the field also places the cursor: select everything a moment later so typing replaces the address
    LaunchedEffect(focused) { if (focused) { kotlinx.coroutines.delay(150); tfv = tfv.copy(selection = androidx.compose.ui.text.TextRange(0, tfv.text.length)) } }
    LaunchedEffect(tab.url, focused) { if (!focused) tfv = androidx.compose.ui.text.input.TextFieldValue(if (tab.isHome) "" else tab.url) }
    val shown = if (focused) text else if (tab.isHome) "" else UrlInput.display(tab.url)
    Row(
        Modifier.fillMaxWidth().background(if (tab.incognito) Color(0xFF15151A) else Tk.Surface).padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.weight(1f).height(42.dp).clip(RoundedCornerShape(50)).background(Tk.Surface2).border(1.dp, if (focused) Tk.Lime else Tk.Line, RoundedCornerShape(50)).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (tab.incognito) Icons.Rounded.VisibilityOff else if (tab.isHome) Icons.Rounded.Search else if (UrlInput.isSecure(tab.url)) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                null, tint = if (!tab.isHome && !UrlInput.isSecure(tab.url)) Color(0xFFFFB020) else Tk.Lime, modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(8.dp))
            Box(Modifier.weight(1f)) {
                if (shown.isEmpty()) Text(tr("Search ya address likho", "Search or type address"), color = Tk.Dim, fontSize = 14.sp, maxLines = 1)
                BasicTextField(
                    value = if (focused) tfv else androidx.compose.ui.text.input.TextFieldValue(shown), onValueChange = { tfv = it },
                    singleLine = true, textStyle = TextStyle(color = Tk.White, fontSize = 14.sp),
                    cursorBrush = SolidColor(Tk.Lime),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go, keyboardType = KeyboardType.Uri),
                    keyboardActions = KeyboardActions(onGo = { val typed = tfv.text; focus.clearFocus(); ctl.go(tab, typed) }),
                    modifier = Modifier.fillMaxWidth().testTag("browser_address").onFocusChanged { f ->
                        focused = f.isFocused
                        // like every browser: tapping the bar selects the whole address so typing replaces it
                        if (f.isFocused) { val u = if (tab.isHome) "" else tab.url; tfv = androidx.compose.ui.text.input.TextFieldValue(u, androidx.compose.ui.text.TextRange(0, u.length)) }
                    },
                )
            }
            if (focused && text.isNotEmpty()) Icon(Icons.Rounded.Close, tr("Saaf karo", "Clear"), tint = Tk.Muted, modifier = Modifier.size(18.dp).clickable { tfv = androidx.compose.ui.text.input.TextFieldValue("") })
        }
        IconButton(onClick = { if (tab.isHome) {} else ctl.reload(tab) }, enabled = !tab.isHome) {
            Icon(if (tab.loading) Icons.Rounded.Close else Icons.Rounded.Refresh, tr("Reload", "Reload"), tint = if (tab.isHome) Tk.Dim else Tk.White)
        }
    }
}

@Composable
private fun NavBar(ctl: BrowserController, tab: BTab, open: (Panel) -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().background(if (tab.incognito) Color(0xFF15151A) else Tk.Surface).height(46.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
        IconButton(onClick = { ctl.back(tab) }, enabled = !tab.isHome) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, tr("Peeche", "Back"), tint = if (tab.isHome) Tk.Dim else Tk.White) }
        IconButton(onClick = { ctl.forward(tab) }, enabled = tab.canForward) { Icon(Icons.AutoMirrored.Rounded.ArrowForward, tr("Aage", "Forward"), tint = if (tab.canForward) Tk.White else Tk.Dim) }
        IconButton(onClick = { ctl.goHome(tab) }, enabled = !tab.isHome) { Icon(Icons.Rounded.Home, tr("Browser home", "Browser home"), tint = if (tab.isHome) Tk.Dim else Tk.White) }
        IconButton(onClick = { ctl.toggleBookmark(tab) }, enabled = !tab.isHome) {
            val b = ctl.isBookmarked(tab)
            Icon(if (b) Icons.Rounded.Star else Icons.Rounded.StarBorder, tr("Bookmark", "Bookmark"), tint = if (b) Tk.Lime else if (tab.isHome) Tk.Dim else Tk.White)
        }
        // tab switcher with count
        Box(Modifier.size(40.dp).clickable { open(Panel.Tabs) }.testTag("browser_tabs_btn"), contentAlignment = Alignment.Center) {
            Box(Modifier.size(22.dp).border(1.8.dp, Tk.White, RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
                Text("${ctl.tabs.size}", color = Tk.White, fontSize = 11.sp, maxLines = 1)
            }
        }
        Box {
            IconButton(onClick = { menu = true }, modifier = Modifier.testTag("browser_menu_btn")) { Icon(Icons.Rounded.MoreVert, tr("Menu", "Menu"), tint = Tk.White) }
            BrowserMenu(ctl, tab, menu, { menu = false }, open)
        }
    }
}

@Composable
private fun BrowserMenu(ctl: BrowserController, tab: BTab, show: Boolean, dismiss: () -> Unit, open: (Panel) -> Unit) {
    @Composable fun item(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, check: Boolean? = null, enabled: Boolean = true, onClick: () -> Unit) {
        DropdownMenuItem(
            text = { Text(label, color = if (enabled) Tk.White else Tk.Dim) },
            leadingIcon = { Icon(icon, null, tint = if (enabled) Tk.Lime else Tk.Dim) },
            trailingIcon = if (check == null) null else ({ Icon(if (check) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank, null, tint = if (check) Tk.Lime else Tk.Muted) }),
            enabled = enabled, onClick = { dismiss(); onClick() },
        )
    }
    DropdownMenu(show, dismiss, containerColor = Tk.Sheet) {
        item(Icons.Rounded.Add, tr("Naya tab", "New tab")) { ctl.newTab() }
        item(Icons.Rounded.VisibilityOff, tr("Incognito tab", "Incognito tab")) { ctl.newTab(incognito = true) }
        item(Icons.Rounded.Star, tr("Bookmarks", "Bookmarks")) { open(Panel.Bookmarks) }
        item(Icons.Rounded.History, tr("History", "History")) { open(Panel.History) }
        item(Icons.Rounded.Download, tr("Downloads", "Downloads")) { open(Panel.Downloads) }
        HorizontalDivider(color = Tk.Line)
        item(Icons.Rounded.FindInPage, tr("Page me dhundho", "Find in page"), enabled = !tab.isHome) { ctl.openFind() }
        item(Icons.Rounded.DesktopWindows, tr("Desktop site", "Desktop site"), check = tab.desktop, enabled = !tab.isHome) { ctl.setDesktop(tab, !tab.desktop) }
        item(Icons.Rounded.DarkMode, tr("Dark mode", "Dark mode"), check = ctl.dark) { ctl.chooseDark(!ctl.dark) }
        item(Icons.Rounded.Shield, tr("Ad block", "Block ads"), check = ctl.adBlock) { ctl.chooseAdBlock(!ctl.adBlock) }
        item(Icons.Rounded.Search, tr("Search: ", "Search: ") + ctl.engine.label + tr(" (badlo)", " (change)")) { ctl.chooseEngine(if (ctl.engine == SearchEngine.GOOGLE) SearchEngine.DUCKDUCKGO else SearchEngine.GOOGLE) }
        HorizontalDivider(color = Tk.Line)
        item(Icons.Rounded.Share, tr("Share karo", "Share"), enabled = !tab.isHome) { ctl.share(tab) }
        item(Icons.AutoMirrored.Rounded.OpenInNew, tr("Dusre browser me kholo", "Open in other browser"), enabled = !tab.isHome) { ctl.openExternal(tab) }
        item(Icons.Rounded.DeleteSweep, tr("Browsing data saaf karo", "Clear browsing data")) { ctl.clearData() }
    }
}

@Composable
private fun FindBar(ctl: BrowserController) {
    val fr = remember { FocusRequester() }
    LaunchedEffect(Unit) { fr.requestFocus() }
    Row(Modifier.fillMaxWidth().background(Tk.Surface).height(50.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(50)).background(Tk.Surface2).padding(horizontal = 14.dp), contentAlignment = Alignment.CenterStart) {
            if (ctl.findQuery.isEmpty()) Text(tr("Page me dhundho", "Find in page"), color = Tk.Dim, fontSize = 14.sp)
            BasicTextField(
                ctl.findQuery, { ctl.find(it) }, singleLine = true, textStyle = TextStyle(color = Tk.White, fontSize = 14.sp), cursorBrush = SolidColor(Tk.Lime),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { ctl.findNext(true) }),
                modifier = Modifier.fillMaxWidth().focusRequester(fr).testTag("browser_find_field"),
            )
        }
        Text(if (ctl.findQuery.isBlank()) "" else "${ctl.findIndex}/${ctl.findCount}", color = Tk.Muted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 6.dp))
        IconButton(onClick = { ctl.findNext(false) }) { Icon(Icons.Rounded.KeyboardArrowUp, "Prev", tint = Tk.White) }
        IconButton(onClick = { ctl.findNext(true) }) { Icon(Icons.Rounded.KeyboardArrowDown, "Next", tint = Tk.White) }
        IconButton(onClick = { ctl.closeFind() }) { Icon(Icons.Rounded.Close, tr("Band", "Close"), tint = Tk.White) }
    }
}

@Composable
private fun StartPage(ctl: BrowserController, tab: BTab) {
    val quick = remember(ctl.bookmarks) {
        val defaults = listOf("Tech Kadak" to "https://techkadak.github.io", "GitHub" to "https://github.com", "F-Droid" to "https://f-droid.org", "Wikipedia" to "https://www.wikipedia.org")
        (ctl.bookmarks.map { it.title to it.url } + defaults).distinctBy { it.second }.take(8)
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(18.dp))
        Box(Modifier.size(64.dp).clip(RoundedCornerShape(18.dp)).background(Brush.verticalGradient(listOf(Tk.LimeLight, Tk.Lime, Tk.LimeDeep))), contentAlignment = Alignment.Center) {
            Icon(if (tab.incognito) Icons.Rounded.VisibilityOff else Icons.Rounded.Public, null, tint = Tk.OnLime, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text(if (tab.incognito) tr("Incognito tab", "Incognito tab") else "Tech Kadak Browser", color = Tk.White, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(6.dp))
        Text(
            if (tab.incognito) tr("Is tab ki history, form aur cache save nahi hote. Band karne pe cookies bhi hat jaati hain.", "No history, forms or cache are saved in this tab. Its cookies go when you close it.")
            else tr("Upar search karo ya address likho. ${ctl.engine.label} se search hoga.", "Search or type an address above. Searches use ${ctl.engine.label}."),
            color = Tk.Muted, style = MaterialTheme.typography.bodyMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(22.dp))
        LazyVerticalGrid(GridCells.Fixed(4), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            gridItems(quick, key = { it.second }) { (title, url) ->
                Column(Modifier.clip(RoundedCornerShape(14.dp)).clickable { ctl.go(tab, url) }.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(52.dp).clip(CircleShape).background(Tk.Surface2).border(1.dp, Tk.Line, CircleShape), contentAlignment = Alignment.Center) {
                        Text(title.firstOrNull()?.uppercase() ?: "•", color = Tk.Lime, fontSize = 20.sp, fontFamily = Display)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(title, color = Tk.White, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun PanelFrame(title: String, onClose: () -> Unit, actions: @Composable RowScope.() -> Unit = {}, body: @Composable ColumnScope.() -> Unit) {
    BackHandler(onBack = onClose)
    Column(Modifier.fillMaxSize().background(Tk.Black).clickable(enabled = false) {}) {
        Row(Modifier.fillMaxWidth().background(Tk.Surface).padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, tr("Wapas", "Back"), tint = Tk.White) }
            Text(title, Modifier.weight(1f), color = Tk.White, style = MaterialTheme.typography.titleLarge)
            actions()
        }
        body()
    }
}

@Composable
private fun TabsPanel(ctl: BrowserController, onClose: () -> Unit) {
    PanelFrame(tr("Tabs (${ctl.tabs.size})", "Tabs (${ctl.tabs.size})"), onClose, actions = {
        if (ctl.incognitoCount > 0) TextButton(onClick = { ctl.closeAllIncognito(); onClose() }) { Text(tr("Incognito band", "Close incognito"), color = Tk.Lime) }
        IconButton(onClick = { ctl.newTab(); onClose() }, modifier = Modifier.testTag("browser_new_tab")) { Icon(Icons.Rounded.Add, tr("Naya tab", "New tab"), tint = Tk.Lime) }
        IconButton(onClick = { ctl.newTab(incognito = true); onClose() }) { Icon(Icons.Rounded.VisibilityOff, "Incognito", tint = Tk.White) }
    }) {
        LazyVerticalGrid(GridCells.Fixed(2), contentPadding = PaddingValues(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
            gridItems(ctl.tabs.toList(), key = { it.id }) { t ->
                val sel = t.id == ctl.currentId
                Column(
                    Modifier.clip(RoundedCornerShape(16.dp)).background(if (t.incognito) Color(0xFF15151A) else Tk.Surface)
                        .border(BorderStroke(if (sel) 2.dp else 1.dp, if (sel) Tk.Lime else Tk.Line), RoundedCornerShape(16.dp))
                        .clickable { ctl.select(t); onClose() }.padding(12.dp).height(92.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (t.incognito) { Icon(Icons.Rounded.VisibilityOff, null, tint = Tk.Muted, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)) }
                        Text(if (t.isHome) tr("Naya tab", "New tab") else t.title.ifBlank { UrlInput.hostOf(t.url) }, Modifier.weight(1f), color = Tk.White, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Icon(Icons.Rounded.Close, tr("Tab band karo", "Close tab"), tint = Tk.Muted, modifier = Modifier.size(22.dp).clickable { ctl.closeTab(t) })
                    }
                    Spacer(Modifier.weight(1f))
                    Text(if (t.isHome) "" else UrlInput.display(t.url), color = Tk.Muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

private val timeFmt = SimpleDateFormat("dd MMM, hh:mm a", Locale.ENGLISH)

@Composable
private fun HistoryPanel(ctl: BrowserController, onClose: () -> Unit, onOpen: (String) -> Unit) {
    var q by remember { mutableStateOf("") }
    val list = remember(ctl.history, q) { BrowserLists.searchHistory(ctl.history, q) }
    PanelFrame(tr("History", "History"), onClose = onClose, actions = {
        if (ctl.history.isNotEmpty()) TextButton(onClick = { ctl.clearHistory() }) { Text(tr("Saaf karo", "Clear all"), color = Tk.Lime) }
    }) {
        SearchField(q, { q = it }, tr("History me dhundho", "Search history"))
        if (list.isEmpty()) EmptyNote(if (ctl.history.isEmpty()) tr("Abhi history khali hai", "No history yet") else tr("Kuch nahi mila", "Nothing found"))
        LazyColumn(Modifier.fillMaxSize()) {
            items(list, key = { "${it.at}|${it.url}" }) { e ->
                Row(Modifier.fillMaxWidth().clickable { onOpen(e.url) }.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(e.title, color = Tk.White, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(UrlInput.display(e.url) + "  •  " + timeFmt.format(Date(e.at)), color = Tk.Muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = { ctl.removeHistory(e) }) { Icon(Icons.Rounded.Close, tr("Hatao", "Remove"), tint = Tk.Muted) }
                }
            }
        }
    }
}

@Composable
private fun BookmarksPanel(ctl: BrowserController, onClose: () -> Unit, onOpen: (String) -> Unit) {
    PanelFrame(tr("Bookmarks", "Bookmarks"), onClose = onClose) {
        if (ctl.bookmarks.isEmpty()) EmptyNote(tr("Koi bookmark nahi. Page pe ⭐ dabao", "No bookmarks yet. Tap ⭐ on a page"))
        LazyColumn(Modifier.fillMaxSize()) {
            items(ctl.bookmarks, key = { it.url }) { b ->
                Row(Modifier.fillMaxWidth().clickable { onOpen(b.url) }.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Star, null, tint = Tk.Lime, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(b.title, color = Tk.White, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(UrlInput.display(b.url), color = Tk.Muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = { ctl.removeBookmark(b) }) { Icon(Icons.Rounded.Delete, tr("Hatao", "Remove"), tint = Tk.Muted) }
                }
            }
        }
    }
}

@Composable
private fun DownloadsPanel(ctl: BrowserController, onClose: () -> Unit) {
    var rows by remember { mutableStateOf(emptyList<BrowserController.DlStatus>()) }
    LaunchedEffect(ctl.downloads) {
        while (true) {
            rows = withContext(Dispatchers.IO) { ctl.queryDownloads() }
            delay(if (rows.any { it.state == DownloadManager.STATUS_RUNNING || it.state == DownloadManager.STATUS_PENDING }) 700 else 2500)
        }
    }
    PanelFrame(tr("Downloads", "Downloads"), onClose) {
        if (rows.isEmpty()) EmptyNote(tr("Abhi koi download nahi", "No downloads yet"))
        LazyColumn(Modifier.fillMaxSize().testTag("browser_downloads_list")) {
            items(rows, key = { it.rec.id }) { d ->
                val ok = d.state == DownloadManager.STATUS_SUCCESSFUL
                val running = d.state == DownloadManager.STATUS_RUNNING || d.state == DownloadManager.STATUS_PENDING
                Column(Modifier.fillMaxWidth().clickable(enabled = ok) { ctl.openDownload(d) }.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (ok) Icons.Rounded.CheckCircle else if (running) Icons.Rounded.Download else Icons.Rounded.ErrorOutline, null, tint = if (ok) Tk.Lime else if (running) Tk.White else Color(0xFFFF6B6B), modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(d.rec.name, color = Tk.White, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                when {
                                    ok -> tr("Ho gaya • tap karke kholo • ", "Done • tap to open • ") + fmtSize(d.total.takeIf { it > 0 } ?: d.done)
                                    running -> fmtSize(d.done) + (if (d.total > 0) " / " + fmtSize(d.total) else "")
                                    d.state == -1 -> tr("Hata diya gaya", "Removed")
                                    else -> tr("Fail ho gaya", "Failed")
                                }, color = Tk.Muted, fontSize = 11.sp, maxLines = 1,
                            )
                        }
                        IconButton(onClick = { ctl.removeDownload(d.rec) }) { Icon(Icons.Rounded.Delete, if (running) tr("Cancel", "Cancel") else tr("Hatao", "Delete"), tint = Tk.Muted) }
                    }
                    if (running && d.total > 0) LinearProgressIndicator({ (d.done.toFloat() / d.total).coerceIn(0f, 1f) }, Modifier.fillMaxWidth().padding(top = 6.dp).height(3.dp), color = Tk.Lime, trackColor = Tk.Surface2)
                }
            }
        }
    }
}

private fun fmtSize(b: Long): String = when {
    b >= 1024L * 1024 -> "%.1f MB".format(b / 1048576.0)
    b >= 1024 -> "%d KB".format(b / 1024)
    else -> "$b B"
}

@Composable
private fun SearchField(q: String, onChange: (String) -> Unit, hint: String) {
    Box(Modifier.fillMaxWidth().padding(12.dp).height(42.dp).clip(RoundedCornerShape(50)).background(Tk.Surface2).padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
        if (q.isEmpty()) Text(hint, color = Tk.Dim, fontSize = 14.sp)
        BasicTextField(q, onChange, singleLine = true, textStyle = TextStyle(color = Tk.White, fontSize = 14.sp), cursorBrush = SolidColor(Tk.Lime), modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun EmptyNote(t: String) {
    Text(t, Modifier.fillMaxWidth().padding(32.dp), color = Tk.Muted, style = MaterialTheme.typography.bodyMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
}
