@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.techkadak.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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
import com.techkadak.app.R
import com.techkadak.app.data.CardStyle
import com.techkadak.app.data.SectionPref
import com.techkadak.app.data.ThemeMode
import com.techkadak.app.data.UserSettings
import kotlin.math.roundToInt

/* ─────────────────────────── SETTINGS / CUSTOMISE ─────────────────────────── */

class SettingsActions(
    val onTheme: (ThemeMode) -> Unit = {},
    val onAccent: (Long?) -> Unit = {},
    val onFont: (Float) -> Unit = {},
    val onCard: (CardStyle) -> Unit = {},
    val onSections: (List<SectionPref>?) -> Unit = {},
    val onEnglish: (Boolean) -> Unit = {},
    val onInApp: (Boolean) -> Unit = {},
    val onRefreshAnim: (Boolean) -> Unit = {},
    val onOpeningAnim: (Boolean) -> Unit = {},
    val onPush: (Boolean) -> Unit = {},
    val onDaily: (Boolean) -> Unit = {},
    val onClearCache: ((String) -> Unit) -> Unit = {},
    val onReset: () -> Unit = {},
    val onReplayOnboarding: () -> Unit = {},
    val onAbout: () -> Unit = {},
    val onAutoUpdate: (com.techkadak.app.data.AutoUpdateMode) -> Unit = {},
    val onDownloads: () -> Unit = {},
    val onStats: (Boolean) -> Unit = {},
    val onAdmin: () -> Unit = {},
)

@Composable
fun SettingsScreen(
    s: UserSettings,
    sections: List<SectionPref>,
    push: Boolean, daily: Boolean, firebaseOn: Boolean,
    act: SettingsActions,
    onBack: (() -> Unit)?,
    statsOn: Boolean = true,
) {
    var stats by remember(statsOn) { mutableStateOf(statsOn) }
    var showPicker by remember { mutableStateOf(false) }
    var cacheMsg by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenTitle(tr("Customise", "Customise"), tr("App ko apne hisaab se banao", "Make the app yours"), onBack)
        PreviewCard()

        Group(tr("Theme", "Theme"), Icons.Rounded.DarkMode)
        ThemeMode.entries.chunked(2).forEach { pair ->
            Row(Modifier.padding(horizontal = 18.dp, vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { m -> ThemeOption(m, m == s.theme, Modifier.weight(1f)) { act.onTheme(m) } }
            }
        }

        Group(tr("Accent colour", "Accent colour"), Icons.Rounded.Palette)
        val current = s.accent
        FlowRow(Modifier.padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AccentPresets.forEachIndexed { i, (name, argb) ->
                val sel = if (current == null) i == 0 else current == argb
                Swatch(Color(argb), sel, name) { act.onAccent(if (i == 0) null else argb) }
            }
            val custom = current != null && AccentPresets.none { it.second == current }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(44.dp).clip(CircleShape)
                        .background(if (custom) Brush.linearGradient(listOf(Color(current!!), Color(current))) else Brush.sweepGradient(listOf(Color(0xFFFF4D4D), Color(0xFFFFD60A), Color(0xFF9BFF2E), Color(0xFF2EFFB0), Color(0xFFFF8A1F), Color(0xFFFF4D4D))))
                        .border(if (custom) 3.dp else 1.dp, if (custom) Tk.White else Tk.Line, CircleShape)
                        .clickable { showPicker = true },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Colorize, null, tint = Color.Black, modifier = Modifier.size(20.dp)) }
                Text(tr("Apna", "Custom"), color = Tk.Muted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
            }
        }

        Group(tr("Font size", "Font size"), Icons.Rounded.FormatSize)
        Row(Modifier.padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("A", color = Tk.Muted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Slider(
                s.fontScale, { act.onFont((it * 20).roundToInt() / 20f) }, Modifier.weight(1f).padding(horizontal = 12.dp),
                valueRange = 0.85f..1.3f, steps = 8,
                colors = SliderDefaults.colors(thumbColor = Tk.Lime, activeTrackColor = Tk.Lime, inactiveTrackColor = Tk.Surface2, activeTickColor = Tk.OnLime, inactiveTickColor = Tk.Line),
            )
            Text("A", color = Tk.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            "${(s.fontScale * 100).roundToInt()}%  •  " + when {
                s.fontScale < 0.95f -> tr("Chhota", "Small"); s.fontScale > 1.08f -> tr("Bada", "Large"); else -> "Normal"
            },
            Modifier.padding(horizontal = 18.dp), color = Tk.Muted, style = MaterialTheme.typography.bodySmall,
        )

        Group(tr("Card style", "Card style"), Icons.Rounded.Dashboard)
        Row(Modifier.padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CardStyle.entries.forEach { c ->
                val icon = when (c) { CardStyle.GRID -> Icons.Rounded.GridView; CardStyle.LIST -> Icons.AutoMirrored.Rounded.ViewList; CardStyle.COMPACT -> Icons.Rounded.ViewAgenda }
                OptionTile(icon, tr(c.hi, c.en), c == s.cardStyle, Modifier.weight(1f)) { act.onCard(c) }
            }
        }

        Group(tr("Home sections", "Home sections"), Icons.Rounded.Tune)
        Text(
            tr("On/off karo aur ↑ ↓ se order badlo", "Toggle and reorder with ↑ ↓"),
            Modifier.padding(horizontal = 18.dp), color = Tk.Muted, style = MaterialTheme.typography.bodySmall,
        )
        Column(Modifier.padding(horizontal = 14.dp, vertical = 6.dp).clip(RoundedCornerShape(18.dp)).background(Tk.Surface).border(1.dp, Tk.Line, RoundedCornerShape(18.dp))) {
            sections.forEachIndexed { i, sp ->
                SectionRow(
                    sp, first = i == 0, last = i == sections.lastIndex,
                    onToggle = { v -> act.onSections(sections.toMutableList().also { it[i] = sp.copy(visible = v) }) },
                    onMove = { d ->
                        val j = i + d
                        if (j in sections.indices) act.onSections(sections.toMutableList().also { val t = it[i]; it[i] = it[j]; it[j] = t })
                    },
                )
                if (i != sections.lastIndex) HorizontalDivider(color = Tk.Line)
            }
        }
        Text(
            tr("Default order wapas", "Reset to default order"),
            Modifier.padding(horizontal = 18.dp).clickable { act.onSections(null) }.padding(vertical = 8.dp),
            color = Tk.Lime, style = MaterialTheme.typography.labelLarge,
        )

        Group(tr("Bhasha", "Language"), Icons.Rounded.Translate)
        Row(Modifier.padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OptionTile(null, "Hinglish", !s.english, Modifier.weight(1f)) { act.onEnglish(false) }
            OptionTile(null, "English", s.english, Modifier.weight(1f)) { act.onEnglish(true) }
        }

        Group(tr("Links", "Links"), Icons.Rounded.Language)
        SwitchRow(Icons.Rounded.Web, tr("Website app ke andar kholo", "Open website pages in-app"), tr("Tech Kadak ke official pages app me hi khulenge", "Official Tech Kadak pages open inside the app"), s.inAppBrowser, true, act.onInApp)

        Group(tr("Animation", "Animation"), Icons.Rounded.Refresh)
        SwitchRow(Icons.Rounded.Refresh, tr("Refresh animation", "Refresh animation"), tr("Neeche kheenchne pe robot ka haath aata hai. Off = simple refresh", "A mascot hand reaches down when you pull to refresh. Off = plain refresh"), s.refreshAnim, true, act.onRefreshAnim)
        SwitchRow(Icons.Rounded.AutoAwesome, tr("Opening animation", "Opening animation"), tr("App khulte waqt logo ka intro. Tap karke skip. Off = seedha app", "Logo intro when the app opens. Tap to skip. Off = straight to the app"), s.openingAnim, true, act.onOpeningAnim)

        Group(tr("Apps auto-update", "Apps auto-update"), Icons.Rounded.SystemUpdate)
        Row(Modifier.padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OptionTile(null, tr("Off", "Off"), s.autoUpdate == com.techkadak.app.data.AutoUpdateMode.OFF, Modifier.weight(1f)) { act.onAutoUpdate(com.techkadak.app.data.AutoUpdateMode.OFF) }
            OptionTile(null, tr("Sirf Wi-Fi pe", "Wi-Fi only"), s.autoUpdate == com.techkadak.app.data.AutoUpdateMode.WIFI, Modifier.weight(1f)) { act.onAutoUpdate(com.techkadak.app.data.AutoUpdateMode.WIFI) }
            OptionTile(null, tr("Hamesha", "Always"), s.autoUpdate == com.techkadak.app.data.AutoUpdateMode.ALWAYS, Modifier.weight(1f)) { act.onAutoUpdate(com.techkadak.app.data.AutoUpdateMode.ALWAYS) }
        }
        Text(
            tr("Tech Kadak se install kiye apps ka naya version background me aata hai. Android 12+ pe update bina tap ke; warna \"Update ready — tap to install\" notification. Tech Kadak ka apna update sabse pehle.",
                "Apps installed through Tech Kadak update in the background. Android 12+ updates without a tap; otherwise you get an \"Update ready — tap to install\" notification. Tech Kadak's own update comes first."),
            Modifier.padding(horizontal = 18.dp, vertical = 8.dp), color = Tk.Dim, style = MaterialTheme.typography.bodySmall,
        )
        Row(
            Modifier.padding(horizontal = 18.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Tk.Surface2).clickable(onClick = act.onDownloads).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Download, null, tint = Tk.Lime); Spacer(Modifier.width(10.dp))
            Text(tr("Downloads & Updates", "Downloads & Updates"), Modifier.weight(1f), color = Tk.White, style = MaterialTheme.typography.titleSmall)
            Icon(Icons.Rounded.ChevronRight, null, tint = Tk.Dim)
        }

        Group(tr("Privacy", "Privacy"), Icons.Rounded.Shield)
        SwitchRow(
            Icons.Rounded.QueryStats, tr("Usage stats bhejo", "Send usage stats"),
            tr("Anonymous ginti (kitne log app khol rahe, kaunsa app install hua) taaki Tech Kadak behtar bane. Naam, email, phone kabhi nahi.",
                "Anonymous counts (opens, which apps get installed) to improve Tech Kadak. Never your name, email or phone."),
            stats, true,
        ) { stats = it; act.onStats(it) }
        run {
            val shareCtx = androidx.compose.ui.platform.LocalContext.current
            var share by remember { mutableStateOf(com.techkadak.app.stats.DeviceShare.enabled(shareCtx)) }
            SwitchRow(
                androidx.compose.material.icons.Icons.Rounded.PhoneAndroid, tr("Device ka naam + apps list dikhao", "Show device name + app list"),
                tr("Tech Kadak owner ko is phone ka naam aur installed apps ke NAAM dikhenge, taaki behtar app suggestions ban sakein. Messages, photos, passwords kabhi nahi. Kabhi bhi band kar sakte ho.",
                    "The Tech Kadak owner sees this phone's name and the NAMES of installed apps, to make better app suggestions. Never messages, photos or passwords. Turn off any time."),
                share, true,
            ) { share = it; com.techkadak.app.stats.DeviceShare.set(shareCtx, it); Thread { com.techkadak.app.stats.Stats.flush(shareCtx, force = true) }.start() }
        }
        Row(
            Modifier.padding(horizontal = 18.dp, vertical = 6.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Tk.Surface2).clickable(onClick = act.onAdmin).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.AdminPanelSettings, null, tint = Tk.Lime); Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(tr("Admin access 🔐", "Admin access 🔐"), color = Tk.White, style = MaterialTheme.typography.titleSmall)
                Text(tr("Sirf app owner ke liye — dashboard", "App owner only — dashboard"), color = Tk.Muted, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = Tk.Dim)
        }

        if (com.techkadak.app.data.UpiLink.isValidId(com.techkadak.app.BuildConfig.UPI_ID)) {
            Group(tr("Support", "Support"), Icons.Rounded.LocalCafe)
            SupportCard(Modifier.padding(horizontal = 18.dp))
            Text(tr("Ye sirf option hai, app free rahega.", "This is optional, the app stays free."), Modifier.padding(horizontal = 18.dp, vertical = 6.dp), color = Tk.Dim, style = MaterialTheme.typography.bodySmall)
        }

        Group(tr("Notifications", "Notifications"), Icons.Rounded.Notifications)
        SwitchRow(Icons.Rounded.NotificationsActive, tr("Naya app aaye to notification", "Notify me when a new app lands"), if (firebaseOn) "Firebase" else tr("Firebase abhi set nahi hai", "Firebase not set up"), push && firebaseOn, firebaseOn, act.onPush)
        SwitchRow(Icons.Rounded.Today, tr("Roz ki yaad", "Daily reminder"), tr("Bot naya app daale to phone khud bataye", "Phone tells you when a new app lands"), daily, true, act.onDaily)

        Group(tr("Storage", "Storage"), Icons.Rounded.Storage)
        Row(Modifier.padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlineButton(tr("Cache saaf karo", "Clear cache"), icon = Icons.Rounded.CleaningServices) { act.onClearCache { cacheMsg = it } }
            cacheMsg?.let { Text(tr("$it saaf ✅", "$it cleared ✅"), color = Tk.Muted, style = MaterialTheme.typography.bodySmall) }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlineButton(tr("Look reset", "Reset look"), icon = Icons.Rounded.RestartAlt, onClick = act.onReset)
            OutlineButton(tr("Intro dobara", "Show intro"), icon = Icons.Rounded.Slideshow, onClick = act.onReplayOnboarding)
        }
        Group(tr("About", "About"), Icons.Rounded.Info)
        Row(
            Modifier.padding(horizontal = 18.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Tk.Surface2).clickable(onClick = act.onAbout).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(tr("Tech Kadak ke baare me • Developer • Licenses", "About Tech Kadak • Developer • Licenses"), Modifier.weight(1f), color = Tk.White, style = MaterialTheme.typography.titleSmall)
            Icon(Icons.Rounded.ChevronRight, null, tint = Tk.Dim)
        }
        CreditLine(Modifier.fillMaxWidth().padding(top = 18.dp))
        Text(
            tr("Favourites reset se nahi hat-te. Version ", "Favourites are kept on reset. Version ") + BuildConfig.VERSION_NAME,
            Modifier.fillMaxWidth().padding(24.dp), color = Tk.Dim, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
        )
    }
    if (showPicker) ColorPickerDialog(s.accent ?: Palette.LIME.toArgb().toLong() and 0xFFFFFFFFL, onDismiss = { showPicker = false }) { act.onAccent(it); showPicker = false }
}

@Composable
private fun PreviewCard() {
    Column(
        Modifier.padding(horizontal = 18.dp, vertical = 8.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(Tk.HeroStart, Tk.Surface))).border(1.dp, Tk.LimeBorder, RoundedCornerShape(22.dp)).padding(16.dp),
    ) {
        Text(tr("PREVIEW", "PREVIEW"), color = Tk.Lime, fontFamily = Display, fontWeight = FontWeight.Black, fontSize = 10.sp, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.tk_logo), null, Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)), contentScale = ContentScale.Crop)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Tech Kadak", color = Tk.White, style = MaterialTheme.typography.titleLarge)
                Row(verticalAlignment = Alignment.CenterVertically) { PriceBadge("Free"); Spacer(Modifier.width(6.dp)); Text("AI Tools", color = Tk.Muted, style = MaterialTheme.typography.bodySmall) }
            }
            Icon(Icons.Rounded.Favorite, null, tint = Tk.Lime)
        }
        Spacer(Modifier.height(12.dp))
        LimeButton("DOWNLOAD", Modifier.fillMaxWidth(), Icons.Rounded.Download) {}
    }
}

@Composable
private fun Group(title: String, icon: ImageVector) {
    Row(Modifier.padding(start = 18.dp, end = 18.dp, top = 22.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Tk.Lime, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, color = Tk.White, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun ThemeOption(m: ThemeMode, sel: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val p = Palette.of(m, Tk.palette.accent)
    val border by animateColorAsState(if (sel) Tk.Lime else Tk.Line, label = "tb")
    Column(
        modifier.clip(RoundedCornerShape(18.dp)).border(if (sel) 2.dp else 1.dp, border, RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val bgMod = if (m == ThemeMode.GLASS) Modifier.background(Brush.linearGradient(listOf(Color(0xFF173A12), Color(0xFF07110B), Color(0xFF0E3A2C), Color(0xFF3A3410))))
            else Modifier.background(p.bg)
        Column(Modifier.fillMaxWidth().height(74.dp).clip(RoundedCornerShape(12.dp)).then(bgMod).border(1.dp, p.line, RoundedCornerShape(12.dp)).padding(8.dp)) {
            if (m == ThemeMode.GLASS) {
                Box(Modifier.fillMaxWidth().height(24.dp).clip(RoundedCornerShape(8.dp)).background(Color(0x2EFFFFFF)).border(1.dp, Color(0x55FFFFFF), RoundedCornerShape(8.dp)))
                Spacer(Modifier.height(5.dp))
            }
            Box(Modifier.fillMaxWidth(0.8f).height(8.dp).clip(CircleShape).background(p.text.copy(alpha = 0.85f)))
            Spacer(Modifier.height(5.dp))
            Box(Modifier.fillMaxWidth(0.55f).height(6.dp).clip(CircleShape).background(p.muted))
            Spacer(Modifier.weight(1f))
            Box(Modifier.fillMaxWidth().height(14.dp).clip(CircleShape).background(p.ink))
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (sel) { Icon(Icons.Rounded.CheckCircle, null, tint = Tk.Lime, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)) }
            Text(m.hi, color = Tk.White, style = MaterialTheme.typography.labelMedium, maxLines = 1)
            if (m == ThemeMode.GLASS && !sel) { Spacer(Modifier.width(4.dp)); Text("NAYA", Modifier.clip(RoundedCornerShape(50)).background(Tk.Lime).padding(horizontal = 5.dp, vertical = 1.dp), color = Tk.OnLime, fontSize = 8.sp, fontWeight = FontWeight.Black) }
        }
    }
}

@Composable
private fun Swatch(c: Color, sel: Boolean, name: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(44.dp).clip(CircleShape).background(c).border(if (sel) 3.dp else 1.dp, if (sel) Tk.White else Tk.Line, CircleShape).clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) { if (sel) Icon(Icons.Rounded.Check, null, tint = Color.Black, modifier = Modifier.size(20.dp)) }
        Text(name, color = Tk.Muted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun OptionTile(icon: ImageVector?, label: String, sel: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier.height(48.dp).clip(RoundedCornerShape(14.dp)).background(if (sel) Tk.Lime else Tk.Surface2)
            .border(1.dp, if (sel) Tk.Lime else Tk.Line, RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) { Icon(icon, null, tint = if (sel) Tk.OnLime else Tk.Lime, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)) }
        Text(label, color = if (sel) Tk.OnLime else Tk.White, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

@Composable
private fun SectionRow(sp: SectionPref, first: Boolean, last: Boolean, onToggle: (Boolean) -> Unit, onMove: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 4.dp, top = 2.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(tr(sp.section.hi, sp.section.en), Modifier.weight(1f), color = if (sp.visible) Tk.White else Tk.Dim, style = MaterialTheme.typography.titleSmall)
        IconButton(onClick = { onMove(-1) }, enabled = !first) { Icon(Icons.Rounded.KeyboardArrowUp, "Upar", tint = if (first) Tk.Line else Tk.Muted) }
        IconButton(onClick = { onMove(1) }, enabled = !last) { Icon(Icons.Rounded.KeyboardArrowDown, "Neeche", tint = if (last) Tk.Line else Tk.Muted) }
        Switch(sp.visible, onToggle, Modifier.scale(0.85f), colors = tkSwitchColors())
    }
}


@Composable
fun tkSwitchColors() = SwitchDefaults.colors(
    checkedTrackColor = Tk.Lime, checkedThumbColor = Tk.OnLime, uncheckedTrackColor = Tk.Surface2,
    uncheckedThumbColor = Tk.Muted, uncheckedBorderColor = Tk.Line, checkedBorderColor = Tk.Lime,
)

@Composable
private fun SwitchRow(icon: ImageVector, title: String, sub: String, on: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Tk.Surface2), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Tk.Lime) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Tk.White, style = MaterialTheme.typography.titleSmall)
            Text(sub, color = Tk.Muted, style = MaterialTheme.typography.bodySmall)
        }
        Switch(on, onChange, enabled = enabled, colors = tkSwitchColors())
    }
}

/** Custom colour: Hue / Saturation / Brightness sliders + hex. */
@Composable
fun ColorPickerDialog(initial: Long, onDismiss: () -> Unit, onPick: (Long) -> Unit) {
    val hsv = remember { FloatArray(3).also { android.graphics.Color.colorToHSV(initial.toInt(), it) } }
    var h by remember { mutableFloatStateOf(hsv[0]) }
    var sat by remember { mutableFloatStateOf(hsv[1]) }
    var v by remember { mutableFloatStateOf(hsv[2]) }
    val argb = android.graphics.Color.HSVToColor(floatArrayOf(h, sat, v)).toLong() and 0xFFFFFFFFL
    val c = Color(argb)
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.padding(22.dp).fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(Tk.Surface).border(1.dp, Tk.Line, RoundedCornerShape(26.dp)).padding(20.dp)) {
            Text(tr("Apna colour chuno", "Pick your colour"), color = Tk.White, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(c).border(1.dp, Tk.Line, RoundedCornerShape(16.dp)))
                Spacer(Modifier.width(14.dp))
                Text("#%06X".format(argb and 0xFFFFFF), color = Tk.White, fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Spacer(Modifier.height(10.dp))
            PickerSlider("Hue", h / 360f, Brush.horizontalGradient((0..6).map { Color(android.graphics.Color.HSVToColor(floatArrayOf(it * 60f, 1f, 1f))) })) { h = it * 360f }
            PickerSlider(tr("Rang", "Saturation"), sat, Brush.horizontalGradient(listOf(Color(android.graphics.Color.HSVToColor(floatArrayOf(h, 0f, v))), Color(android.graphics.Color.HSVToColor(floatArrayOf(h, 1f, v)))))) { sat = it }
            PickerSlider(tr("Roshni", "Brightness"), v, Brush.horizontalGradient(listOf(Color.Black, Color(android.graphics.Color.HSVToColor(floatArrayOf(h, sat, 1f)))))) { v = it.coerceAtLeast(0.25f) }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                Text(tr("Cancel", "Cancel"), Modifier.clickable(onClick = onDismiss).padding(12.dp), color = Tk.Muted, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.width(8.dp))
                Text(
                    tr("LAGAO", "APPLY"), Modifier.clip(RoundedCornerShape(50)).background(c).clickable { onPick(argb) }.padding(horizontal = 20.dp, vertical = 10.dp),
                    color = if (c.luminance() > 0.4f) Color.Black else Color.White, fontFamily = Display, fontWeight = FontWeight.Black, fontSize = 13.sp,
                )
            }
        }
    }
}

@Composable
private fun PickerSlider(label: String, value: Float, track: Brush, onChange: (Float) -> Unit) {
    Text(label, color = Tk.Muted, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 10.dp))
    Box(contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 10.dp).height(12.dp).clip(CircleShape).background(track))
        Slider(value, onChange, colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.Transparent, inactiveTrackColor = Color.Transparent))
    }
}
