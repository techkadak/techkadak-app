package com.techkadak.app.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

/**
 * v1.5: Play-Store jaisi screenshot carousel. Sirf official source ke URLs (apps.json "screenshots").
 * Khali list = kuch nahi dikhta. Ek ek screenshot swipe hota hai, lazy load + cache (IconLoader).
 */
@Composable
fun ScreenshotCarousel(urls: List<String>, modifier: Modifier = Modifier) {
    if (urls.isEmpty()) return
    Column(modifier) {
        SectionHeader(tr("Screenshots", "Screenshots"), subtitle = tr("App ke official screenshots", "Official screenshots of the app"))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items(urls, key = { it }) { url ->
                val shape = RoundedCornerShape(18.dp)
                NetImage(
                    url,
                    Modifier.height(340.dp).width(170.dp).clip(shape).border(1.dp, Tk.Line, shape),
                    target = 720,
                    contentScale = ContentScale.Fit,
                )
            }
        }
    }
}
