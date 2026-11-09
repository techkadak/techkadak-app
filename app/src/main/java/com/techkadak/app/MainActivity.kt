package com.techkadak.app

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.toArgb
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.techkadak.app.data.AppItem
import com.techkadak.app.notify.Notifier
import com.techkadak.app.ui.Host
import com.techkadak.app.ui.MainViewModel
import com.techkadak.app.ui.TechKadakApp
import com.techkadak.app.ui.Tk
import com.techkadak.app.ui.TkThemed
import com.techkadak.app.ui.tr

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()
    private val openSlug = mutableStateOf<String?>(null)
    private val askNotif = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        // v1.2.1 anti-tamper: modified / re-signed copy -> sirf warning screen, koi feature nahi
        if (!Integrity.ok(this)) {
            setContent { TamperScreen(onOpen = { openLink(Config.SITE_BASE) }) }
            return
        }
        // DataStore settings padhne tak splash rakho (bas kuch ms) — taaki galat theme flash na ho.
        splash.setKeepOnScreenCondition { vm.settings == null }
        openSlug.value = intent?.getStringExtra(Notifier.EXTRA_SLUG)
        if (intent?.getBooleanExtra(Notifier.EXTRA_UPDATE, false) == true) vm.remindNow()
        handleDownloadIntent(intent)
        val sp = getSharedPreferences("tk", MODE_PRIVATE)
        if (Build.VERSION.SDK_INT >= 33 && !sp.getBoolean("asked_notif", false)) {
            sp.edit().putBoolean("asked_notif", true).apply()
            runCatching { askNotif.launch(Manifest.permission.POST_NOTIFICATIONS) }
        }
        val host = Host(
            onLink = ::openLink, onShare = ::share, onShareApp = ::shareApp, onOpenMail = ::openMail,
            onGoogle = { vm.googleSignIn(this) },
            onSendOtp = { num, resend -> vm.sendOtp(this, num, resend) },
            onMarket = ::openMarket,
            onAskNotif = ::askNotifOnce,
            onSharePoster = { a, f, b -> sharePoster(a, f, b) },
        )
        setContent {
            TkThemed(vm) {
                // Status / nav bar theme ke saath (light theme me dark icons)
                val bg = Tk.Black.toArgb(); val nav = Tk.Surface.toArgb(); val light = Tk.isLight
                SideEffect {
                    window.statusBarColor = bg
                    window.navigationBarColor = nav
                    WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = light; isAppearanceLightNavigationBars = light
                    }
                }
                TechKadakApp(vm, openSlug.value, host)
            }
        }
        // v1.2.1: pichli baar crash hua tha to report pop-up ("Report copy karo")
        runCatching { CrashLog.showIfAny(this) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (!tamperOk) return
        intent.getStringExtra(Notifier.EXTRA_SLUG)?.let { openSlug.value = it }
        if (intent.getBooleanExtra(Notifier.EXTRA_UPDATE, false)) vm.remindNow()
        handleDownloadIntent(intent)
    }

    private val tamperOk by lazy { Integrity.ok(this) }

    override fun onStart() {
        super.onStart()
        if (!tamperOk) return
        vm.onAppForeground() // 30+ min baad wapas = naya open -> update pop-up phir
    }

    override fun onStop() {
        super.onStop()
        if (!tamperOk) return
        vm.onAppBackground()
    }

    override fun onResume() {
        super.onResume()
        if (!tamperOk) return
        vm.refreshInstalled() // install ke baad wapas aaye -> "Open" dikhe
    }

    private fun handleDownloadIntent(i: Intent?) {
        if (i == null) return
        if (i.getBooleanExtra(com.techkadak.app.install.ApkDownloads.EXTRA_DOWNLOADS, false)) vm.openDownloads()
        i.getStringExtra(com.techkadak.app.install.ApkDownloads.EXTRA_INSTALL)?.let { vm.installDl(it) }
    }

    private fun askNotifOnce() {
        if (Build.VERSION.SDK_INT >= 33 && androidx.core.content.ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            runCatching { askNotif.launch(Manifest.permission.POST_NOTIFICATIONS) }
        }
    }

    /** Play-Store-only app: market:// (Play app me seedha), warna https Play page. */
    private fun openMarket(pkg: String?, url: String?) {
        if (pkg != null) {
            try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); return } catch (_: ActivityNotFoundException) {}
        }
        openLink(url ?: pkg?.let { "https://play.google.com/store/apps/details?id=$it" } ?: return)
    }

    private fun openLink(url: String) {
        if (!url.startsWith("https://")) return
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, tr("Link kholne ke liye browser chahiye", "A browser is needed to open this link"), Toast.LENGTH_SHORT).show()
        }
    }

    private fun share(a: AppItem) {
        val text = "${a.name} — ${a.what}\n\nTech Kadak pe dekho: ${a.pageUrl(Config.SITE_BASE)}"
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), tr("Share karo", "Share")))
    }

    private fun sharePoster(a: AppItem, f: com.techkadak.app.ui.PosterFormat, b: android.graphics.Bitmap) {
        runCatching {
            val file = com.techkadak.app.ui.Poster.save(this, a, f, b)
            startActivity(com.techkadak.app.ui.Poster.shareIntent(this, a, file))
        }.onFailure { Toast.makeText(this, tr("Poster share nahi hua", "Couldn't share the poster"), Toast.LENGTH_SHORT).show() }
    }

    private fun shareApp() {
        val i = Intent(Intent.ACTION_SEND).setType("text/plain")
            .putExtra(Intent.EXTRA_SUBJECT, "Tech Kadak app")
            .putExtra(Intent.EXTRA_TEXT, Config.SHARE_APP_TEXT)
        startActivity(Intent.createChooser(i, tr("Tech Kadak share karo", "Share Tech Kadak")))
    }

    private fun openMail() {
        try {
            startActivity(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_EMAIL).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, tr("Phone me mail app nahi mila", "No mail app found"), Toast.LENGTH_SHORT).show()
        }
    }
}

/** Modified / fake copy ke liye full-screen warning. Asli app ka link hi kholta hai. */
@androidx.compose.runtime.Composable
private fun TamperScreen(onOpen: () -> Unit) {
    val lime = androidx.compose.ui.graphics.Color(0xFFC6FF00)
    androidx.compose.foundation.layout.Box(
        androidx.compose.ui.Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black).padding(28.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        androidx.compose.foundation.layout.Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
            androidx.compose.material3.Text("⚠️", fontSize = 56.sp)
            androidx.compose.foundation.layout.Spacer(androidx.compose.ui.Modifier.height(16.dp))
            androidx.compose.material3.Text(
                "Ye Tech Kadak ka modified/fake version hai.",
                color = androidx.compose.ui.graphics.Color.White, fontSize = 22.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            androidx.compose.foundation.layout.Spacer(androidx.compose.ui.Modifier.height(10.dp))
            androidx.compose.material3.Text(
                "Asli app sirf techkadak.github.io se download karo.",
                color = androidx.compose.ui.graphics.Color(0xFFCCCCCC), fontSize = 16.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            androidx.compose.foundation.layout.Spacer(androidx.compose.ui.Modifier.height(28.dp))
            androidx.compose.material3.Button(
                onClick = onOpen,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = lime, contentColor = androidx.compose.ui.graphics.Color.Black),
            ) { androidx.compose.material3.Text("Asli app download karo", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) }
        }
    }
}
