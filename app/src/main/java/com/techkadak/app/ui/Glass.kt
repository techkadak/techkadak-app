package com.techkadak.app.ui

import android.provider.Settings
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/** System "Remove animations" / animation scale 0 = reduce motion: wallpaper ruk jaata hai. */
@Composable
fun reduceMotion(): Boolean {
    val ctx = LocalContext.current
    return remember { runCatching { Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }.getOrDefault(false) }
}

/**
 * Liquid Glass theme ka wallpaper: gehra hara-kaala base + dheere ghoomte soft lime / mint / amber blobs.
 * Iske upar translucent panes "frosted glass" jaise dikhte hain. Original design (kisi OS ka asset nahi).
 */
@Composable
fun GlassBackdrop(modifier: Modifier = Modifier, animate: Boolean = true, content: @Composable BoxScope.() -> Unit) {
    val still = !animate || reduceMotion()
    val t = if (still) 0.15f else {
        val inf = rememberInfiniteTransition(label = "glass")
        val v by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(26000, easing = LinearEasing), RepeatMode.Restart), label = "t")
        v
    }
    val accent = Tk.palette.accent
    Box(modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF07110B), Color(0xFF040705), Color(0xFF0A0F07))))) {
        Canvas(Modifier.fillMaxSize().blur(48.dp)) {
            val w = size.width; val h = size.height
            val a = (t * 2 * Math.PI).toFloat()
            fun blob(cx: Float, cy: Float, r: Float, c: Color) =
                drawCircle(Brush.radialGradient(listOf(c, c.copy(alpha = 0f)), Offset(cx, cy), r), r, Offset(cx, cy))
            blob(w * (0.80f + 0.10f * cos(a)), h * (0.12f + 0.06f * sin(a)), w * 0.75f, accent.copy(alpha = 0.55f))
            blob(w * (0.10f + 0.12f * sin(a * 1.3f)), h * (0.42f + 0.08f * cos(a)), w * 0.70f, Color(0xFF2EFFB0).copy(alpha = 0.30f))
            blob(w * (0.75f + 0.08f * sin(a * 0.7f)), h * (0.78f + 0.05f * cos(a * 1.1f)), w * 0.65f, Color(0xFFFFD60A).copy(alpha = 0.20f))
            blob(w * (0.30f + 0.10f * cos(a * 0.9f)), h * (0.95f + 0.03f * sin(a)), w * 0.55f, accent.copy(alpha = 0.30f))
        }
        // halki scrim: text hamesha padhne layak (contrast)
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.28f)))
        content()
    }
}
