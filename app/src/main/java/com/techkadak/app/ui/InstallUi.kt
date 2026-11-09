package com.techkadak.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.techkadak.app.data.AppItem
import com.techkadak.app.install.ApkSource
import com.techkadak.app.install.DlItem
import com.techkadak.app.install.DlStatus
import com.techkadak.app.install.ResolvedApk
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/* ───────────── "Kadak Bot" — Tech Kadak ka apna robo (Canvas se bana, koi stock character nahi) ───────────── */

enum class BotMood { WORKING, HAPPY, SAD, SLEEPY }

/**
 * Robo progress track pe chalta hai aur peeth pe ek dabba (box) le jaata hai jo lime se bharta hai.
 * HAPPY = uchhal + ✅, SAD = udaas aankhen + jhuka antenna, SLEEPY = pause / net ka intezaar (zZ).
 * [clock] test me fixed frame ke liye.
 */
@Composable
fun KadakBot(mood: BotMood, progress: Float, modifier: Modifier = Modifier, clock: Float? = null, showTrack: Boolean = true) {
    val inf = rememberInfiniteTransition(label = "bot")
    val live by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart), label = "t")
    val t = clock ?: live
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), tween(350), label = "p")
    val tm = rememberTextMeasurer()
    val lime = Tk.Lime; val ink = Color(0xFF0B0B0B); val line = Tk.Line; val glow = Tk.Glow
    val body = if (Tk.isLight) Color(0xFF111111) else Color(0xFF151515)
    Canvas(modifier) {
        val u = size.height / 100f // 1 unit
        val trackY = size.height - 8 * u
        val botW = 46 * u
        val margin = 6 * u
        val x0 = margin + (size.width - botW - 2 * margin) * (if (mood == BotMood.HAPPY) 1f else p)
        val walk = sin(t * 2 * PI).toFloat()
        val bob = when (mood) {
            BotMood.WORKING -> abs(walk) * 3 * u
            BotMood.HAPPY -> abs(sin(t * PI).toFloat()) * 18 * u
            BotMood.SLEEPY -> sin(t * 2 * PI).toFloat() * 1.2f * u
            BotMood.SAD -> 0f
        }
        if (showTrack) {
            drawRoundRect(line, Offset(margin, trackY), Size(size.width - 2 * margin, 5 * u), CornerRadius(3 * u))
            val fillW = (size.width - 2 * margin) * (if (mood == BotMood.HAPPY) 1f else p)
            if (fillW > 0) {
                drawRoundRect(glow, Offset(margin, trackY - 2 * u), Size(fillW, 9 * u), CornerRadius(5 * u))
                drawRoundRect(lime, Offset(margin, trackY), Size(fillW, 5 * u), CornerRadius(3 * u))
            }
        }
        val baseY = trackY - 4 * u - bob
        translate(left = x0) {
            val tilt = if (mood == BotMood.SAD) -6f else if (mood == BotMood.WORKING) walk * 2f else 0f
            rotate(tilt, pivot = Offset(botW / 2, baseY)) {
                drawBot(u, botW, baseY, mood, t, walk, p, lime, ink, body, glow, tm)
            }
        }
        // ✅ khushi wala badge
        if (mood == BotMood.HAPPY) {
            val c = Offset(x0 - 12 * u, trackY - 58 * u - bob * 0.5f)
            drawCircle(glow, 15 * u, c); drawCircle(lime, 11 * u, c)
            drawLine(ink, Offset(c.x - 5 * u, c.y), Offset(c.x - 1.5f * u, c.y + 4 * u), 2.6f * u, StrokeCap.Round)
            drawLine(ink, Offset(c.x - 1.5f * u, c.y + 4 * u), Offset(c.x + 5.5f * u, c.y - 4 * u), 2.6f * u, StrokeCap.Round)
            for (i in 0 until 5) { // chhote sparkle
                val a = (i * 72 + t * 90) * PI / 180
                drawCircle(lime.copy(alpha = 0.7f), 1.6f * u, Offset(c.x + (17 * u) * kotlin.math.cos(a).toFloat(), c.y + (17 * u) * sin(a).toFloat()))
            }
        }
        if (mood == BotMood.SLEEPY) {
            val zx = x0 + botW + 2 * u; val zy = baseY - 62 * u
            drawText(tm, "z", Offset(zx, zy + 6 * u - t * 4 * u), TextStyle(color = lime.copy(alpha = 0.9f - t * 0.5f), fontSize = (9 * u / density).sp, fontWeight = FontWeight.Black))
            drawText(tm, "Z", Offset(zx + 7 * u, zy - 6 * u - t * 4 * u), TextStyle(color = lime.copy(alpha = 0.6f), fontSize = (12 * u / density).sp, fontWeight = FontWeight.Black))
        }
    }
}

private fun DrawScope.drawBot(
    u: Float, w: Float, baseY: Float, mood: BotMood, t: Float, walk: Float, p: Float,
    lime: Color, ink: Color, body: Color, glow: Color, tm: androidx.compose.ui.text.TextMeasurer,
) {
    val cx = w / 2
    // pair
    val legH = 9 * u
    val step = if (mood == BotMood.WORKING) walk * 4 * u else 0f
    drawRoundRect(body, Offset(cx - 11 * u + step, baseY - legH), Size(7 * u, legH + 2 * u), CornerRadius(3 * u))
    drawRoundRect(body, Offset(cx + 4 * u - step, baseY - legH), Size(7 * u, legH + 2 * u), CornerRadius(3 * u))
    // dhad (body)
    val bodyTop = baseY - legH - 26 * u
    drawRoundRect(body, Offset(cx - 17 * u, bodyTop), Size(34 * u, 27 * u), CornerRadius(9 * u))
    drawRoundRect(lime.copy(alpha = 0.35f), Offset(cx - 17 * u, bodyTop), Size(34 * u, 27 * u), CornerRadius(9 * u), style = Stroke(1.2f * u))
    // TK chest badge
    drawRoundRect(lime, Offset(cx - 9 * u, bodyTop + 7 * u), Size(18 * u, 12 * u), CornerRadius(3.5f * u))
    val tl = tm.measure("TK", TextStyle(color = ink, fontSize = (8.5f * u / density).sp, fontWeight = FontWeight.Black))
    drawText(tl, topLeft = Offset(cx - tl.size.width / 2f, bodyTop + 13 * u - tl.size.height / 2f))
    // haath + dabba (WORKING: peeth pe dabba jo bharta hai)
    if (mood == BotMood.WORKING || mood == BotMood.SLEEPY) {
        val bx = cx - 34 * u; val by = bodyTop - 2 * u; val bs = 20 * u
        drawLine(body, Offset(cx - 16 * u, bodyTop + 9 * u), Offset(bx + bs - 2 * u, by + bs * 0.6f), 4 * u, StrokeCap.Round)
        drawRoundRect(Color(0xFF1E1E1E), Offset(bx, by), Size(bs, bs), CornerRadius(4 * u))
        val fh = bs * p
        if (fh > 0) drawRoundRect(lime, Offset(bx, by + bs - fh), Size(bs, fh), CornerRadius(4 * u))
        drawRoundRect(lime.copy(alpha = 0.6f), Offset(bx, by), Size(bs, bs), CornerRadius(4 * u), style = Stroke(1.2f * u))
        drawLine(lime.copy(alpha = 0.5f), Offset(bx + bs / 2, by), Offset(bx + bs / 2, by + bs), 1f * u)
        drawLine(body, Offset(cx + 16 * u, bodyTop + 9 * u), Offset(cx + 22 * u, bodyTop + 17 * u + walk * 2 * u), 4 * u, StrokeCap.Round)
    } else if (mood == BotMood.HAPPY) {
        drawLine(body, Offset(cx - 16 * u, bodyTop + 8 * u), Offset(cx - 25 * u, bodyTop - 6 * u), 4 * u, StrokeCap.Round)
        drawLine(body, Offset(cx + 16 * u, bodyTop + 8 * u), Offset(cx + 25 * u, bodyTop - 6 * u), 4 * u, StrokeCap.Round)
    } else {
        drawLine(body, Offset(cx - 16 * u, bodyTop + 9 * u), Offset(cx - 20 * u, bodyTop + 22 * u), 4 * u, StrokeCap.Round)
        drawLine(body, Offset(cx + 16 * u, bodyTop + 9 * u), Offset(cx + 20 * u, bodyTop + 22 * u), 4 * u, StrokeCap.Round)
    }
    // sir (head)
    val headTop = bodyTop - 25 * u
    drawRoundRect(body, Offset(cx - 18 * u, headTop), Size(36 * u, 23 * u), CornerRadius(10 * u))
    drawRoundRect(lime.copy(alpha = 0.45f), Offset(cx - 18 * u, headTop), Size(36 * u, 23 * u), CornerRadius(10 * u), style = Stroke(1.2f * u))
    // antenna
    val droop = if (mood == BotMood.SAD) 7 * u else 0f
    val antTip = Offset(cx + droop, headTop - 9 * u + droop * 0.6f)
    drawLine(body, Offset(cx, headTop), antTip, 2.4f * u, StrokeCap.Round)
    val pulse = if (mood == BotMood.SAD) 0.4f else 0.6f + 0.4f * sin(t * 2 * PI).toFloat()
    drawCircle(glow, 6 * u * pulse + 2 * u, antTip)
    drawCircle(if (mood == BotMood.SAD) lime.copy(alpha = 0.5f) else lime, 3 * u, antTip)
    // aankhen
    val eyeY = headTop + 11.5f * u
    val eyes = listOf(cx - 8 * u, cx + 8 * u)
    when (mood) {
        BotMood.WORKING -> {
            val blink = if (t in 0.46f..0.54f) 0.15f else 1f
            eyes.forEach { ex ->
                drawCircle(glow, 6 * u, Offset(ex, eyeY))
                drawRoundRect(lime, Offset(ex - 3.2f * u, eyeY - 4.5f * u * blink), Size(6.4f * u, 9 * u * blink), CornerRadius(3 * u))
            }
        }
        BotMood.HAPPY -> eyes.forEach { ex ->
            drawCircle(glow, 6 * u, Offset(ex, eyeY))
            drawArc(lime, 200f, 140f, false, Offset(ex - 4.5f * u, eyeY - 2 * u), Size(9 * u, 8 * u), style = Stroke(2.4f * u, cap = StrokeCap.Round))
        }
        BotMood.SAD -> eyes.forEachIndexed { i, ex ->
            drawArc(lime.copy(alpha = 0.8f), 20f, 140f, false, Offset(ex - 4.5f * u, eyeY - 5 * u), Size(9 * u, 7 * u), style = Stroke(2.2f * u, cap = StrokeCap.Round))
            if (i == 1) drawCircle(Color(0xFF8FD3FF), 1.8f * u, Offset(ex + 1 * u, eyeY + 5 * u + t * 3 * u)) // aansu
        }
        BotMood.SLEEPY -> eyes.forEach { ex ->
            drawLine(lime, Offset(ex - 3.5f * u, eyeY + 1 * u), Offset(ex + 3.5f * u, eyeY + 1 * u), 2.2f * u, StrokeCap.Round)
        }
    }
    // muh (mouth)
    when (mood) {
        BotMood.HAPPY -> drawArc(lime, 20f, 140f, false, Offset(cx - 4 * u, headTop + 14 * u), Size(8 * u, 5 * u), style = Stroke(1.6f * u, cap = StrokeCap.Round))
        BotMood.SAD -> drawArc(lime.copy(alpha = 0.7f), 200f, 140f, false, Offset(cx - 4 * u, headTop + 17 * u), Size(8 * u, 5 * u), style = Stroke(1.6f * u, cap = StrokeCap.Round))
        else -> drawLine(lime.copy(alpha = 0.7f), Offset(cx - 2.5f * u, headTop + 18 * u), Offset(cx + 2.5f * u, headTop + 18 * u), 1.6f * u, StrokeCap.Round)
    }
}

/* ───────────── Detail page: Install card ───────────── */

/** Detail page ka state: source + live preview (size/version) + download + phone pe installed? */
data class InstallUiState(
    val source: ApkSource,
    val preview: ResolvedApk? = null,
    val previewLoading: Boolean = false,
    val previewError: String? = null,
    val dl: DlItem? = null,
    val installedVersion: String? = null,
    val installed: Boolean = false,
    val hasUpdate: Boolean = false,
)

class InstallActions(
    val onDownload: () -> Unit = {}, val onPause: () -> Unit = {}, val onResume: () -> Unit = {}, val onCancel: () -> Unit = {},
    val onInstall: () -> Unit = {}, val onOpen: () -> Unit = {}, val onPlay: () -> Unit = {}, val onLink: (String) -> Unit = {},
    val onDownloads: () -> Unit = {},
)

private fun mb(b: Long) = if (b <= 0) "?" else "%.1f MB".format(b / 1_048_576f)

@Composable
fun InstallCard(s: InstallUiState, a: InstallActions, modifier: Modifier = Modifier, botClock: Float? = null) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Tk.Surface).border(1.dp, Tk.Line, RoundedCornerShape(22.dp)).padding(16.dp),
    ) {
        // source chip + size
        Row(verticalAlignment = Alignment.CenterVertically) {
            val icon = when (s.source.kind) { ApkSource.Kind.APK -> Icons.Rounded.VerifiedUser; ApkSource.Kind.PLAY -> Icons.Rounded.Shop; ApkSource.Kind.WEB -> Icons.Rounded.Computer }
            Icon(icon, null, tint = Tk.Lime, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(s.source.sourceLabel, color = Tk.White, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.weight(1f))
            val meta = when {
                s.source.kind != ApkSource.Kind.APK -> null
                s.dl?.total?.let { it > 0 } == true -> mb(s.dl.total)
                s.preview != null -> listOfNotNull(s.preview.versionName?.let { "v$it" }, s.preview.size.takeIf { it > 0 }?.let(::mb)).joinToString(" • ")
                s.previewLoading -> tr("size dekh rahe…", "checking size…")
                else -> null
            }
            meta?.let { Text(it, color = Tk.Muted, style = MaterialTheme.typography.labelMedium) }
        }
        Spacer(Modifier.height(12.dp))
        val dl = s.dl
        when {
            s.source.kind == ApkSource.Kind.PLAY -> {
                if (s.installed) BigButton(tr("OPEN", "OPEN"), Icons.AutoMirrored.Rounded.OpenInNew, a.onOpen)
                else BigButton(tr("Play Store pe kholo", "Open in Play Store"), Icons.Rounded.Shop, a.onPlay)
                Hint(tr("Ye app sirf Play Store pe hai — ek tap me wahan khulega.", "This app is only on the Play Store — opens there in one tap."))
            }
            s.source.kind == ApkSource.Kind.WEB -> {
                BigButton(tr("PC / Web ke liye — official link", "For PC / Web — official link"), Icons.Rounded.Computer, { s.source.link?.let(a.onLink) }, outlined = true)
                Hint(tr("Iska Android APK nahi hai. Official site khulegi.", "No Android APK for this one. Opens the official site."))
            }
            dl != null && dl.status in listOf(DlStatus.QUEUED, DlStatus.RUNNING, DlStatus.WAITING_NET, DlStatus.PAUSED) -> {
                val mood = if (dl.status == DlStatus.RUNNING || dl.status == DlStatus.QUEUED) BotMood.WORKING else BotMood.SLEEPY
                KadakBot(mood, dl.progress, Modifier.fillMaxWidth().height(110.dp), clock = botClock)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${(dl.progress * 100).toInt()}%", color = Tk.Lime, fontFamily = Display, fontWeight = FontWeight.Black, fontSize = 22.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        when (dl.status) {
                            DlStatus.QUEUED -> tr("Line me hai…", "Queued…")
                            DlStatus.WAITING_NET -> tr("Net ka intezaar — wahi se chalega", "Waiting for network — will resume")
                            DlStatus.PAUSED -> tr("Ruka hua", "Paused")
                            else -> "${mb(dl.done)} / ${mb(dl.total)}"
                        },
                        color = Tk.Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f),
                    )
                    if (dl.status == DlStatus.PAUSED) RoundAction(Icons.Rounded.PlayArrow, tr("Chalao", "Resume"), a.onResume)
                    else RoundAction(Icons.Rounded.Pause, tr("Roko", "Pause"), a.onPause)
                    Spacer(Modifier.width(8.dp))
                    RoundAction(Icons.Rounded.Close, tr("Cancel", "Cancel"), a.onCancel)
                }
                Hint(tr("App band kar do tab bhi download chalta rahega — notification me % dikhega.", "Close the app and it keeps downloading — progress shows in notifications."))
            }
            dl != null && dl.status == DlStatus.FAILED -> {
                KadakBot(BotMood.SAD, dl.progress, Modifier.fillMaxWidth().height(110.dp), clock = botClock)
                Spacer(Modifier.height(6.dp))
                Text(dl.error ?: tr("Download nahi hua", "Download failed"), color = Tk.White, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(10.dp))
                Row { BigButton(tr("DOBARA TRY", "RETRY"), Icons.Rounded.Refresh, a.onResume, Modifier.weight(1f)); Spacer(Modifier.width(8.dp)); RoundAction(Icons.Rounded.Close, "Cancel", a.onCancel) }
            }
            dl != null && dl.status == DlStatus.DONE && !(s.installed && !s.hasUpdate) -> {
                KadakBot(BotMood.HAPPY, 1f, Modifier.fillMaxWidth().height(110.dp), clock = botClock)
                Spacer(Modifier.height(8.dp))
                BigButton(if (s.installed) tr("UPDATE INSTALL KARO", "INSTALL UPDATE") else tr("INSTALL KARO", "INSTALL"), Icons.Rounded.InstallMobile, a.onInstall)
                Hint(tr("Android ek baar \"Install\" poochega — ye har Play-Store-bahar wale app me hota hai.", "Android asks for one \"Install\" tap — true for every app outside the Play Store."))
            }
            s.installed && s.hasUpdate -> {
                Row {
                    BigButton(tr("UPDATE", "UPDATE"), Icons.Rounded.SystemUpdate, a.onDownload, Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    BigButton(tr("OPEN", "OPEN"), Icons.AutoMirrored.Rounded.OpenInNew, a.onOpen, Modifier.weight(1f), outlined = true)
                }
                s.installedVersion?.let { Hint(tr("Phone pe v$it hai — naya v${s.preview?.versionName ?: "?"} aaya hai", "You have v$it — v${s.preview?.versionName ?: "?"} is out")) }
            }
            s.installed -> {
                BigButton(tr("OPEN", "OPEN"), Icons.AutoMirrored.Rounded.OpenInNew, a.onOpen)
                s.installedVersion?.let { Hint(tr("Phone pe installed hai (v$it) ✅", "Installed on this phone (v$it) ✅")) }
            }
            else -> {
                BigButton(
                    tr("INSTALL", "INSTALL") + (s.preview?.size?.takeIf { it > 0 }?.let { "  •  " + mb(it) } ?: ""),
                    Icons.Rounded.Download, a.onDownload,
                )
                Hint(
                    s.previewError ?: tr("APK seedha ${s.source.sourceLabel} se aayega (hum re-host nahi karte).", "The APK comes straight from ${s.source.sourceLabel} (never re-hosted)."),
                )
            }
        }
    }
}

@Composable
private fun Hint(text: String) = Text(text, Modifier.padding(top = 8.dp), color = Tk.Dim, style = MaterialTheme.typography.bodySmall)

@Composable
private fun BigButton(text: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier.fillMaxWidth(), outlined: Boolean = false) {
    Row(
        modifier.height(54.dp).clip(RoundedCornerShape(16.dp))
            .then(if (outlined) Modifier.border(1.5.dp, Tk.Lime, RoundedCornerShape(16.dp)) else Modifier.background(Tk.Lime))
            .clickable(onClick = onClick).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = if (outlined) Tk.Lime else Tk.OnLime, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, color = if (outlined) Tk.Lime else Tk.OnLime, fontWeight = FontWeight.Black, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun RoundAction(icon: ImageVector, desc: String, onClick: () -> Unit) {
    Box(Modifier.size(42.dp).clip(CircleShape).background(Tk.Surface2).border(1.dp, Tk.Line, CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, desc, tint = Tk.White, modifier = Modifier.size(20.dp))
    }
}

/* ───────────── Downloads screen (queue) ───────────── */

class DownloadsActions(
    val onPause: (String) -> Unit = {}, val onResume: (String) -> Unit = {}, val onCancel: (String) -> Unit = {},
    val onInstall: (String) -> Unit = {}, val onOpenApp: (String) -> Unit = {}, val onRemove: (String) -> Unit = {},
)

@Composable
fun DownloadsScreen(
    items: List<DlItem>, installed: (DlItem) -> Boolean, onBack: () -> Unit, act: DownloadsActions, apps: List<AppItem> = emptyList(), botClock: Float? = null,
    updatesBlock: @Composable () -> Unit = {},
) {
    Column(Modifier.fillMaxSize().background(Tk.Black)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, tr("Wapas", "Back"), tint = Tk.White) }
            Text(tr("Downloads", "Downloads"), color = Tk.White, fontFamily = Display, fontWeight = FontWeight.Black, fontSize = 22.sp)
            Spacer(Modifier.weight(1f))
            val active = items.count { it.active }
            if (active > 0) Text(tr("$active chal rahe", "$active active"), Modifier.padding(end = 16.dp).clip(RoundedCornerShape(50)).background(Tk.LimeSoft).padding(horizontal = 10.dp, vertical = 4.dp), color = Tk.Lime, style = MaterialTheme.typography.labelMedium)
        }
        updatesBlock()
        if (items.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                KadakBot(BotMood.SLEEPY, 0f, Modifier.fillMaxWidth().height(130.dp), clock = botClock, showTrack = false)
                Spacer(Modifier.height(12.dp))
                Text(tr("Abhi koi download nahi", "No downloads yet"), color = Tk.White, style = MaterialTheme.typography.titleMedium)
                Text(tr("Kisi app pe INSTALL dabao — yahan dikhega", "Tap INSTALL on any app — it shows up here"), color = Tk.Muted, style = MaterialTheme.typography.bodyMedium)
            }
            return
        }
        val sorted = items.sortedWith(compareByDescending<DlItem> { it.active }.thenByDescending { it.updatedAt })
        val icons = remember(apps) { apps.associateBy { it.slug } }
        LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(sorted, key = { it.slug }) { d ->
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Tk.Surface).border(1.dp, if (d.active) Tk.LimeBorder else Tk.Line, RoundedCornerShape(18.dp)).padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val app = icons[d.slug]
                        if (app != null) AppIcon(app, 46.dp) else Box(Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(Tk.Surface2))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(d.name, color = Tk.White, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                listOfNotNull(
                                    when (d.status) {
                                        DlStatus.RUNNING -> "${(d.progress * 100).toInt()}% • ${mb(d.done)} / ${mb(d.total)}"
                                        DlStatus.QUEUED -> tr("Line me hai", "Queued")
                                        DlStatus.WAITING_NET -> tr("Net ka intezaar", "Waiting for network")
                                        DlStatus.PAUSED -> tr("Ruka hua • ${(d.progress * 100).toInt()}%", "Paused • ${(d.progress * 100).toInt()}%")
                                        DlStatus.DONE -> if (installed(d)) tr("Installed ✅", "Installed ✅") else tr("Ready • ${mb(d.total)}", "Ready • ${mb(d.total)}")
                                        DlStatus.FAILED -> d.error ?: tr("Ruk gaya", "Failed")
                                    },
                                    d.sourceLabel,
                                ).joinToString(" • "),
                                color = if (d.status == DlStatus.FAILED) Color(0xFFFF8A80) else Tk.Muted, style = MaterialTheme.typography.bodySmall, maxLines = 2,
                            )
                        }
                        when (d.status) {
                            DlStatus.RUNNING, DlStatus.QUEUED, DlStatus.WAITING_NET -> { RoundAction(Icons.Rounded.Pause, "Pause") { act.onPause(d.slug) }; Spacer(Modifier.width(6.dp)); RoundAction(Icons.Rounded.Close, "Cancel") { act.onCancel(d.slug) } }
                            DlStatus.PAUSED, DlStatus.FAILED -> { RoundAction(Icons.Rounded.PlayArrow, "Resume") { act.onResume(d.slug) }; Spacer(Modifier.width(6.dp)); RoundAction(Icons.Rounded.Close, "Cancel") { act.onCancel(d.slug) } }
                            DlStatus.DONE -> {
                                val inst = installed(d)
                                Text(
                                    if (inst) tr("OPEN", "OPEN") else tr("INSTALL", "INSTALL"),
                                    Modifier.clip(RoundedCornerShape(50)).background(Tk.Lime).clickable { if (inst) d.packageName?.let(act.onOpenApp) else act.onInstall(d.slug) }.padding(horizontal = 14.dp, vertical = 8.dp),
                                    color = Tk.OnLime, fontWeight = FontWeight.Black, fontSize = 13.sp,
                                )
                                Spacer(Modifier.width(6.dp)); RoundAction(Icons.Rounded.DeleteOutline, tr("Hatao", "Remove")) { act.onRemove(d.slug) }
                            }
                        }
                    }
                    if (d.status != DlStatus.DONE) {
                        Spacer(Modifier.height(8.dp))
                        KadakBot(
                            when (d.status) { DlStatus.FAILED -> BotMood.SAD; DlStatus.PAUSED, DlStatus.WAITING_NET -> BotMood.SLEEPY; else -> BotMood.WORKING },
                            d.progress, Modifier.fillMaxWidth().height(64.dp), clock = botClock,
                        )
                    }
                }
            }
        }
    }
}

/** "Updates" — Tech Kadak se aaye installed apps jinka naya version hai. */
@Composable
fun UpdatesBlock(updates: List<MainViewModel.PendingUpdate>, checking: Boolean, onUpdateAll: () -> Unit, onOpen: (AppItem) -> Unit) {
    Column(Modifier.padding(horizontal = 14.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Tk.Surface).border(1.dp, if (updates.isNotEmpty()) Tk.LimeBorder else Tk.Line, RoundedCornerShape(18.dp)).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.SystemUpdate, null, tint = Tk.Lime)
            Spacer(Modifier.width(8.dp))
            Text(
                when {
                    checking -> tr("Updates check ho rahe…", "Checking for updates…")
                    updates.isEmpty() -> tr("Sab apps up to date ✅", "All apps up to date ✅")
                    else -> tr("${updates.size} update aaye", "${updates.size} updates available")
                },
                Modifier.weight(1f), color = Tk.White, style = MaterialTheme.typography.titleSmall,
            )
            if (updates.isNotEmpty()) Text(
                tr("Sab update karo", "Update all"),
                Modifier.clip(RoundedCornerShape(50)).background(Tk.Lime).clickable(onClick = onUpdateAll).padding(horizontal = 12.dp, vertical = 7.dp),
                color = Tk.OnLime, fontWeight = FontWeight.Black, fontSize = 12.sp,
            )
        }
        updates.forEach { u ->
            Row(Modifier.padding(top = 10.dp).fillMaxWidth().clickable { onOpen(u.app) }, verticalAlignment = Alignment.CenterVertically) {
                AppIcon(u.app, 34.dp)
                Spacer(Modifier.width(10.dp))
                Text(u.app.name, Modifier.weight(1f), color = Tk.White, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                Text("v${u.from ?: "?"} → v${u.to ?: "?"}", color = Tk.Lime, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
    Spacer(Modifier.height(10.dp))
}
