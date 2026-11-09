package com.techkadak.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.MarkEmailUnread
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private fun seg(t: Float, a: Float, b: Float) = ((t - a) / (b - a)).coerceIn(0f, 1f)
private fun ease(x: Float) = FastOutSlowInEasing.transform(x)
private fun mix(a: Float, b: Float, t: Float) = a + (b - a) * t

/**
 * OTP / verify boxes (ROHIT ke reference jaisa, lime theme):
 * boxes ek-ek karke bharte hain (focus glow) → complete hone pe boxes circle me ghoomte hain,
 * lime hote hain, aur ek chamakte square me mil jaate hain (sparkles) = SUCCESS.
 *
 * @param slots har box ka text ("" = khali). Email mode me icons/dots.
 * @param focus kaunsa box abhi active (glow), -1 = koi nahi
 * @param success null = abhi entering; 0..1 = success animation ka progress (tests ke liye fix bhi kar sakte ho)
 */
@Composable
fun OtpBoxes(slots: List<String>, focus: Int, success: Float?, modifier: Modifier = Modifier, boxSize: Int = 54) {
    val n = slots.size
    val density = LocalDensity.current
    val box = boxSize.dp
    val spacingPx = with(density) { (boxSize + 12).dp.toPx() }
    val radiusPx = with(density) { (boxSize * 1.05f).dp.toPx() }
    val t = success ?: 0f
    val toCircle = ease(seg(t, 0f, 0.3f))
    val spin = ease(seg(t, 0.25f, 0.72f))
    val merge = ease(seg(t, 0.68f, 0.88f))
    val final = ease(seg(t, 0.82f, 1f))
    val pulse = rememberInfiniteTransition(label = "otp").animateFloat(0.35f, 1f, infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Reverse), label = "glow")

    Box(modifier.fillMaxWidth().height((boxSize * 3.4f).dp), contentAlignment = Alignment.Center) {
        if (success != null && final > 0f) {
            // sparkles
            repeat(12) { i ->
                val ang = (i / 12f) * 2 * PI.toFloat() + 0.3f
                val r = with(density) { mix(36.dp.toPx(), 92.dp.toPx(), final) }
                val a = (1f - seg(t, 0.93f, 1f) * 0.6f) * final
                Box(
                    Modifier.size(if (i % 3 == 0) 7.dp else 4.dp).graphicsLayer { translationX = cos(ang) * r; translationY = sin(ang) * r; alpha = a }
                        .clip(CircleShape).background(Tk.Lime),
                )
            }
            val s = mix(0.6f, 1f, final)
            Box(
                Modifier.size((boxSize * 1.5f).dp).graphicsLayer { scaleX = s; scaleY = s; alpha = final }
                    .shadow(24.dp, RoundedCornerShape(26.dp), ambientColor = Tk.Lime, spotColor = Tk.Lime)
                    .clip(RoundedCornerShape(26.dp)).background(Brush.linearGradient(listOf(Tk.LimeLight, Tk.Lime, Tk.LimeDeep)))
                    .border(2.dp, Tk.LimeLight, RoundedCornerShape(26.dp)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Check, null, tint = Tk.OnLime, modifier = Modifier.size((boxSize * 0.8f).dp)) }
        }
        for (i in 0 until n) {
            val rowX = (i - (n - 1) / 2f) * spacingPx
            val ang = (-PI / 2 + i * 2 * PI / n).toFloat() + spin * (1.5f * PI).toFloat()
            val r = radiusPx * (1f - merge)
            val cx = cos(ang) * r
            val cy = sin(ang) * r
            val x = mix(rowX, cx, toCircle)
            val y = mix(0f, cy, toCircle)
            val filled = slots[i].isNotEmpty()
            val limeMix = if (success != null) seg(t, 0.3f, 0.6f) else 0f
            val isFocus = success == null && i == focus
            val boxAlpha = 1f - final
            Box(
                Modifier.size(box).graphicsLayer {
                    translationX = x; translationY = y; rotationZ = spin * 360f * (if (i % 2 == 0) 1f else -1f) * 0.25f + toCircle * (i * 8f - 12f)
                    alpha = boxAlpha; val sc = if (filled && success == null) 1f else 1f; scaleX = sc; scaleY = sc
                }
                    .then(if (isFocus) Modifier.shadow((10 * pulse.value).dp, RoundedCornerShape(16.dp), ambientColor = Tk.Lime, spotColor = Tk.Lime) else Modifier)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.verticalGradient(listOf(lerp(Tk.Surface2, Tk.Lime, limeMix * 0.9f), lerp(Tk.Surface, Tk.LimeDeep, limeMix * 0.9f))))
                    .border(
                        if (isFocus) 2.dp else 1.dp,
                        when { limeMix > 0f -> lerp(Tk.Line, Tk.LimeLight, limeMix); isFocus -> Tk.Lime.copy(alpha = 0.5f + 0.5f * pulse.value); filled -> Tk.LimeBorder; else -> Tk.Line },
                        RoundedCornerShape(16.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (filled) Text(slots[i], color = lerp(Tk.White, Tk.OnLime, limeMix), fontFamily = Display, fontWeight = FontWeight.Black, fontSize = (boxSize * 0.4f).sp)
                else if (isFocus) Box(Modifier.width(2.dp).height((boxSize * 0.42f).dp).graphicsLayer { alpha = pulse.value }.background(Tk.Lime))
            }
        }
    }
}

/** Success animation chalao jab [done] true ho jaye. Returns progress (null = abhi nahi). */
@Composable
fun rememberSuccessProgress(done: Boolean, durationMs: Int = 1800): Float? {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(done) {
        if (done) anim.animateTo(1f, tween(durationMs, easing = LinearEasing)) else anim.snapTo(0f)
    }
    return if (done) anim.value else null
}

/** Notification jaisa chip: "MESSAGE · OTP" + Paste (phone) ya "Inbox kholo" (email). */
@Composable
fun AutofillChip(title: String, body: String, action: String, onAction: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Tk.Surface2).border(1.dp, Tk.Line, RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(Tk.LimeSoft), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.MarkEmailUnread, null, tint = Tk.Lime, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title.uppercase(), color = Tk.Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text(body, color = Tk.White, style = MaterialTheme.typography.bodySmall, maxLines = 1)
        }
        Text(action, Modifier.clip(RoundedCornerShape(50)).background(Tk.White).clickable(onClick = onAction).padding(horizontal = 12.dp, vertical = 6.dp), color = Tk.Black, style = MaterialTheme.typography.labelMedium)
    }
}

/** Reference jaisa dark card wrapper. */
@Composable
fun VerifyCard(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
            .background(Brush.verticalGradient(listOf(Tk.Surface2, Tk.Surface)))
            .border(1.dp, Tk.Line, RoundedCornerShape(28.dp)).padding(horizontal = 20.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, color = Tk.White, fontFamily = Display, fontWeight = FontWeight.Black, fontSize = 22.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, color = Tk.Muted, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        content()
    }
}
