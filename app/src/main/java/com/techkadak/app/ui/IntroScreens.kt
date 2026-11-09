package com.techkadak.app.ui

import android.graphics.Paint
import android.graphics.PathMeasure
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import com.techkadak.app.R
import com.techkadak.app.data.AppItem
import com.techkadak.app.data.Catalog
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private fun seg(t: Float, a: Float, b: Float) = ((t - a) / (b - a)).coerceIn(0f, 1f)
private fun ez(x: Float) = FastOutSlowInEasing.transform(x)

/* ───────────── SPLASH: lime screen → TK mark pop → "TECH KADAK" type → dot → bounce → circular reveal ───────────── */

private const val SPLASH_MS = 5000

/** Splash ka state: progress 0..1. App content iske circle-reveal se dikhta hai. */
class SplashState(val anim: Animatable<Float, *>, val override: Float?) {
    val t: Float get() = override ?: anim.value
    val reveal: Float get() = ez(seg(t, 0.94f, 1f))
}

@Composable
fun rememberSplash(progressOverride: Float? = null, onDone: () -> Unit): SplashState {
    val ctx = LocalContext.current
    val anim = remember { Animatable(0f) }
    // "Remove animations" (system) on: static end frame ~0.8 s, no motion.
    if (progressOverride == null && reducedMotion(ctx)) {
        LaunchedEffect(Unit) { kotlinx.coroutines.delay(800); onDone() }
        return remember { SplashState(anim, 0.86f) }
    }
    LaunchedEffect(Unit) { if (progressOverride == null) { anim.animateTo(1f, tween(SPLASH_MS, easing = LinearEasing)); onDone() } }
    return remember(progressOverride) { SplashState(anim, progressOverride) }
}

private fun reducedMotion(ctx: android.content.Context): Boolean = try {
    android.provider.Settings.Global.getFloat(ctx.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
} catch (e: Exception) { false }

/**
 * Opening (v1.5): OpeningLayer draws the logo reveal; then the app opens out of a circle at the centre.
 * [content] = app (circle ke andar dikhta hai). Tap = skip.
 */
@Composable
fun SplashHost(state: SplashState?, onSkip: () -> Unit, content: @Composable () -> Unit) {
    if (state == null) { content(); return }
    val t = state.t
    val reveal = state.reveal
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wPx = constraints.maxWidth.toFloat(); val hPx = constraints.maxHeight.toFloat()
        OpeningLayer(t, onSkip)
        // App: pehle chhupa, phir centre se circle me khulta hai
        val center = Offset(wPx / 2f, hPx / 2f)
        val maxR = kotlin.math.hypot(maxOf(center.x, wPx - center.x), maxOf(center.y, hPx - center.y))
        val r = maxR * reveal
        Box(
            Modifier.fillMaxSize().graphicsLayer {
                alpha = if (reveal > 0f) 1f else 0f
                if (reveal < 1f) { clip = true; shape = RevealShape(center, r) }
            },
        ) { Box(Modifier.fillMaxSize().background(Tk.Black)) { content() } }
    }
}

/** Circle reveal: Outline.Rounded (har Android version pe clip hota hai). */
private class RevealShape(val c: Offset, val r: Float) : androidx.compose.ui.graphics.Shape {
    override fun createOutline(size: androidx.compose.ui.geometry.Size, layoutDirection: androidx.compose.ui.unit.LayoutDirection, density: androidx.compose.ui.unit.Density) =
        androidx.compose.ui.graphics.Outline.Rounded(androidx.compose.ui.geometry.RoundRect(c.x - r, c.y - r, c.x + r, c.y + r, androidx.compose.ui.geometry.CornerRadius(r)))
}

/* ───────────── ONBOARDING: 4 pages, animated glassy mockups ───────────── */

@Composable
fun OnboardingScreen(apps: List<AppItem> = emptyList(), pageOverride: Int? = null, appearOverride: Float? = null, onDone: () -> Unit) {
    val pageCount = 4
    val state = rememberPagerState(initialPage = pageOverride ?: 0) { pageCount }
    val scope = rememberCoroutineScope()
    val withIcons = remember(apps) { apps.filter { it.icon != null } }
    val titles = listOf(
        tr("Paid apps ka\nFREE jawab", "FREE answers to\npaid apps"),
        tr("Har app official\nlink se, 100% safe", "Every app from its\nofficial link"),
        tr("Roz naya app,\nkhud update", "A new app daily,\nauto-updated"),
        tr("Apni pasand\nka look", "Your look,\nyour way"),
    )
    val subs = listOf(
        tr("Mehenge apps ki jagah kaam ke free / open-source apps — check karke.", "Useful free / open-source apps instead of pricey ones — all checked."),
        tr("DOWNLOAD hamesha F-Droid / GitHub / Play Store / official site pe. Koi fake APK ya rating nahi.", "DOWNLOAD always opens F-Droid / GitHub / Play Store / the official site. No fake APKs or ratings."),
        tr("Website ka bot har din ek naya app daalta hai — app me apne aap aa jaata hai.", "The website bot adds an app every day — it shows up here by itself."),
        tr("Theme, colour, font, card style, home sections — sab tumhare haath me.", "Theme, colour, font, card style, home sections — all yours to change."),
    )
    Box(Modifier.fillMaxSize().background(Tk.Black)) {
        HorizontalPager(state, Modifier.fillMaxSize()) { i ->
            val off = (state.currentPage - i) + state.currentPageOffsetFraction
            // har page ka "appear" — page aate hi 0→1
            val appearAnim = remember { Animatable(0f) }
            LaunchedEffect(state.currentPage == i) { if (state.currentPage == i && appearOverride == null) { appearAnim.snapTo(0f); appearAnim.animateTo(1f, tween(1100, easing = LinearEasing)) } }
            val appear = appearOverride ?: appearAnim.value
            Column(Modifier.fillMaxSize().graphicsLayer { alpha = 1f - abs(off).coerceIn(0f, 1f) * 0.6f }) {
                Box(
                    Modifier.fillMaxWidth().weight(1f).padding(top = 64.dp, start = 18.dp, end = 18.dp)
                        .graphicsLayer { translationX = off * size.width * 0.35f }, // parallax
                    contentAlignment = Alignment.Center,
                ) {
                    when (i) {
                        0 -> PaidFreeMock(withIcons, appear)
                        1 -> OrbitMock(withIcons, apps.size, appear)
                        2 -> DailyMock(apps.firstOrNull(), appear)
                        else -> LookMock(appear)
                    }
                }
                Column(Modifier.fillMaxWidth().padding(start = 26.dp, end = 26.dp, bottom = 150.dp).graphicsLayer { translationX = off * size.width * 0.15f }) {
                    Text(titles[i], color = Tk.White, fontFamily = Display, fontWeight = FontWeight.Black, fontSize = 31.sp, lineHeight = 37.sp)
                    Spacer(Modifier.height(10.dp))
                    Text(subs[i], color = Tk.Muted, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        // top-right Skip
        Text(
            tr("Skip  ›", "Skip  ›"), Modifier.align(Alignment.TopEnd).systemBarsPadding().padding(12.dp).clip(RoundedCornerShape(50))
                .clickable(onClick = onDone).padding(horizontal = 14.dp, vertical = 8.dp),
            color = Tk.Muted, style = MaterialTheme.typography.labelLarge,
        )
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(horizontal = 22.dp, vertical = 22.dp)) {
            Row(Modifier.padding(start = 4.dp, bottom = 18.dp)) {
                repeat(pageCount) { i ->
                    val sel = state.currentPage == i
                    Box(Modifier.padding(end = 6.dp).height(7.dp).width(if (sel) 24.dp else 7.dp).clip(CircleShape).background(if (sel) Tk.Lime else Tk.Line))
                }
            }
            val last = state.currentPage == pageCount - 1
            Row(
                Modifier.fillMaxWidth().height(58.dp).clip(RoundedCornerShape(50)).background(if (last) Tk.Lime else Tk.White)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = ripple(color = Tk.Lime)) {
                        if (last) onDone() else scope.launch { state.animateScrollToPage(state.currentPage + 1) }
                    },
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(if (last) tr("Shuru karo", "Get started") else tr("Continue", "Continue"), color = if (last) Tk.OnLime else Tk.Black, fontFamily = Display, fontWeight = FontWeight.Black, fontSize = 16.sp)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = if (last) Tk.OnLime else Tk.Black, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun floatY(seed: Int): Float {
    val inf = rememberInfiniteTransition(label = "float$seed")
    val v by inf.animateFloat(-1f, 1f, infiniteRepeatable(tween(1800 + seed * 230, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "f")
    return v
}

@Composable
private fun Glass(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier.shadow(18.dp, RoundedCornerShape(22.dp), ambientColor = Color.Black, spotColor = Color.Black).clip(RoundedCornerShape(22.dp))
            .background(Brush.verticalGradient(listOf(Tk.Surface2.copy(alpha = 0.96f), Tk.Surface.copy(alpha = 0.92f))))
            .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.18f), Color.White.copy(alpha = 0.03f))), RoundedCornerShape(22.dp)),
        content = content,
    )
}

/** Brand ka naam kabhi nahi (ROHIT ka rule) — sirf kis tarah ke paid apps ka jawab hai. */
internal fun paidLine(a: AppItem): String {
    val r = (a.replaces ?: "").lowercase()
    val kind = listOf(
        "scan" to "scanner", "pdf" to "PDF", "video" to "video editor", "photo" to "photo editor", "image" to "photo editor",
        "vpn" to "VPN", "password" to "password manager", "note" to "notes", "music" to "music", "office" to "office", "design" to "design",
    ).firstOrNull { r.contains(it.first) }?.second
    return if (kind != null) "PAID $kind apps ka free jawab ✅" else "PAID apps ka free jawab ✅"
}

/** p1: floating app cards — "PAID ❌ → FREE ✅". */
@Composable
private fun PaidFreeMock(apps: List<AppItem>, appear: Float) {
    val picks = remember(apps) { (apps.filter { it.replaces != null } + apps).distinct().take(3) }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(Modifier.size(300.dp).background(Brush.radialGradient(listOf(Tk.Glow, Color.Transparent))))
        picks.forEachIndexed { k, a ->
            val p = ez(seg(appear, k * 0.18f, 0.55f + k * 0.18f))
            val fy = floatY(k)
            val baseY = (k - 1) * 104f
            val baseX = listOf(-26f, 30f, -12f)[k]
            Glass(Modifier.width(276.dp).graphicsLayer {
                translationY = (baseY + fy * 6f) * density + (1f - p) * 140f * density
                translationX = baseX * density; alpha = p; rotationZ = listOf(-4f, 3f, -2f)[k] * p
            }) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(a, 46.dp, 13.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(a.name, color = Tk.White, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            paidLine(a), color = Tk.Lime, style = MaterialTheme.typography.labelSmall,
                            maxLines = 2, overflow = TextOverflow.Ellipsis,
                        )
                    }
                    PriceBadge(a.pricing)
                }
            }
        }
    }
}

/** p2: real logos orbiting + "60+ apps" pill (real count). */
@Composable
private fun OrbitMock(apps: List<AppItem>, total: Int, appear: Float) {
    val inf = rememberInfiniteTransition(label = "orbit")
    val rot by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(16000, easing = LinearEasing)), label = "r")
    val icons = remember(apps) { apps.take(10) }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(Modifier.size(320.dp).background(Brush.radialGradient(listOf(Tk.Glow, Color.Transparent))))
        Canvas(Modifier.size(300.dp)) {
            drawCircle(Tk.Line, radius = size.minDimension * 0.46f, style = Stroke(1.dp.toPx()))
            drawCircle(Tk.Line, radius = size.minDimension * 0.28f, style = Stroke(1.dp.toPx()))
        }
        icons.forEachIndexed { k, a ->
            val ring = if (k % 2 == 0) 138f else 84f
            val ang = (k * 36f + rot * (if (k % 2 == 0) 1f else -1.4f)) * (PI / 180f).toFloat()
            val p = ez(seg(appear, k * 0.05f, 0.5f + k * 0.05f))
            Box(Modifier.graphicsLayer {
                translationX = cos(ang) * ring * density * p; translationY = sin(ang) * ring * density * p; alpha = p
            }) { AppIcon(a, if (k % 2 == 0) 50.dp else 40.dp, 14.dp) }
        }
        val pp = ez(seg(appear, 0.35f, 0.8f))
        Column(Modifier.graphicsLayer { scaleX = 0.6f + 0.4f * pp; scaleY = 0.6f + 0.4f * pp; alpha = pp }, horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)).background(Brush.linearGradient(listOf(Tk.LimeLight, Tk.Lime, Tk.LimeDeep))), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.VerifiedUser, null, tint = Tk.OnLime, modifier = Modifier.size(34.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "${(total / 10) * 10}+ apps", Modifier.clip(RoundedCornerShape(50)).background(Tk.White).padding(horizontal = 14.dp, vertical = 5.dp),
                color = Color.Black, fontFamily = Display, fontWeight = FontWeight.Black, fontSize = 13.sp,
            )
        }
    }
}

/** p3: "Aaj ka naya app" notification slide-in + rising lime streak. */
@Composable
private fun DailyMock(app: AppItem?, appear: Float) {
    val notif = ez(seg(appear, 0.05f, 0.45f))
    val line = ez(seg(appear, 0.3f, 1f))
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Glass(Modifier.fillMaxWidth(0.92f).height(230.dp).graphicsLayer { translationY = 40f * density }) {
            Column(Modifier.padding(16.dp)) {
                Text(tr("Roz ka naya app", "New app every day"), color = Tk.Muted, style = MaterialTheme.typography.labelMedium)
                Text("7 " + tr("din, 7 apps", "days, 7 apps"), color = Tk.White, fontFamily = Display, fontWeight = FontWeight.Black, fontSize = 20.sp)
                Spacer(Modifier.height(8.dp))
                val lime = Tk.Lime; val soft = Tk.LimeSoft; val grid = Tk.Line
                Canvas(Modifier.fillMaxWidth().weight(1f)) {
                    val pts = listOf(0.15f, 0.22f, 0.2f, 0.38f, 0.45f, 0.62f, 0.9f)
                    val step = size.width / (pts.size - 1)
                    repeat(4) { r -> val y = size.height * r / 3f; drawLine(grid, Offset(0f, y), Offset(size.width, y), 1f) }
                    val path = Path(); val fillP = Path()
                    val n = (pts.size - 1) * line
                    for (k in 0..pts.size - 1) {
                        if (k > n + 1) break
                        val kk = k.toFloat().coerceAtMost(n)
                        val i0 = kk.toInt().coerceAtMost(pts.size - 1); val fr = kk - i0
                        val v = if (i0 + 1 < pts.size) pts[i0] + (pts[i0 + 1] - pts[i0]) * fr else pts[i0]
                        val x = kk * step; val y = size.height * (1f - v)
                        if (k == 0) { path.moveTo(x, y); fillP.moveTo(x, size.height); fillP.lineTo(x, y) } else { path.lineTo(x, y); fillP.lineTo(x, y) }
                        if (k >= n) { fillP.lineTo(x, size.height); fillP.close(); drawCircle(lime, 6.dp.toPx(), Offset(x, y)); break }
                    }
                    drawPath(fillP, Brush.verticalGradient(listOf(soft.copy(alpha = 0.5f), Color.Transparent)))
                    drawPath(path, lime, style = Stroke(4.dp.toPx(), cap = StrokeCap.Round))
                }
            }
        }
        Glass(Modifier.fillMaxWidth(0.86f).graphicsLayer { translationY = (-150f - (1f - notif) * 120f) * density; alpha = notif }) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.tk_logo), null, Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("TECH KADAK • " + tr("abhi", "now"), color = Tk.Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(tr("Aaj ka naya app: ", "Today's new app: ") + (app?.name ?: "…") + " 🔥", color = Tk.White, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(app?.what ?: "", color = Tk.Muted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/** p4: theme/accent swatches morphing + mini phone preview. */
@Composable
private fun LookMock(appear: Float) {
    val inf = rememberInfiniteTransition(label = "look")
    val cyc by inf.animateFloat(0f, 4f, infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "c")
    val accents = listOf(Color(0xFF9BFF2E), Color(0xFFFF8A1F), Color(0xFF2EFFB0), Color(0xFFFFD60A))
    val i0 = cyc.toInt() % 4; val fr = ez(seg(cyc - cyc.toInt(), 0.7f, 1f))
    val acc = androidx.compose.ui.graphics.lerp(accents[i0], accents[(i0 + 1) % 4], fr)
    val lightMix = if (i0 == 2) 1f - fr else if (i0 == 1) fr else 0f
    val bg = androidx.compose.ui.graphics.lerp(Color(0xFF0E0E0E), Color(0xFFF5F6F1), lightMix)
    val fg = androidx.compose.ui.graphics.lerp(Color.White, Color(0xFF0A0A0A), lightMix)
    val p = ez(seg(appear, 0f, 0.6f))
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(Modifier.size(320.dp).background(Brush.radialGradient(listOf(acc.copy(alpha = 0.35f), Color.Transparent))))
        // mini phone
        Column(
            Modifier.width(170.dp).height(300.dp).graphicsLayer { alpha = p; translationY = (1f - p) * 80f * density; rotationZ = -5f }
                .shadow(20.dp, RoundedCornerShape(30.dp)).clip(RoundedCornerShape(30.dp)).background(bg).border(4.dp, Color(0xFF2A2A2A), RoundedCornerShape(30.dp)).padding(14.dp),
        ) {
            Box(Modifier.fillMaxWidth().height(70.dp).clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(listOf(acc.copy(alpha = 0.35f), bg))).border(1.dp, acc.copy(alpha = 0.5f), RoundedCornerShape(16.dp)))
            Spacer(Modifier.height(10.dp))
            repeat(3) {
                Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(fg.copy(alpha = 0.12f)))
                    Spacer(Modifier.width(8.dp))
                    Column { Box(Modifier.width(70.dp).height(7.dp).clip(CircleShape).background(fg.copy(alpha = 0.8f))); Spacer(Modifier.height(4.dp)); Box(Modifier.width(40.dp).height(6.dp).clip(CircleShape).background(acc)) }
                }
            }
            Spacer(Modifier.weight(1f))
            Box(Modifier.fillMaxWidth().height(26.dp).clip(CircleShape).background(acc))
        }
        // swatches
        accents.forEachIndexed { k, c ->
            val q = ez(seg(appear, 0.2f + k * 0.1f, 0.7f + k * 0.1f))
            val ang = (-60f + k * 40f) * (PI / 180f).toFloat()
            val sel = k == i0
            Box(Modifier.graphicsLayer {
                translationX = (120f + cos(ang) * 30f) * density * q; translationY = sin(ang) * 120f * density; alpha = q
                val s = if (sel) 1.25f else 1f; scaleX = s; scaleY = s
            }.size(40.dp).clip(CircleShape).background(c).border(if (sel) 3.dp else 0.dp, Color.White, CircleShape))
        }
        Glass(Modifier.graphicsLayer { translationX = -110f * density; translationY = 110f * density; alpha = ez(seg(appear, 0.4f, 0.9f)) }) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.DarkMode, null, tint = acc, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("AMOLED • Dark • Light", color = Tk.White, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
