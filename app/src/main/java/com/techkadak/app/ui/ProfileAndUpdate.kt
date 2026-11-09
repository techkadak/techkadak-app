package com.techkadak.app.ui

import androidx.compose.foundation.Image
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.ui.draw.drawWithContent
import kotlinx.coroutines.delay
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.techkadak.app.BuildConfig
import com.techkadak.app.Config
import com.techkadak.app.R
import com.techkadak.app.data.DataSource
import com.techkadak.app.update.DlState
import com.techkadak.app.update.Release

class ProfileActions(
    val onLogin: () -> Unit = {}, val onVerify: () -> Unit = {}, val onLogout: () -> Unit = {}, val onDelete: () -> Unit = {},
    val onSettings: () -> Unit = {}, val onAbout: () -> Unit = {}, val onShareApp: () -> Unit = {}, val onRequest: () -> Unit = {},
    val onFavs: () -> Unit = {}, val onNew: () -> Unit = {}, val onLink: (String) -> Unit = {},
)

@Composable
fun ProfileScreen(
    appCount: Int,
    source: DataSource?,
    user: com.techkadak.app.auth.TkUser?,
    authEnabled: Boolean,
    favCount: Int,
    act: ProfileActions,
    member: Triple<String, Long, String>? = null,
    onMemberReplay: () -> Unit = {},
    updateCard: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 8.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(tr("Profile", "Profile"), Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium, color = Tk.White)
            IconButton(onClick = act.onSettings) { Icon(Icons.Rounded.Settings, "Settings", tint = Tk.White) }
        }
        AccountCard(user, authEnabled, act.onLogin, act.onVerify, act.onLogout, act.onDelete)
        member?.let { ProfileMemberCard(it.first, it.second, it.third, onMemberReplay) }
        // Quick actions grid
        Column(Modifier.padding(horizontal = 14.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickTile(Icons.Rounded.Palette, tr("Customise", "Customise"), tr("Theme, colour, font", "Theme, colour, font"), Modifier.weight(1f), highlight = true, onClick = act.onSettings)
                QuickTile(Icons.Rounded.Favorite, tr("Meri Pasand", "Favourites"), "$favCount apps", Modifier.weight(1f), onClick = act.onFavs)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickTile(Icons.Rounded.AddCircle, tr("App request karo", "Request an app"), tr("Telegram pe batao", "Tell us on Telegram"), Modifier.weight(1f), onClick = act.onRequest)
                QuickTile(Icons.Rounded.Share, tr("App share karo", "Share app"), tr("Doston ko bhejo", "Send to friends"), Modifier.weight(1f), onClick = act.onShareApp)
            }
        }
        Text(tr("Follow karo", "Follow"), Modifier.padding(start = 20.dp, top = 14.dp, bottom = 4.dp), color = Tk.White, style = MaterialTheme.typography.titleLarge)
        LinkRow(Icons.Rounded.PhotoCamera, "Instagram", tr("@techkadak — roz naye apps", "@techkadak — new apps daily")) { act.onLink(Config.INSTAGRAM) }
        LinkRow(Icons.Rounded.SmartDisplay, "YouTube", "Tech Kadak channel") { act.onLink(Config.YOUTUBE) }
        LinkRow(Icons.AutoMirrored.Rounded.Send, "Telegram", "t.me/techkadak") { act.onLink(Config.TELEGRAM) }
        LinkRow(Icons.Rounded.Info, tr("About Tech Kadak", "About Tech Kadak"), tr("Website, promise, version", "Website, promise, version"), onClick = act.onAbout)
        LinkRow(Icons.Rounded.NewReleases, tr("Naye apps (din wise)", "New apps (by day)"), tr("Bot ne kab kya daala", "What the bot added, by day"), onClick = act.onNew)

        Text(tr("App update", "App update"), Modifier.padding(start = 20.dp, top = 18.dp, bottom = 4.dp), color = Tk.White, style = MaterialTheme.typography.titleLarge)
        updateCard()

        Text(
            "Version ${BuildConfig.VERSION_NAME} • Data: ${when (source) { DataSource.LIVE -> "live website"; DataSource.CACHE -> tr("saved copy", "saved copy"); DataSource.BUNDLED -> tr("app ke andar wali copy", "built-in copy"); null -> "—" }}\n" +
                tr("Tech Kadak kisi bhi app ki APK host nahi karta — sab links official hain.", "Tech Kadak never hosts APKs — all links are official."),
            Modifier.fillMaxWidth().padding(24.dp), color = Tk.Dim, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun QuickTile(icon: ImageVector, title: String, sub: String, modifier: Modifier, highlight: Boolean = false, onClick: () -> Unit) {
    Column(
        modifier.height(104.dp).clip(RoundedCornerShape(20.dp))
            .background(if (highlight) Brush.linearGradient(listOf(Tk.Lime, Tk.LimeDeep)) else Brush.linearGradient(listOf(Tk.Surface2, Tk.Surface)))
            .border(1.dp, if (highlight) Tk.Lime else Tk.Line, RoundedCornerShape(20.dp)).pressable(onClick).padding(14.dp),
    ) {
        Icon(icon, null, tint = if (highlight) Tk.OnLime else Tk.Lime, modifier = Modifier.size(24.dp))
        Spacer(Modifier.weight(1f))
        Text(title, color = if (highlight) Tk.OnLime else Tk.White, style = MaterialTheme.typography.titleSmall, maxLines = 1)
        Text(sub, color = if (highlight) Tk.OnLimeMuted else Tk.Muted, style = MaterialTheme.typography.bodySmall, maxLines = 1)
    }
}

@Composable
private fun LinkRow(icon: ImageVector, title: String, sub: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(Tk.Surface2), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Tk.Lime) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Tk.White, style = MaterialTheme.typography.titleSmall)
            Text(sub, color = Tk.Muted, style = MaterialTheme.typography.bodySmall)
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = Tk.Dim)
    }
}

@Composable
private fun ToggleRow(icon: ImageVector, title: String, sub: String, on: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(Tk.Surface2), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Tk.Lime) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Tk.White, style = MaterialTheme.typography.titleSmall)
            Text(sub, color = Tk.Muted, style = MaterialTheme.typography.bodySmall)
        }
        Switch(on, onChange, enabled = enabled, colors = SwitchDefaults.colors(checkedTrackColor = Tk.Lime, checkedThumbColor = Tk.OnLime, uncheckedTrackColor = Tk.Surface2, uncheckedThumbColor = Tk.Muted, uncheckedBorderColor = Tk.Line))
    }
}

@Composable
fun UpdateCard(checking: Boolean, msg: String?, available: Release?, onCheck: () -> Unit, onShow: () -> Unit) {
    Row(
        Modifier.padding(horizontal = 18.dp, vertical = 6.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Tk.Surface)
            .border(1.dp, if (available != null) Tk.Lime else Tk.Line, RoundedCornerShape(18.dp)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(if (available != null) Icons.Rounded.SystemUpdate else Icons.Rounded.CheckCircle, null, tint = Tk.Lime, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(if (available != null) tr("Naya update aaya: v", "Update available: v") + available.version else "Tech Kadak v${BuildConfig.VERSION_NAME}", color = Tk.White, style = MaterialTheme.typography.titleSmall)
            Text(msg ?: if (available != null) tr("Tap karke install karo", "Tap to install") else tr("GitHub se naya version khud check hota hai", "New versions are checked automatically"), color = Tk.Muted, style = MaterialTheme.typography.bodySmall)
        }
        if (checking) CircularProgressIndicator(Modifier.size(22.dp), color = Tk.Lime, strokeWidth = 2.dp)
        else Text(if (available != null) "UPDATE" else "CHECK", Modifier.clip(RoundedCornerShape(50)).background(Tk.Lime).clickable { if (available != null) onShow() else onCheck() }.padding(horizontal = 14.dp, vertical = 8.dp),
            color = Tk.OnLime, fontFamily = Display, fontWeight = FontWeight.Black, fontSize = 11.sp)
    }
}

/** v1.5: animated card-style update pop-up. Springs up over the dim scrim; cancellable every open ("Baad mein"); never full-screen. */
@Composable
fun UpdateDialog(r: Release, dl: DlState, canInstall: () -> Boolean, onDownload: () -> Unit, onInstall: () -> Unit, onPermission: () -> Unit, onLater: () -> Unit, onSkip: () -> Unit, force: Boolean = false) {
    var needPerm by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    // v1.5.1 "Mini update": on open the download starts by itself (Wi-Fi / unmetered only) with a progress bar, and the Android
    // installer opens when it is done (one Android "Update" tap is all that is left). Already-downloaded file = button only.
    val metered = remember { runCatching { (ctx.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager).isActiveNetworkMetered }.getOrDefault(true) }
    var autoInstalled by remember { mutableStateOf(dl is DlState.Done) }
    LaunchedEffect(Unit) { if (!metered && dl is DlState.Idle) onDownload() }
    LaunchedEffect(dl) {
        if (dl is DlState.Done && !autoInstalled) { autoInstalled = true; if (canInstall()) onInstall() else needPerm = true }
    }
    // Remove animations (system setting) = show the card at its end state at once.
    val noMotion = remember { android.provider.Settings.Global.getFloat(ctx.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
    val enter = remember { Animatable(if (noMotion) 1f else 0f) }
    val lines = remember(r.notes) { updateNoteLines(r.notes) }
    LaunchedEffect(Unit) {
        if (!noMotion) enter.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow))
    }
    Dialog(onDismissRequest = { if (!force) onLater() }, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = !force, dismissOnClickOutside = !force)) {
        Column(
            Modifier.padding(22.dp).fillMaxWidth()
                .graphicsLayer {
                    translationY = (1f - enter.value) * 260f
                    scaleX = 0.94f + 0.06f * enter.value; scaleY = 0.94f + 0.06f * enter.value
                    alpha = enter.value.coerceIn(0f, 1f)
                }
                .clip(RoundedCornerShape(28.dp)).background(Tk.Sheet).border(1.5.dp, Tk.Lime, RoundedCornerShape(28.dp)).padding(22.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).clip(CircleShape).background(Tk.Lime), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.RocketLaunch, null, tint = Tk.OnLime) }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(tr("Mini update ⚡", "Mini update ⚡"), color = Tk.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Text("v" + BuildConfig.VERSION_NAME, Modifier.clip(RoundedCornerShape(50)).background(Tk.Surface2).padding(horizontal = 8.dp, vertical = 3.dp), color = Tk.Muted, style = MaterialTheme.typography.labelMedium)
                        Text("  →  ", color = Tk.Muted, style = MaterialTheme.typography.labelMedium)
                        Text("v" + r.version, Modifier.clip(RoundedCornerShape(50)).background(Tk.Lime).padding(horizontal = 8.dp, vertical = 3.dp), color = Tk.OnLime, style = MaterialTheme.typography.labelMedium)
                    }
                    if (r.apkSize > 0) Text("%.1f MB".format(r.apkSize / 1_048_576f), color = Tk.Dim, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 3.dp))
                }
            }
            if (lines.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Text(tr("Is baar kya naya hai", "What's new"), color = Tk.Lime, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                lines.forEachIndexed { idx, line ->
                    // staggered: each line rises and fades in one after another
                    // staggered off the same spring: line i starts after the card has begun to rise
                    val p = ((enter.value - 0.35f - idx * 0.06f) / 0.35f).coerceIn(0f, 1f)
                    Text(
                        "•  $line", color = Tk.Muted, style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 2.dp).graphicsLayer { alpha = p; translationY = (1f - p) * 10f },
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            if (needPerm) {
                Text(
                    "Ek baar ki setting: Android pucchega \"Install unknown apps\". Tech Kadak ke saamne \"Allow from this source\" ON karo, phir wapas aake INSTALL dabao. Ye sirf Tech Kadak ke apne update ke liye hai.",
                    color = Tk.White, style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                LimeButton("SETTING KHOLO", Modifier.fillMaxWidth(), Icons.Rounded.Settings) { onPermission(); needPerm = false }
            } else when (dl) {
                is DlState.Idle -> {
                    LimeButton(tr("Update karo", "Update"), Modifier.fillMaxWidth(), Icons.Rounded.Download, onDownload)
                    Text(
                        tr("Download app ke andar hi hoga. Android last me ek baar \"Update\" tap maangega — Play Store ke bahar wale har app me hota hai.",
                            "The download happens inside the app. Android asks for one final \"Update\" tap — true for every app outside the Play Store."),
                        Modifier.padding(top = 8.dp), color = Tk.Dim, style = MaterialTheme.typography.bodySmall,
                    )
                }
                is DlState.Running -> {
                    val pct = (dl.progress.coerceIn(0f, 1f) * 100).toInt()
                    val total = r.apkSize.takeIf { it > 0 }
                    LinearProgressIndicator({ dl.progress.coerceIn(0f, 1f) }, Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)).shimmer(), color = Tk.Lime, trackColor = Tk.Surface2)
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(tr("Mini update download ho raha hai… $pct%", "Mini update downloading… $pct%"), color = Tk.Muted, style = MaterialTheme.typography.bodySmall)
                        if (total != null) Text("%.1f / %.1f MB".format(total * dl.progress / 1_048_576f, total / 1_048_576f), color = Tk.Dim, style = MaterialTheme.typography.bodySmall)
                    }
                }
                is DlState.Done -> {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 10.dp)) {
                        Icon(Icons.Rounded.CheckCircle, null, tint = Tk.Lime, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(tr("Download ho gaya ✅ Install screen me \"Update\" dabao", "Download complete ✅ Tap \"Update\" on the install screen"), color = Tk.White, style = MaterialTheme.typography.bodyMedium)
                    }
                    LimeButton(tr("INSTALL KARO", "INSTALL"), Modifier.fillMaxWidth(), Icons.Rounded.InstallMobile) { if (canInstall()) onInstall() else needPerm = true }
                }
                is DlState.Failed -> {
                    Text(tr("Download nahi hua", "Download didn't finish"), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text(dl.why, color = Tk.Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 2.dp))
                    Spacer(Modifier.height(10.dp))
                    LimeButton(tr("Dobara try karo", "Try again"), Modifier.fillMaxWidth(), Icons.Rounded.Refresh, onDownload)
                }
            }
            Spacer(Modifier.height(10.dp))
            if (force) {
                Text(tr("Ye update zaroori hai — iske bina app aage nahi chalega.", "This update is required to keep using the app."), color = Tk.Dim, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp))
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    Text(tr("Baad mein", "Later"), Modifier.clip(RoundedCornerShape(50)).clickable(onClick = onLater).padding(10.dp), color = Tk.Lime, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

/** Release notes -> short plain lines (no markdown, no empty lines, max 6). */
internal fun updateNoteLines(notes: String): List<String> = notes.lines()
    .map { it.replace("**", "").replace("`", "").trim().removePrefix("-").removePrefix("•").removePrefix("*").trim() }
    .filter { it.isNotBlank() }
    .take(6)

/** A soft white band that sweeps across while a download runs (shimmer). */
@Composable
private fun Modifier.shimmer(): Modifier {
    val inf = rememberInfiniteTransition(label = "shimmer")
    val x by inf.animateFloat(initialValue = -1f, targetValue = 2f, animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "shimmerX")
    return this.drawWithContent {
        drawContent()
        val w = size.width
        val band = w * 0.35f
        val start = x * w
        drawRect(brush = Brush.linearGradient(colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.35f), Color.Transparent), start = Offset(start - band, 0f), end = Offset(start + band, 0f)))
    }
}
