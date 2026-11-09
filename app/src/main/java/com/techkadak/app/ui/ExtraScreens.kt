package com.techkadak.app.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.techkadak.app.BuildConfig
import com.techkadak.app.Config
import com.techkadak.app.R
import kotlinx.coroutines.launch

/* ─────────────── ABOUT (ROHIT ka diya hua copy, as-is) ─────────────── */

object AboutCopy {
    const val DEVELOPER = "Rohit Saini"
    const val TAGLINE = "Tech researcher · Creator of Tech Kadak"
    const val BIO = "Main Rohit Saini hoon — technology ka researcher aur Tech Kadak ka banane wala. Mera kaam hai roz naye apps, AI tools aur open-source projects ko khud test karna, aur sirf wahi aap tak laana jo sach me kaam ka ho. Paid apps ke free jawab, privacy-friendly tools aur students ke kaam ki cheezein — sab ek jagah, simple Hinglish me. Har app ka link official source se hi milta hai, taaki aap safe rahein. 💚"
    const val MISSION = "Mission: har Indian tak sahi tech, bina faltu kharche ke."
}

@Composable
fun AboutScreen(
    appCount: Int, onBack: () -> Unit, onLink: (String) -> Unit, onWeb: (String) -> Unit, onShareApp: () -> Unit,
    onLicenses: () -> Unit = {}, updateCard: @Composable () -> Unit = {},
    installed: Pair<String, Long> = BuildConfig.VERSION_NAME to BuildConfig.VERSION_CODE.toLong(),
    updateAvailable: String? = null,
    history: List<com.techkadak.app.update.ReleaseInfo> = emptyList(),
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenTitle(tr("About", "About"), null, onBack)
        // App header
        Column(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Image(painterResource(R.drawable.tk_logo), null, Modifier.size(92.dp).clip(RoundedCornerShape(26.dp)).border(2.dp, Tk.Lime, RoundedCornerShape(26.dp)), contentScale = ContentScale.Crop)
            Spacer(Modifier.height(12.dp))
            Text("Tech Kadak", color = Tk.White, fontFamily = Display, fontWeight = FontWeight.Black, fontSize = 26.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Version ${installed.first} (${installed.second})", color = Tk.Muted, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (updateAvailable != null) tr("Update available: v$updateAvailable", "Update available: v$updateAvailable") else tr("Up to date ✅", "Up to date ✅"),
                    Modifier.clip(RoundedCornerShape(50)).background(if (updateAvailable != null) Tk.Lime else Tk.LimeSoft).padding(horizontal = 9.dp, vertical = 3.dp),
                    color = if (updateAvailable != null) Tk.OnLime else Tk.Lime, style = MaterialTheme.typography.labelSmall,
                )
            }
            Text(tr("Roz ek kadak free app 🔥 • $appCount apps", "One kadak free app a day 🔥 • $appCount apps"), color = Tk.Lime, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 2.dp))
        }
        // Developer card
        Column(
            Modifier.padding(horizontal = 18.dp, vertical = 6.dp).fillMaxWidth().clip(RoundedCornerShape(26.dp))
                .background(Brush.verticalGradient(listOf(Tk.HeroStart, Tk.Surface))).border(1.dp, Tk.LimeBorder, RoundedCornerShape(26.dp)).padding(18.dp),
        ) {
            Text(tr("DEVELOPER", "DEVELOPER"), color = Tk.Lime, fontFamily = Display, fontWeight = FontWeight.Black, fontSize = 10.sp, letterSpacing = 2.sp)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.tk_logo), null, Modifier.size(58.dp).clip(CircleShape).border(2.dp, Tk.Lime, CircleShape), contentScale = ContentScale.Crop)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(AboutCopy.DEVELOPER, color = Tk.White, style = MaterialTheme.typography.headlineSmall)
                    Text(AboutCopy.TAGLINE, color = Tk.Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(AboutCopy.BIO, color = Tk.TextSoft, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            Text(
                AboutCopy.MISSION, Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Tk.LimeSoft).padding(12.dp),
                color = Tk.Lime, style = MaterialTheme.typography.titleSmall,
            )
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                LinkPill(Icons.Rounded.PhotoCamera, "Instagram") { onLink(Config.INSTAGRAM) }
                LinkPill(Icons.Rounded.SmartDisplay, "YouTube") { onLink(Config.YOUTUBE) }
                LinkPill(Icons.AutoMirrored.Rounded.Send, "Telegram") { onLink(Config.TELEGRAM) }
                LinkPill(Icons.Rounded.Language, "Website") { onWeb(Config.SITE_BASE) }
            }
        }
        Row(Modifier.padding(horizontal = 18.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LimeButton(tr("SHARE APP", "SHARE APP"), Modifier.weight(1f), Icons.Rounded.Share, onShareApp)
        }
        SupportCard(Modifier.padding(horizontal = 18.dp, vertical = 6.dp))
        ReleaseQrCard(Modifier.padding(horizontal = 18.dp, vertical = 6.dp))
        updateCard()
        Text(tr("Version history • Kya naya hai", "Version history • What's new"), Modifier.padding(start = 20.dp, top = 16.dp, bottom = 6.dp), color = Tk.White, style = MaterialTheme.typography.titleLarge)
        if (history.isEmpty()) {
            Text(tr("v${installed.first} — abhi wala version. Naye versions yahan dikhenge.", "v${installed.first} — current version. New versions will show up here."),
                Modifier.padding(horizontal = 20.dp), color = Tk.Muted, style = MaterialTheme.typography.bodySmall)
        }
        history.take(8).forEach { h ->
            val current = h.version == installed.first
            Column(Modifier.padding(horizontal = 18.dp, vertical = 4.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Tk.Surface)
                .border(1.dp, if (current) Tk.LimeBorder else Tk.Line, RoundedCornerShape(16.dp)).clickable { if (h.url.startsWith("https://")) onLink(h.url) }.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("v${h.version}", color = Tk.White, fontFamily = Display, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    Spacer(Modifier.width(8.dp))
                    if (current) Text(tr("TUMHARA", "INSTALLED"), Modifier.clip(RoundedCornerShape(50)).background(Tk.Lime).padding(horizontal = 7.dp, vertical = 2.dp), color = Tk.OnLime, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Text(com.techkadak.app.data.Dates.pretty(h.date), color = Tk.Dim, style = MaterialTheme.typography.bodySmall)
                }
                if (h.notes.isNotBlank()) Text(h.notes.take(400), Modifier.padding(top = 6.dp), color = Tk.Muted, style = MaterialTheme.typography.bodySmall, maxLines = 8)
            }
        }
        AboutRow(Icons.Rounded.Shield, tr("Privacy", "Privacy"),
            tr("Hum aapka data nahi bechte. Login optional hai. Favourites aapke phone pe save hote hain; login karne pe account Firebase (Google) pe banta hai — kabhi bhi delete kar sakte ho. App anonymous usage ginti bhejta hai (app khola, kaunsa app install hua) — naam, email ya phone kabhi nahi; Settings → \"Usage stats bhejo\" se band karo.",
                "We don't sell your data. Login is optional. Favourites stay on your phone; if you log in, your account lives on Firebase (Google) and you can delete it any time. The app sends anonymous usage counts (opens, which apps get installed) — never your name, email or phone; turn it off in Settings → \"Send usage stats\"."))
        AboutRow(Icons.Rounded.VerifiedUser, tr("Hamara promise", "Our promise"),
            tr("Sirf official download links. Kisi app ki APK host ya modify nahi karte. Koi fake rating ya download count nahi.",
                "Official download links only. We never host or modify APKs. No fake ratings or download counts."))
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onLicenses).padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Description, null, tint = Tk.Lime)
            Spacer(Modifier.width(14.dp))
            Text(tr("Open-source licenses", "Open-source licenses"), Modifier.weight(1f), color = Tk.White, style = MaterialTheme.typography.titleSmall)
            Icon(Icons.Rounded.ChevronRight, null, tint = Tk.Dim)
        }
        Text("Made with 💚 in India", Modifier.fillMaxWidth().padding(top = 18.dp), color = Tk.White, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
        Text("© Tech Kadak • techkadak.github.io", Modifier.fillMaxWidth().padding(top = 4.dp), color = Tk.Dim, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        CreditLine(Modifier.fillMaxWidth().padding(bottom = 28.dp, top = 8.dp))
    }
}

@Composable
private fun LinkPill(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(Modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(Tk.Lime), contentAlignment = Alignment.Center) { Icon(icon, label, tint = Tk.OnLime) }
        Text(label, color = Tk.Muted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun AboutRow(icon: ImageVector, title: String, body: String) {
    Row(Modifier.padding(horizontal = 18.dp, vertical = 6.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Tk.Surface).border(1.dp, Tk.Line, RoundedCornerShape(18.dp)).padding(14.dp)) {
        Icon(icon, null, tint = Tk.Lime, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, color = Tk.White, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(3.dp))
            Text(body, color = Tk.Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** Open-source licenses (jo libraries app me lagi hain). */
@Composable
fun LicensesScreen(onBack: () -> Unit, onLink: (String) -> Unit) {
    val libs = listOf(
        Triple("Kotlin + kotlinx.coroutines", "Apache License 2.0", "https://github.com/JetBrains/kotlin"),
        Triple("AndroidX (Compose, Material 3, Activity, Lifecycle, Core, Splashscreen)", "Apache License 2.0", "https://developer.android.com/jetpack/androidx"),
        Triple("AndroidX DataStore", "Apache License 2.0", "https://developer.android.com/jetpack/androidx/releases/datastore"),
        Triple("AndroidX Glance (widget)", "Apache License 2.0", "https://developer.android.com/jetpack/androidx/releases/glance"),
        Triple("AndroidX WorkManager", "Apache License 2.0", "https://developer.android.com/jetpack/androidx/releases/work"),
        Triple("AndroidX Credentials", "Apache License 2.0", "https://developer.android.com/jetpack/androidx/releases/credentials"),
        Triple("Firebase Android SDK (Messaging, Remote Config, Auth)", "Apache License 2.0", "https://github.com/firebase/firebase-android-sdk"),
        Triple("Material Icons", "Apache License 2.0", "https://github.com/google/material-design-icons"),
        Triple("Unbounded font", "SIL Open Font License 1.1", "https://github.com/w3f/unbounded"),
        Triple("DM Sans font", "SIL Open Font License 1.1", "https://github.com/googlefonts/dm-fonts"),
    )
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenTitle(tr("Licenses", "Licenses"), tr("Is app me ye open-source cheezein lagi hain", "Open-source software used in this app"), onBack)
        libs.forEach { (name, lic, url) ->
            Column(Modifier.fillMaxWidth().clickable { onLink(url) }.padding(horizontal = 20.dp, vertical = 11.dp)) {
                Text(name, color = Tk.White, style = MaterialTheme.typography.titleSmall)
                Text(lic, color = Tk.Lime, style = MaterialTheme.typography.bodySmall)
            }
            HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = Tk.Line)
        }
        Text(tr("App ke apps ki jaankari techkadak.github.io se aati hai. Har app apne developer ka hai.", "App listings come from techkadak.github.io. Each app belongs to its developer."),
            Modifier.padding(20.dp), color = Tk.Dim, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SocialCard(icon: ImageVector, title: String, handle: String, sub: String, onClick: () -> Unit) {
    Row(
        Modifier.padding(horizontal = 18.dp, vertical = 5.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Tk.Surface)
            .border(1.dp, Tk.Line, RoundedCornerShape(18.dp)).pressable(onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(Tk.Lime), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Tk.OnLime) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = Tk.White, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(8.dp))
                Text(handle, color = Tk.Lime, style = MaterialTheme.typography.labelMedium, maxLines = 1)
            }
            Text(sub, color = Tk.Muted, style = MaterialTheme.typography.bodySmall)
        }
        Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, tint = Tk.Dim, modifier = Modifier.size(18.dp))
    }
}

/* ─────────────── IN-APP WEBVIEW (sirf official pages) ─────────────── */

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebScreen(url: String, onBack: () -> Unit, onExternal: (String) -> Unit) {
    var title by remember { mutableStateOf(Uri.parse(url).host ?: "") }
    var progress by remember { mutableFloatStateOf(0f) }
    var web by remember { mutableStateOf<WebView?>(null) }
    BackHandler { val w = web; if (w != null && w.canGoBack()) w.goBack() else onBack() }
    val allowedHost = remember { Uri.parse(Config.SITE_BASE).host }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().background(Tk.Surface).padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.Close, tr("Band karo", "Close"), tint = Tk.White) }
            Column(Modifier.weight(1f)) {
                Text(title, color = Tk.White, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Lock, null, tint = Tk.Lime, modifier = Modifier.size(11.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(Uri.parse(url).host ?: "", color = Tk.Muted, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                }
            }
            IconButton(onClick = { onExternal(web?.url ?: url) }) { Icon(Icons.AutoMirrored.Rounded.OpenInNew, tr("Browser me kholo", "Open in browser"), tint = Tk.White) }
        }
        if (progress in 0.01f..0.99f) LinearProgressIndicator({ progress }, Modifier.fillMaxWidth().height(2.dp), color = Tk.Lime, trackColor = Tk.Surface)
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    setBackgroundColor(Tk.Black.toArgb())
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                            val u = request.url
                            // Apni website andar; baaki (download links, Instagram…) bahar official app/browser me.
                            return if (u.scheme == "https" && u.host == allowedHost) false else { onExternal(u.toString()); true }
                        }
                        override fun onPageStarted(view: WebView, u: String?, favicon: Bitmap?) { progress = 0.1f }
                        override fun onPageFinished(view: WebView, u: String?) { progress = 1f; view.title?.takeIf { it.isNotBlank() }?.let { title = it } }
                    }
                    webChromeClient = object : android.webkit.WebChromeClient() {
                        override fun onProgressChanged(view: WebView, newProgress: Int) { progress = newProgress / 100f }
                    }
                    loadUrl(url)
                    web = this
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/* ─────────────── FORCE UPDATE (Remote Config min_version_code) ─────────────── */

@Composable
fun ForceUpdateDialog(minCode: Long, onUpdate: () -> Unit) {
    Dialog(onDismissRequest = {}, properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false)) {
        Column(
            Modifier.padding(22.dp).fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Tk.Surface).border(1.5.dp, Tk.Lime, RoundedCornerShape(28.dp)).padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(64.dp).clip(CircleShape).background(Tk.Lime), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.SystemUpdate, null, tint = Tk.OnLime, modifier = Modifier.size(32.dp)) }
            Spacer(Modifier.height(14.dp))
            Text(tr("Update zaroori hai", "Update required"), color = Tk.White, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                tr("Ye version (v${BuildConfig.VERSION_NAME}) ab purana ho gaya. Naya version install karo, phir app pehle jaisa chalega.",
                    "This version (v${BuildConfig.VERSION_NAME}) is too old. Install the new version to keep using the app."),
                color = Tk.Muted, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))
            LimeButton(tr("UPDATE KARO", "UPDATE NOW"), Modifier.fillMaxWidth(), Icons.Rounded.Download, onUpdate)
        }
    }
}
