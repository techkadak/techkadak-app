package com.techkadak.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * v1.3: pull-to-refresh with the Tech Kadak mascot's hand.
 * Pull down at the top -> a black-hoodie sleeve with a lime cuff + glove reaches down from the top edge and
 * grabs the content, the content follows with a rubber-band stretch, a lime spinner + Hinglish line show
 * while the list refreshes, then the hand lets go and the list springs back. Original artwork, Canvas only.
 * Settings -> "Refresh animation" off = plain Material pull-to-refresh.
 */
object PullGeometry {
    /** Rubber band: 1:1 up to the threshold, then only a quarter of the extra pull. */
    fun gap(fraction: Float, thresholdPx: Float): Float {
        val f = if (fraction.isNaN()) 0f else fraction.coerceIn(0f, 3f)
        val eff = if (f <= 1f) f else 1f + (f - 1f) * 0.25f
        return eff * thresholdPx
    }
    fun grip(fraction: Float): Float = (if (fraction.isNaN()) 0f else fraction).coerceIn(0f, 1f)
    /** Minimum time the animation stays visible so a fast refresh (or offline fail) is still seen. */
    const val MIN_SHOW_MS = 1300L
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TkRefreshBox(
    refreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier.fillMaxSize(),
    content: @Composable BoxScope.() -> Unit,
) {
    if (!Tk.refreshAnim) {
        PullToRefreshBox(isRefreshing = refreshing, onRefresh = onRefresh, modifier = modifier, content = content)
        return
    }
    val threshold: Dp = 112.dp
    val state = rememberPullToRefreshState()
    var hold by remember { mutableStateOf(false) }
    val busy = refreshing || hold
    LaunchedEffect(hold) { if (hold) { delay(PullGeometry.MIN_SHOW_MS); hold = false } }
    LaunchedEffect(busy) { if (busy) state.animateToThreshold() else state.animateToHidden() }
    val haptic = LocalHapticFeedback.current
    val busyNow by rememberUpdatedState(busy)
    LaunchedEffect(state) {
        snapshotFlow { state.distanceFraction >= 1f }.distinctUntilChanged().collect { crossed ->
            if (crossed && !busyNow) runCatching { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
        }
    }
    val thrPx = with(LocalDensity.current) { threshold.toPx() }
    Box(
        modifier.clipToBounds().pullToRefresh(
            state = state, isRefreshing = busy, threshold = threshold,
            onRefresh = { hold = true; onRefresh() },
        ),
    ) {
        Box(Modifier.fillMaxSize().graphicsLayer { translationY = PullGeometry.gap(state.distanceFraction, thrPx) }) { content() }
        MascotHandIndicator(state, busy, threshold, thrPx)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BoxScope.MascotHandIndicator(state: PullToRefreshState, busy: Boolean, threshold: Dp, thrPx: Float) {
    val still = reduceMotion()
    val inf = rememberInfiniteTransition(label = "tkpull")
    val swing by inf.animateFloat(-1f, 1f, infiniteRepeatable(tween(520, easing = androidx.compose.animation.core.FastOutSlowInEasing), RepeatMode.Reverse), label = "swing")
    val spin by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "spin")
    val ink = Tk.Lime
    val sleeve = if (Tk.isLight) Color(0xFF111311) else Color(0xFF0C0E0C)
    val rim = if (Tk.isLight) Color(0xFF111311) else ink.copy(alpha = 0.60f)
    val glove = Color(0xFFF6F8F1)
    val outline = Color(0xFF0A0A0A)
    val onInk = Tk.OnLime
    val glow = ink.copy(alpha = if (Tk.isLight) 0.18f else 0.28f)
    Canvas(Modifier.align(Alignment.TopCenter).fillMaxWidth().height(threshold * 1.6f)) {
        val f = state.distanceFraction
        if (f <= 0.001f && !busy) return@Canvas
        val u = 1.dp.toPx()
        val gap = PullGeometry.gap(f, thrPx)
        val grip = if (busy) 1f else PullGeometry.grip(f)
        val cx = size.width / 2f
        val tipY = gap + 2f * u
        val angle = if (busy && !still) swing * 5f else 0f
        // soft glow behind the hand
        drawCircle(Brush.radialGradient(listOf(glow, Color.Transparent), Offset(cx, tipY - 40 * u), 90 * u), 90 * u, Offset(cx, tipY - 40 * u))
        rotate(angle, pivot = Offset(cx, -30 * u)) {
            drawMascotHand(cx, tipY - (if (busy && !still) (swing + 1f) * 3f * u else 0f), u, grip, sleeve, rim, ink, glove, outline, onInk)
        }
    }
    // Hinglish status line + spinner, right of the hand, centred in the gap
    val fNow = state.distanceFraction
    Row(
        Modifier.align(Alignment.TopCenter).fillMaxWidth().height(threshold)
            .graphicsLayer {
                val g = PullGeometry.gap(state.distanceFraction, thrPx)
                translationY = g / 2f - thrPx / 2f
                alpha = (state.distanceFraction * 1.4f - 0.35f).coerceIn(0f, 1f)
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.weight(1f))
        Row(Modifier.weight(1f).padding(start = 46.dp, end = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (busy) {
                Canvas(Modifier.size(20.dp)) {
                    val sw = 3.dp.toPx()
                    drawArc(ink.copy(alpha = 0.22f), 0f, 360f, false, style = Stroke(sw))
                    drawArc(ink, spin, 100f, false, style = Stroke(sw, cap = StrokeCap.Round))
                }
                Spacer(Modifier.width(8.dp))
            }
            Text(
                when {
                    busy -> tr("Naye apps dhoondh rahi hun…", "Looking for new apps…")
                    fNow >= 1f -> tr("Chhod do, ho jayega!", "Let go to refresh!")
                    else -> tr("Neeche kheencho…", "Keep pulling…")
                },
                color = Tk.White, style = MaterialTheme.typography.labelSmall, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 2,
            )
        }
    }
}

/** Mascot hand: hoodie sleeve + lime cuff + cartoon glove. Fingertips end at [tipY]. */
private fun DrawScope.drawMascotHand(
    cx: Float, tipY: Float, u: Float, grip: Float,
    sleeve: Color, rim: Color, lime: Color, glove: Color, outline: Color, onLime: Color,
) {
    val sw = 2f * u
    // sleeve (comes from far above the top edge)
    val sleeveW = 56f * u
    val wristY = tipY - 66f * u
    drawRoundRect(sleeve, Offset(cx - sleeveW / 2, wristY - 400f * u), Size(sleeveW, 400f * u), CornerRadius(10f * u))
    drawRoundRect(rim, Offset(cx - sleeveW / 2, wristY - 400f * u), Size(sleeveW, 400f * u), CornerRadius(10f * u), style = Stroke(1.5f * u))
    // lime hoodie stripe
    drawRoundRect(lime, Offset(cx + 12f * u, wristY - 400f * u), Size(4f * u, 400f * u), CornerRadius(2f * u))
    // lime ribbed cuff
    val cuffH = 15f * u
    drawRoundRect(lime, Offset(cx - sleeveW / 2 - 3f * u, wristY - cuffH / 2), Size(sleeveW + 6f * u, cuffH), CornerRadius(6f * u))
    for (i in 1..6) {
        val x = cx - sleeveW / 2 + i * (sleeveW / 7f)
        drawLine(onLime.copy(alpha = 0.28f), Offset(x, wristY - cuffH / 2 + 3f * u), Offset(x, wristY + cuffH / 2 - 3f * u), 1.2f * u)
    }
    // thumb
    rotate(-38f, pivot = Offset(cx - 22f * u, tipY - 40f * u)) {
        drawRoundRect(glove, Offset(cx - 33f * u, tipY - 46f * u), Size(13f * u, 26f * u), CornerRadius(6.5f * u))
        drawRoundRect(outline, Offset(cx - 33f * u, tipY - 46f * u), Size(13f * u, 26f * u), CornerRadius(6.5f * u), style = Stroke(sw))
    }
    // four stubby fingers; they close in toward the middle as the grip builds
    val xs = floatArrayOf(-16.5f, -5.5f, 5.5f, 16.5f)
    val lens = floatArrayOf(24f, 30f, 30f, 24f)
    for (i in 0..3) {
        val x = cx + xs[i] * u * (1f - 0.28f * grip) - 5f * u
        val len = lens[i] * u
        val top = tipY - len
        drawRoundRect(glove, Offset(x, top), Size(10f * u, len), CornerRadius(5f * u))
        drawRoundRect(outline, Offset(x, top), Size(10f * u, len), CornerRadius(5f * u), style = Stroke(sw))
    }
    // palm
    val palmTop = wristY + 6f * u
    val palmH = 34f * u
    drawRoundRect(glove, Offset(cx - 25f * u, palmTop), Size(50f * u, palmH), CornerRadius(15f * u))
    drawRoundRect(outline, Offset(cx - 25f * u, palmTop), Size(50f * u, palmH), CornerRadius(15f * u), style = Stroke(sw))
    // lime accents on the back of the glove: TK dot + knuckle line
    drawCircle(lime, 6f * u, Offset(cx, palmTop + palmH * 0.5f))
    drawCircle(outline, 6f * u, Offset(cx, palmTop + palmH * 0.5f), style = Stroke(1.6f * u))
    drawLine(outline.copy(alpha = 0.35f), Offset(cx - 11f * u, palmTop + palmH - 4f * u), Offset(cx + 11f * u, palmTop + palmH - 4f * u), 1.4f * u, StrokeCap.Round)
}
