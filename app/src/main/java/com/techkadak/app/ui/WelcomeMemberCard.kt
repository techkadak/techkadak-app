package com.techkadak.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.techkadak.app.R
import com.techkadak.app.member.MemberColor
import com.techkadak.app.member.MemberStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import kotlin.math.cos
import kotlin.math.sin

/**
 * v1.5 Welcome member card. Full-screen dim, the card spins in in 3D, a gloss sheen crosses it,
 * it flips to the back on its own after a beat, and a tap flips it either way.
 * Front: TK badge, name, member number and join date. Back: Hinglish welcome and the credit line.
 */
@Composable
fun WelcomeMemberCard(name: String, number: Long, since: String, onDone: () -> Unit, autoFlip: Boolean = true) {
    val style = remember(number) { MemberColor.of(number) }
    val spin = remember { Animatable(0f) }
    val flip = remember { Animatable(0f) } // 0 = front, 180 = back
    val scope = rememberCoroutineScope()
    var flipped by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    val reduce = remember { android.provider.Settings.Global.getFloat(ctx.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
    LaunchedEffect(Unit) {
        if (reduce) { spin.snapTo(1f); return@LaunchedEffect }
        spin.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow))
        if (autoFlip) { delay(1300); flipped = true; flip.animateTo(180f, tween(700, easing = FastOutSlowInEasing)) }
        else if (reduce) Unit
    }
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.72f * spin.value.coerceIn(0f, 1f))).clickable(
            interactionSource = remember { MutableInteractionSource() }, indication = null,
        ) { onDone() },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.width(310.dp).height(196.dp)
                .graphicsLayer {
                    val s = spin.value
                    rotationY = (1f - s) * -120f + flip.value
                    cameraDistance = 14f * density
                    scaleX = 0.82f + 0.18f * s; scaleY = scaleX
                    alpha = s.coerceIn(0f, 1f)
                }
                .clip(RoundedCornerShape(22.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    if (spin.value < 0.9f) return@clickable
                    flipped = !flipped
                    val target = if (flipped) 180f else 0f
                    scope.launch { flip.animateTo(target, tween(600, easing = FastOutSlowInEasing)) }
                },
        ) {
            val showBack = flip.value > 90f
            if (!showBack) CardFace(style, number, name, since, back = false) else Box(Modifier.graphicsLayer { rotationY = 180f }) { CardFace(style, number, name, since, back = true) }
        }
    }
}

@Composable
internal fun CardFace(style: MemberStyle, number: Long, name: String, since: String, back: Boolean) {
    val ink = Color(style.textOn)
    Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(style.colorA), Color(style.colorB)))).drawWithContent {
        drawContent()
        drawPattern(style.patternIdx, ink.copy(alpha = 0.10f))
        // gloss: a soft white band across the top third
        drawRect(Brush.linearGradient(listOf(Color.White.copy(alpha = 0.22f), Color.Transparent), start = Offset(0f, 0f), end = Offset(size.width, size.height * 0.6f)))
    }) {
        if (!back) {
            Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(R.drawable.tk_logo), contentDescription = null, modifier = Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)))
                    Spacer(Modifier.width(10.dp))
                    Text("TECH KADAK MEMBER", color = ink, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                }
                Column {
                    Text(name.ifBlank { "Dost" }, color = ink, fontFamily = Display, fontWeight = FontWeight.Black, fontSize = 26.sp, maxLines = 1)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(memberLabel(number), color = ink, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, softWrap = false)
                        Text("Since $since", color = ink, fontSize = 11.sp, maxLines = 1, softWrap = false)
                    }
                }
            }
        } else {
            Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Welcome, ${name.ifBlank { "Dost" }}! 🎉", color = ink, fontFamily = Display, fontWeight = FontWeight.Black, fontSize = 20.sp, maxLines = 2, textAlign = TextAlign.Start)
                    Spacer(Modifier.height(8.dp))
                    Text("Tech Kadak mein swagat hai. Free apps, free jawab, aur har update ek tap me. Tu ab member hai.", color = ink, fontSize = 13.sp)
                }
                Text("Created by Rohit Saini", color = ink, fontSize = 10.sp, letterSpacing = 1.2.sp)
            }
        }
    }
}

private fun memberLabel(n: Long): String = if (n >= 1_000_000L) "MEMBER #" + n.toString() else "MEMBER #" + "%04d".format(n)

/** Pattern per member: stripes, rings, dots, arcs, bars, waves (the same index always draws the same). */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPattern(idx: Int, c: Color) {
    val w = size.width; val h = size.height
    when (idx % 6) {
        0 -> { var x = -h; while (x < w) { drawLine(c, Offset(x, h), Offset(x + h, 0f), strokeWidth = 6f); x += 26f } }
        1 -> { var r = 30f; while (r < w) { drawCircle(c, r, Offset(w, 0f), style = Stroke(3f)); r += 26f } }
        2 -> { var x = 16f; while (x < w) { var y = 16f; while (y < h) { drawCircle(c, 2.5f, Offset(x, y)); y += 22f }; x += 22f } }
        3 -> { var r = 40f; while (r < w * 1.4f) { drawArc(c, 180f, 90f, false, Offset(-r + w * 0.25f, h * 0.4f - r), Size(2 * r, 2 * r), style = Stroke(2.5f)); r += 22f } }
        4 -> { var x = 0f; while (x < w) { drawRect(c, Offset(x, h - 26f), Size(8f, 26f)); x += 16f } }
        else -> { var t = 0f; while (t < w) { val p = Offset(t, h * 0.5f + sin(t / 22f) * 14f); drawCircle(c, 2f, p); t += 8f } }
    }
}

/** v1.5: compact member card on Profile. Tap replays the full welcome card. Same style as the number gives. */
@Composable
fun ProfileMemberCard(name: String, number: Long, since: String, onReplay: () -> Unit) {
    val style = remember(number) { MemberColor.of(number) }
    Box(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp).height(150.dp).clip(RoundedCornerShape(20.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onReplay),
    ) {
        CardFace(style, number, name, since, back = false)
        Text("Tap: replay ▶", Modifier.align(Alignment.BottomEnd).padding(12.dp), color = Color(style.textOn).copy(alpha = 0.8f), fontSize = 10.sp)
    }
}
