package com.techkadak.app.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.hypot

/*
 * v1.5.1 opening animation (5 s). Flow borrowed from a logo-reveal style; artwork, words and colours are Tech Kadak's own
 * (lime / black / white). Timeline t = 0..1 of SPLASH_MS:
 *  0.00-0.14 lime dot sweeps in from the left and grows into a pill
 *  0.14-0.26 black wipe left to right
 *  0.27-0.44 tagline types in (lime on black), then slides away
 *  0.47-0.60 lime circle expands over black until the screen is lime again
 *  0.60-0.74 TECH KADAK letters build one by one, TK badge forms; white swoosh draws under the word
 *  0.68-0.76 lock-up settles up (badge + wordmark); 0.72-0.80 credit "Created by Rohit Saini" fades in under it
 *  0.94-1.00 the app is revealed from the centre (SplashHost)
 */

internal const val OPENING_TAGLINE = "OFFICIAL LINKS. ZERO JUGAAD."
private const val OPENING_WORD = "TECH KADAK"

private fun oSeg(t: Float, a: Float, b: Float) = ((t - a) / (b - a)).coerceIn(0f, 1f)
private val OutEase = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
private val BackEase = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)

@Composable
fun OpeningLayer(t: Float, onSkip: () -> Unit) {
    val lime = Palette.LIME
    val ink = Color(0xFF0A0A0A)
    val interaction = remember { MutableInteractionSource() }
    Box(Modifier.fillMaxSize().background(lime).clickable(interaction, null, onClick = onSkip)) {
        // Backdrop: dot/pill, black wipe, lime circle
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f
            val dot = 18.dp.toPx()
            val pillW = 120.dp.toPx()
            if (t < 0.15f) {
                val slide = OutEase.transform(oSeg(t, 0f, 0.12f))
                val x = (-w * 0.35f) + (cx - (-w * 0.35f)) * slide
                val morph = OutEase.transform(oSeg(t, 0.06f, 0.14f))
                val pw = dot + (pillW - dot) * morph
                drawRoundRect(ink, topLeft = Offset(x - pw / 2f, cy - dot / 2f), size = Size(pw, dot), cornerRadius = CornerRadius(dot / 2f))
            }
            if (t in 0.14f..0.60f) {
                val wipe = OutEase.transform(oSeg(t, 0.14f, 0.26f))
                if (wipe > 0f) drawRect(ink, size = Size(w * wipe, h))
            }
            if (t in 0.47f..0.60f) {
                drawRect(ink)
                val r = hypot(w / 2f, h / 2f) * OutEase.transform(oSeg(t, 0.47f, 0.60f))
                drawCircle(lime, radius = r, center = Offset(cx, cy))
            }
        }
        // Tagline: typed in, then slides up and away
        if (t in 0.26f..0.50f) {
            val n = (oSeg(t, 0.27f, 0.40f) * OPENING_TAGLINE.length).toInt().coerceIn(0, OPENING_TAGLINE.length)
            val out = oSeg(t, 0.44f, 0.50f)
            Box(
                Modifier.fillMaxSize().graphicsLayer { translationY = -48.dp.toPx() * out; alpha = 1f - out },
                contentAlignment = Alignment.Center,
            ) {
                Text(OPENING_TAGLINE.take(n), color = lime, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 1.5.sp, maxLines = 1, softWrap = false)
            }
        }
        // Lock-up: badge + wordmark + swoosh + credit
        if (t >= 0.58f) LockUp(t, ink, lime)
    }
}

@Composable
private fun LockUp(t: Float, ink: Color, lime: Color) {
    val badgeS = BackEase.transform(oSeg(t, 0.60f, 0.70f))
    val settle = OutEase.transform(oSeg(t, 0.68f, 0.76f))
    val swoosh = OutEase.transform(oSeg(t, 0.66f, 0.78f))
    val credit = oSeg(t, 0.72f, 0.80f)
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            Modifier.graphicsLayer { translationY = -22.dp.toPx() * settle },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(40.dp).graphicsLayer { scaleX = badgeS; scaleY = badgeS; alpha = oSeg(t, 0.60f, 0.63f) }
                        .clip(RoundedCornerShape(12.dp)).background(ink),
                    contentAlignment = Alignment.Center,
                ) { Text("TK", color = lime, fontFamily = Display, fontWeight = FontWeight.Black, fontSize = 15.sp) }
                Spacer(Modifier.width(10.dp))
                OPENING_WORD.forEachIndexed { i, ch ->
                    if (ch == ' ') { Spacer(Modifier.width(7.dp)); return@forEachIndexed }
                    val a = 0.60f + i * 0.012f
                    val p = BackEase.transform(oSeg(t, a, a + 0.05f))
                    Text(
                        ch.toString(), color = ink, fontFamily = Display, fontWeight = FontWeight.Black, fontSize = 26.sp, maxLines = 1, softWrap = false,
                        modifier = Modifier.graphicsLayer { alpha = oSeg(t, a, a + 0.03f); translationY = (1f - p) * 12.dp.toPx() },
                    )
                }
            }
            Canvas(Modifier.width(206.dp).height(12.dp)) {
                val w = size.width
                val path = Path().apply {
                    moveTo(0f, size.height * 0.45f)
                    quadraticBezierTo(w * 0.5f, size.height * 1.25f, w, size.height * 0.1f)
                }
                clipRect(0f, 0f, w * swoosh, size.height) {
                    drawPath(path, Color.White, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                }
            }
            Spacer(Modifier.height(26.dp))
            Box(Modifier.width(48.dp).height(2.dp).graphicsLayer { alpha = credit }.background(ink))
            Spacer(Modifier.height(12.dp))
            Text(
                "CREATED BY", color = ink.copy(alpha = 0.7f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 3.sp, maxLines = 1,
                modifier = Modifier.graphicsLayer { alpha = credit },
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Rohit Saini", color = ink, fontFamily = Display, fontSize = 24.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp, maxLines = 1,
                modifier = Modifier.graphicsLayer { alpha = credit; translationY = (1f - credit) * 8.dp.toPx() },
            )
        }
    }
}
