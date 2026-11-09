package com.techkadak.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.techkadak.app.auth.TkUser
import com.techkadak.app.data.AppItem
import com.techkadak.app.data.Dates
import com.techkadak.app.stats.DashData
import com.techkadak.app.stats.OwnerStats
import kotlinx.coroutines.launch

class AdminActions(
    val onLogin: (String, String) -> Unit = { _, _ -> },
    val onGoogle: () -> Unit = {},
    val onForgot: () -> Unit = {},
    val onLogout: () -> Unit = {},
    val onRefresh: () -> Unit = {},
)

/** Settings → Admin access. Sirf ridhirohit0@gmail.com ka account dashboard khol sakta hai (Firestore rules bhi yahi check karte hain). */
@Composable
fun AdminScreen(
    user: TkUser?,
    busy: Boolean,
    error: String?,
    info: String?,
    lockedSeconds: Int,
    dash: DashData?,
    dashLoading: Boolean,
    dashError: String?,
    apps: List<AppItem>,
    act: AdminActions,
    onBack: () -> Unit,
) {
    val owner = user != null && OwnerStats.isOwner(user.email)
    LaunchedEffect(owner) { if (owner && dash == null && !dashLoading) act.onRefresh() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenTitle(
            if (owner) tr("Owner Dashboard 📊", "Owner Dashboard 📊") else tr("Admin access 🔐", "Admin access 🔐"),
            if (owner) tr("Tech Kadak app ki activity", "Tech Kadak app activity") else tr("Sirf app owner ke liye", "App owner only"),
            onBack,
        )
        when {
            owner -> Dashboard(dash, dashLoading, dashError, apps, user!!, act)
            user != null -> AccessDenied(user, act)
            else -> AdminLogin(busy, error, info, lockedSeconds, act)
        }
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun AdminLogin(busy: Boolean, error: String?, info: String?, lockedSeconds: Int, act: AdminActions) {
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var show by remember { mutableStateOf(false) }
    val locked = lockedSeconds > 0
    Column(Modifier.padding(horizontal = 18.dp)) {
        Box(Modifier.fillMaxWidth().padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(84.dp).clip(CircleShape).background(Tk.LimeSoft).border(2.dp, Tk.Lime, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.AdminPanelSettings, null, tint = Tk.Lime, modifier = Modifier.size(42.dp))
            }
        }
        Text(tr("Owner ka Gmail aur password daalo", "Enter the owner's Gmail and password"), color = Tk.White, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(14.dp))
        val colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Tk.Lime, unfocusedBorderColor = Tk.Line, focusedLabelColor = Tk.Lime, unfocusedLabelColor = Tk.Muted,
            cursorColor = Tk.Lime, focusedTextColor = Tk.White, unfocusedTextColor = Tk.White,
        )
        OutlinedTextField(
            email, { email = it.trim() }, Modifier.fillMaxWidth(), label = { Text("Gmail") }, singleLine = true,
            leadingIcon = { Icon(Icons.Rounded.Email, null, tint = Tk.Muted) }, colors = colors, shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), enabled = !locked,
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            pass, { pass = it }, Modifier.fillMaxWidth(), label = { Text("Password") }, singleLine = true,
            leadingIcon = { Icon(Icons.Rounded.Lock, null, tint = Tk.Muted) },
            trailingIcon = { IconButton(onClick = { show = !show }) { Icon(if (show) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, null, tint = Tk.Muted) } },
            visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), colors = colors, shape = RoundedCornerShape(16.dp), enabled = !locked,
        )
        Spacer(Modifier.height(14.dp))
        if (locked) {
            AdminNotice(Icons.Rounded.Timer, tr("Bahut galat try ho gaye. ${lockedSeconds / 60}:${"%02d".format(lockedSeconds % 60)} baad dobara try karo.", "Too many wrong tries. Try again in ${lockedSeconds / 60}:${"%02d".format(lockedSeconds % 60)}."), true)
        } else {
            LimeButton(if (busy) tr("Check ho raha…", "Checking…") else tr("Login karo", "Log in"), Modifier.fillMaxWidth(), Icons.AutoMirrored.Rounded.Login) {
                if (!busy && email.isNotBlank() && pass.isNotBlank()) act.onLogin(email, pass)
            }
            Spacer(Modifier.height(10.dp))
            OutlineButton(tr("Google se login (owner Gmail)", "Sign in with Google (owner Gmail)"), Modifier.fillMaxWidth(), Icons.Rounded.AccountCircle) { if (!busy) act.onGoogle() }
        }
        error?.let { Spacer(Modifier.height(10.dp)); AdminNotice(Icons.Rounded.ErrorOutline, it, true) }
        info?.let { Spacer(Modifier.height(10.dp)); AdminNotice(Icons.Rounded.CheckCircle, it, false) }
        Text(
            tr("Password bhool gaye?", "Forgot password?"),
            Modifier.padding(top = 12.dp).clickable(enabled = !busy) { act.onForgot() }.padding(vertical = 6.dp),
            color = Tk.Lime, style = MaterialTheme.typography.labelLarge,
        )
        Text(
            tr("Reset link sirf owner ke Gmail pe jaata hai. 5 galat try ke baad 5 minute ka lock.", "The reset link only goes to the owner's Gmail. 5 wrong tries lock it for 5 minutes."),
            color = Tk.Dim, style = MaterialTheme.typography.bodySmall,
        )
    }
}


@Composable
private fun AccessDenied(user: TkUser, act: AdminActions) {
    Column(Modifier.padding(18.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(90.dp).clip(CircleShape).background(Color(0x33FF4D4D)), contentAlignment = Alignment.Center) { Text("🚫", fontSize = 40.sp) }
        Spacer(Modifier.height(14.dp))
        Text("Access denied", color = Tk.White, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            tr("Ye dashboard sirf app owner ke account se khulta hai. Abhi login: ${user.email ?: user.phone ?: "dusra account"}",
                "This dashboard opens only with the app owner's account. Signed in as: ${user.email ?: user.phone ?: "another account"}"),
            color = Tk.Muted, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        OutlineButton(tr("Logout karke owner se login karo", "Log out and sign in as owner"), Modifier.fillMaxWidth(), Icons.AutoMirrored.Rounded.Logout) { act.onLogout() }
    }
}

@Composable
private fun AdminNotice(icon: ImageVector, text: String, bad: Boolean) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (bad) Color(0x22FF4D4D) else Tk.LimeSoft).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = if (bad) Color(0xFFFF6B6B) else Tk.Lime, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, color = Tk.White, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun Dashboard(d: DashData?, loading: Boolean, err: String?, apps: List<AppItem>, user: TkUser, act: AdminActions) {
    val name: (String) -> String = { slug -> apps.firstOrNull { it.slug == slug }?.name ?: slug }
    Column(Modifier.padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(Tk.Lime))
            Spacer(Modifier.width(8.dp))
            Text(user.email ?: "", color = Tk.Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            TextButton(onClick = act.onRefresh, enabled = !loading) {
                Icon(Icons.Rounded.Refresh, null, tint = Tk.Lime, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp))
                Text(if (loading) tr("Load ho raha…", "Loading…") else tr("Refresh", "Refresh"), color = Tk.Lime)
            }
        }
        err?.let { AdminNotice(Icons.Rounded.ErrorOutline, it, true); Spacer(Modifier.height(10.dp)) }
        if (d == null) {
            if (loading) Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Tk.Lime) }
            return@Column
        }
        // KPI grid
        val kpis = listOf(
            Triple(tr("Aaj active", "Active today"), d.activeToday, Icons.Rounded.Bolt),
            Triple(tr("Aaj naye", "New today"), d.newToday, Icons.Rounded.PersonAdd),
            Triple(tr("Wapas aaye (aaj)", "Returning today"), d.returningToday, Icons.Rounded.Replay),
            Triple(tr("7 din active", "Active 7 days"), d.active7, Icons.Rounded.DateRange),
            Triple(tr("Chale gaye (7+ din)", "Gone (7+ days)"), d.churned7, Icons.AutoMirrored.Rounded.TrendingDown),
            Triple(tr("Total devices", "Total devices"), d.totalDevices, Icons.Rounded.PhoneAndroid),
        )
        kpis.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEachIndexed { i, (label, v, icon) -> Kpi(label, v, icon, Modifier.weight(1f), highlight = label == kpis[0].first) }
            }
        }
        Spacer(Modifier.height(10.dp))
        Card(tr("Roz ke active users (30 din)", "Daily active users (30 days)")) {
            BarChart(d.days.map { it.active }, d.days.map { it.new })
            Row(Modifier.padding(top = 6.dp)) {
                Legend(Tk.Lime, tr("Active", "Active")); Spacer(Modifier.width(14.dp)); Legend(Tk.White.copy(alpha = 0.55f), tr("Naye", "New"))
                Spacer(Modifier.weight(1f))
                Text(Dates.pretty(d.days.first().date).substringBeforeLast(' ') + " → " + Dates.pretty(d.days.last().date).substringBeforeLast(' '), color = Tk.Dim, style = MaterialTheme.typography.bodySmall)
            }
            val last7 = d.days.takeLast(7)
            Text(
                tr("7 din: ${last7.sumOf { it.new }} naye users • aaj ${d.activeToday} active", "7 days: ${last7.sumOf { it.new }} new users • ${d.activeToday} active today"),
                Modifier.padding(top = 6.dp), color = Tk.Muted, style = MaterialTheme.typography.bodySmall,
            )
        }
        Card(tr("Aa rahe vs ja rahe", "Coming vs going")) {
            StatLine(tr("Pichhle 7 din me khola", "Opened in last 7 days"), d.active7, d.totalDevices)
            StatLine(tr("7 din se nahi khola (shayad chale gaye)", "Not opened for 7 days (likely gone)"), d.churned7, d.totalDevices)
            StatLine(tr("30 din se nahi khola (uninstall jaisa)", "Not opened for 30 days (uninstall-like)"), d.churned30, d.totalDevices)
        }
        Card(tr("Sabse zyada install (7 din)", "Most installed (7 days)")) {
            if (d.topInstalls.isEmpty()) Muted(tr("Abhi koi install record nahi", "No installs recorded yet"))
            val mx = d.topInstalls.maxOfOrNull { it.second } ?: 1
            d.topInstalls.forEach { (s, n) -> RankBar(name(s), n, mx) }
            if (d.topClicks.isNotEmpty()) {
                Text(tr("Install button dabaya:", "Install taps:"), Modifier.padding(top = 10.dp, bottom = 4.dp), color = Tk.Muted, style = MaterialTheme.typography.labelMedium)
                val mc = d.topClicks.maxOf { it.second }
                d.topClicks.take(5).forEach { (s, n) -> RankBar(name(s), n, mc) }
            }
        }
        Card(tr("Kits aur Posters (7 din)", "Kits and posters (7 days)")) {
            if (d.kitInstalls.isEmpty()) Muted(tr("Kit install: abhi 0", "Kit installs: 0 so far"))
            val mk = d.kitInstalls.maxOfOrNull { it.second } ?: 1
            d.kitInstalls.forEach { (k, n) -> RankBar(k, n, mk) }
            Text(tr("Poster share: ${d.posterShares}", "Poster shares: ${d.posterShares}"), Modifier.padding(top = 8.dp), color = Tk.White, style = MaterialTheme.typography.titleSmall)
            d.posterTop.forEach { (s, n) -> Text("• ${name(s)}: $n", color = Tk.Muted, style = MaterialTheme.typography.bodySmall) }
        }
        Card(tr("Kaun kis version pe", "Version split")) {
            if (d.versions.isEmpty()) Muted(tr("Abhi data nahi", "No data yet"))
            val mv = d.versions.maxOfOrNull { it.second } ?: 1
            d.versions.forEach { (v, n) -> RankBar("v$v", n, mv) }
        }
        if (d.loginTypes.isNotEmpty()) Card(tr("Login type", "Login type")) {
            val ml = d.loginTypes.maxOf { it.second }
            d.loginTypes.forEach { (t, n) -> RankBar(when (t) { "none" -> tr("Bina login", "No login"); "google" -> "Google"; "email" -> "Email"; else -> "Phone" }, n, ml) }
        }
        DevicesCard()
        Text(
            tr("Note: ginti v1.2 se shuru hui hai (4 Nov 2026). Jo log v1.2 se pehle ke version pe hain, wo update karne ke baad hi gine jayenge. Sirf anonymous device ginti — kisi ka naam/email nahi. \"Usage stats\" band karne wale nahi gine jaate. Poora Google Analytics: Firebase console → Analytics.",
                "Note: counting started with v1.2 (4 Nov 2026). People on older versions are counted only after they update. Anonymous device counts only — no names or emails. People who turn off usage stats aren't counted. Full Google Analytics: Firebase console → Analytics."),
            Modifier.padding(vertical = 12.dp), color = Tk.Dim, style = MaterialTheme.typography.bodySmall,
        )
        OutlineButton(tr("Admin se logout", "Log out of admin"), Modifier.fillMaxWidth(), Icons.AutoMirrored.Rounded.Logout) { act.onLogout() }
    }
}

@Composable
private fun Muted(t: String) = Text(t, color = Tk.Muted, style = MaterialTheme.typography.bodySmall)

@Composable
private fun Legend(c: Color, t: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(c)); Spacer(Modifier.width(5.dp))
        Text(t, color = Tk.Muted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun Kpi(label: String, v: Long, icon: ImageVector, modifier: Modifier, highlight: Boolean = false) {
    Column(
        modifier.clip(RoundedCornerShape(20.dp)).background(if (highlight) Tk.Lime else Tk.Surface).border(1.dp, if (highlight) Tk.Lime else Tk.Line, RoundedCornerShape(20.dp)).padding(14.dp),
    ) {
        Icon(icon, null, tint = if (highlight) Tk.OnLime else Tk.Lime, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(8.dp))
        Text(v.toString(), color = if (highlight) Tk.OnLime else Tk.White, fontFamily = Display, fontWeight = FontWeight.Black, fontSize = 26.sp)
        Text(label, color = if (highlight) Tk.OnLimeMuted else Tk.Muted, style = MaterialTheme.typography.bodySmall, maxLines = 1)
    }
}

@Composable
private fun Card(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.padding(vertical = 6.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Tk.Surface).border(1.dp, Tk.Line, RoundedCornerShape(22.dp)).padding(16.dp),
    ) {
        Text(title, color = Tk.White, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun BarChart(active: List<Long>, new: List<Long>) {
    val lime = Tk.Lime; val white = Tk.White.copy(alpha = 0.55f); val grid = Tk.Line
    val max = (active.maxOrNull() ?: 0L).coerceAtLeast(1L)
    Box {
        Canvas(Modifier.fillMaxWidth().height(150.dp)) {
            val n = active.size.coerceAtLeast(1)
            val slot = size.width / n
            val bw = slot * 0.62f
            for (g in 1..3) { val y = size.height * g / 4f; drawLine(grid, Offset(0f, y), Offset(size.width, y), 1f) }
            active.forEachIndexed { i, v ->
                val h = size.height * v / max
                val x = i * slot + (slot - bw) / 2
                drawRoundRect(lime, Offset(x, size.height - h), Size(bw, h.coerceAtLeast(if (v > 0) 3f else 0f)), CornerRadius(bw / 3, bw / 3))
                val nv = new.getOrElse(i) { 0L }
                if (nv > 0) { val nh = size.height * nv / max; drawRoundRect(white, Offset(x, size.height - nh), Size(bw, nh.coerceAtLeast(3f)), CornerRadius(bw / 3, bw / 3)) }
            }
        }
        Text("max $max", Modifier.align(Alignment.TopEnd), color = Tk.Dim, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun StatLine(label: String, v: Long, total: Long) {
    val f = if (total > 0) v.toFloat() / total else 0f
    Column(Modifier.padding(vertical = 5.dp)) {
        Row {
            Text(label, Modifier.weight(1f), color = Tk.White, style = MaterialTheme.typography.bodySmall)
            Text("$v  (${(f * 100).toInt()}%)", color = Tk.Lime, style = MaterialTheme.typography.labelMedium)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(progress = { f }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape), color = Tk.Lime, trackColor = Tk.Surface2, strokeCap = StrokeCap.Round)
    }
}

@Composable
private fun RankBar(label: String, v: Long, max: Long) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.width(130.dp), color = Tk.White, style = MaterialTheme.typography.bodySmall, maxLines = 1)
        Box(Modifier.weight(1f).height(14.dp).clip(RoundedCornerShape(7.dp)).background(Tk.Surface2)) {
            Box(Modifier.fillMaxHeight().fillMaxWidth((v.toFloat() / max.coerceAtLeast(1)).coerceIn(0.02f, 1f)).clip(RoundedCornerShape(7.dp)).background(Tk.Lime))
        }
        Text(v.toString(), Modifier.width(44.dp), color = Tk.Lime, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.End)
    }
}


/** v1.6: devices whose owner chose "Device ka naam + apps list dikhao". Tap a device to see its installed apps. */
@Composable
private fun DevicesCard() {
    var list by remember { mutableStateOf<List<com.techkadak.app.stats.SharedDevice>?>(null) }
    var err by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var open by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    fun load() {
        loading = true; err = null
        scope.launch {
            try { list = com.techkadak.app.stats.OwnerDevices.load() } catch (e: Exception) { err = e.message?.take(120) ?: "error" }
            loading = false
        }
    }
    LaunchedEffect(Unit) { load() }
    Card(tr("Devices (jinhone allow kiya)", "Devices (opted in)")) {
        val l = list
        when {
            loading && l == null -> Muted(tr("Load ho raha hai…", "Loading…"))
            err != null -> Muted(tr("Load nahi hua: ", "Could not load: ") + err)
            l.isNullOrEmpty() -> Muted(tr("Abhi kisi ne allow nahi kiya. v1.6 pe update karne ke baad users ko ek popup dikhega.", "Nobody has allowed it yet. Users see a prompt after updating to v1.6."))
            else -> {
                Muted(tr("${l.size} device. Tap karo to installed apps dikhengi.", "${l.size} devices. Tap one to see installed apps."))
                l.forEach { dv ->
                    Column(Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(12.dp)).background(Tk.Surface).clickable { open = if (open == dv.id) null else dv.id }.padding(12.dp)) {
                        Text(dv.name, color = Tk.White, style = MaterialTheme.typography.titleSmall)
                        Text("${dv.model} · Android ${dv.os} · v${dv.version} · ${dv.last} · ${dv.apps.size} apps", color = Tk.Muted, style = MaterialTheme.typography.bodySmall)
                        if (open == dv.id) Text(dv.apps.joinToString(", "), color = Tk.White, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
        }
        OutlineButton(tr("Refresh", "Refresh"), Modifier.fillMaxWidth().padding(top = 8.dp), Icons.Rounded.Refresh) { load() }
    }
}
