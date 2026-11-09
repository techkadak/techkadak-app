package com.techkadak.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.techkadak.app.Config
import com.techkadak.app.data.AppItem
import com.techkadak.app.data.CardStyle
import com.techkadak.app.data.IconLoader

/** Press karne pe halka sa dab jaane wala click (Play jaisa smooth feel). */
@Composable
fun Modifier.pressable(onClick: () -> Unit): Modifier {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) 0.96f else 1f, androidx.compose.animation.core.spring(dampingRatio = 0.55f, stiffness = 700f), label = "press")
    return this.scale(s).clickable(interactionSource = src, indication = null, onClick = onClick)
}

@Composable
fun shimmerBrush(): Brush {
    val t = rememberInfiniteTransition(label = "shimmer")
    val x by t.animateFloat(-400f, 1400f, infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart), label = "x")
    return Brush.linearGradient(
        listOf(Tk.Surface2, Tk.Shimmer, Tk.Surface2),
        start = Offset(x, 0f), end = Offset(x + 400f, 200f),
    )
}

@Composable
fun ShimmerBox(modifier: Modifier, radius: Dp = 12.dp) {
    Box(modifier.clip(RoundedCornerShape(radius)).background(shimmerBrush()))
}

@Composable
fun AppIcon(app: AppItem, size: Dp, radius: Dp = size * 0.24f) {
    val ctx = LocalContext.current
    val url = remember(app.slug) { app.iconUrl(Config.SITE_BASE) }
    var bmp by remember(url) { mutableStateOf(url?.let { IconLoader.peek(it) }) }
    LaunchedEffect(url) { if (bmp == null && url != null) bmp = IconLoader.load(ctx, url) }
    val shape = RoundedCornerShape(radius)
    Box(
        Modifier.size(size).clip(shape).background(if (bmp != null) Color.White else Tk.Surface2).border(1.dp, Tk.Line, shape),
        contentAlignment = Alignment.Center,
    ) {
        val b = bmp
        if (b != null) {
            Image(b.asImageBitmap(), app.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Text(app.initials, color = Tk.Lime, fontFamily = Display, fontWeight = FontWeight.Black, fontSize = (size.value * 0.34f).sp)
        }
    }
}

@Composable
fun PriceBadge(pricing: String, modifier: Modifier = Modifier) {
    val free = pricing.equals("Free", true)
    Text(
        pricing.uppercase(),
        modifier
            .clip(RoundedCornerShape(50))
            .background(if (free) Tk.Lime else Color.Transparent)
            .border(1.dp, Tk.Lime, RoundedCornerShape(50))
            .padding(horizontal = 7.dp, vertical = 2.dp),
        color = if (free) Tk.OnLime else Tk.Lime,
        style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, letterSpacing = 0.6.sp,
    )
}

@Composable
fun NewBadge(modifier: Modifier = Modifier) {
    Text(
        "NEW", modifier.clip(RoundedCornerShape(4.dp)).background(Tk.White).padding(horizontal = 5.dp, vertical = 1.dp),
        color = Tk.Black, fontFamily = Display, fontWeight = FontWeight.Black, fontSize = 8.sp,
    )
}

@Composable
fun SectionHeader(title: String, subtitle: String? = null, onMore: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().then(if (onMore != null) Modifier.clickable(onClick = onMore) else Modifier)
            .padding(start = 18.dp, end = 10.dp, top = 22.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = Tk.White)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Tk.Muted)
        }
        if (onMore != null) Icon(Icons.AutoMirrored.Rounded.ArrowForward, tr("Aur dekho", "See more"), tint = Tk.Lime, modifier = Modifier.padding(8.dp).size(22.dp))
    }
}

/** Play jaisa chhota card: bada icon, naam, category, badge. Horizontal rows me. */
@Composable
fun AppTile(app: AppItem, isNew: Boolean, size: Dp = 104.dp, onClick: () -> Unit) {
    val compact = size < 90.dp
    Column(Modifier.width(size).pressable(onClick)) {
        Box {
            AppIcon(app, size, size * 0.25f)
            if (isNew) NewBadge(Modifier.align(Alignment.TopEnd).padding(6.dp))
        }
        Spacer(Modifier.height(if (compact) 6.dp else 8.dp))
        Text(
            app.name, style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.titleSmall, color = Tk.White,
            maxLines = if (compact) 1 else 2, overflow = TextOverflow.Ellipsis, lineHeight = if (compact) 15.sp else 17.sp,
        )
        if (!compact) {
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) { PriceBadge(app.pricing) }
        }
    }
}

/** Home ki row — Settings ke "card style" ke hisaab se: Grid (bade tiles), Compact (chhote), List (line wise). */
@Composable
fun AppRow(apps: List<AppItem>, isNew: (AppItem) -> Boolean, onOpen: (AppItem) -> Unit, listLimit: Int = 5) {
    when (Tk.cardStyle) {
        CardStyle.LIST -> Column { apps.take(listLimit).forEach { a -> AppListItem(a, isNew = isNew(a)) { onOpen(a) } } }
        CardStyle.COMPACT -> LazyRow(contentPadding = PaddingValues(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(apps, key = { it.slug }) { AppTile(it, isNew(it), 72.dp, onClick = { onOpen(it) }) }
        }
        CardStyle.GRID -> LazyRow(contentPadding = PaddingValues(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(apps, key = { it.slug }) { AppTile(it, isNew(it), onClick = { onOpen(it) }) }
        }
    }
}

/** Vertical list ko style ke hisaab se: Grid = 3 column tiles, baaki = list. LazyColumn ke andar items ki tarah. */
fun androidx.compose.foundation.lazy.LazyListScope.styledAppList(
    apps: List<AppItem>, isNew: (AppItem) -> Boolean, keyPrefix: String, ranked: Boolean = false, onOpen: (AppItem) -> Unit,
) {
    if (Tk.cardStyle == CardStyle.GRID) {
        val rows = apps.chunked(3)
        items(rows.size, key = { keyPrefix + "g" + it }) { r ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                rows[r].forEach { a -> AppTile(a, isNew(a), 104.dp) { onOpen(a) } }
                repeat(3 - rows[r].size) { Spacer(Modifier.width(104.dp)) }
            }
        }
    } else {
        items(apps.size, key = { keyPrefix + apps[it].slug }) { i ->
            val a = apps[i]
            AppListItem(a, rank = if (ranked) i + 1 else null, isNew = isNew(a)) { onOpen(a) }
        }
    }
}

/** List wala card (search results, category page, top charts). Compact style me chhota. */
@Composable
fun AppListItem(app: AppItem, rank: Int? = null, isNew: Boolean = false, fav: Boolean = false, onClick: () -> Unit) {
    val compact = Tk.cardStyle == CardStyle.COMPACT
    Row(
        Modifier.fillMaxWidth().pressable(onClick).padding(horizontal = 18.dp, vertical = if (compact) 5.dp else 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (rank != null) {
            Text("$rank", Modifier.width(30.dp), color = Tk.Muted, fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        AppIcon(app, if (compact) 44.dp else 62.dp, if (compact) 12.dp else 16.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(app.name, style = MaterialTheme.typography.titleMedium, color = Tk.White, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (isNew) { Spacer(Modifier.width(6.dp)); NewBadge() }
                if (fav) { Spacer(Modifier.width(6.dp)); Icon(Icons.Rounded.Favorite, null, tint = Tk.Lime, modifier = Modifier.size(14.dp)) }
            }
            Text(
                app.categories.joinToString(" • ") { com.techkadak.app.data.Catalog.shortName(it) },
                style = MaterialTheme.typography.bodySmall, color = Tk.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            if (!compact) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PriceBadge(app.pricing)
                    app.platforms?.let {
                        Spacer(Modifier.width(8.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall, color = Tk.Dim, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        if (compact) PriceBadge(app.pricing)
    }
}

/** Dil wala button (favourite). Dabane pe halka sa "pop". */
@Composable
fun FavButton(on: Boolean, modifier: Modifier = Modifier, onToggle: () -> Unit) {
    val s by animateFloatAsState(if (on) 1.15f else 1f, androidx.compose.animation.core.spring(dampingRatio = 0.35f), label = "fav")
    androidx.compose.material3.IconButton(onClick = onToggle, modifier = modifier) {
        Icon(
            if (on) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            if (on) tr("Pasand se hatao", "Remove favourite") else tr("Pasand me daalo", "Add to favourites"),
            tint = if (on) Tk.Lime else Tk.White, modifier = Modifier.scale(s),
        )
    }
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Text(
        text,
        Modifier.clip(shape).background(if (selected) Tk.Lime else Tk.Surface2).border(1.dp, if (selected) Tk.Lime else Tk.Line, shape)
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp),
        color = if (selected) Tk.OnLime else Tk.White, style = MaterialTheme.typography.labelLarge,
    )
}

@Composable
fun LimeButton(text: String, modifier: Modifier = Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector? = null, onClick: () -> Unit) {
    Row(
        modifier.clip(RoundedCornerShape(50)).background(Brush.verticalGradient(listOf(Tk.LimeLight, Tk.Lime, Tk.LimeDeep)))
            .pressable(onClick).padding(horizontal = 22.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) { Icon(icon, null, tint = Tk.OnLime, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, color = Tk.OnLime, fontFamily = Display, fontWeight = FontWeight.Black, fontSize = 15.sp, letterSpacing = 0.5.sp)
    }
}

@Composable
fun OutlineButton(text: String, modifier: Modifier = Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector? = null, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier.clip(shape).border(1.5.dp, Tk.Lime, shape).pressable(onClick).padding(horizontal = 18.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) { Icon(icon, null, tint = Tk.Lime, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(7.dp)) }
        Text(text, color = Tk.Lime, style = MaterialTheme.typography.labelLarge)
    }
}

/** Loading ke time Home ka skeleton. */
@Composable
fun HomeSkeleton() {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        ShimmerBox(Modifier.padding(horizontal = 18.dp).fillMaxWidth().height(210.dp), 26.dp)
        repeat(2) {
            ShimmerBox(Modifier.padding(start = 18.dp, top = 26.dp, bottom = 12.dp).width(150.dp).height(18.dp), 6.dp)
            Row(Modifier.padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                repeat(4) {
                    Column {
                        ShimmerBox(Modifier.size(104.dp), 26.dp)
                        ShimmerBox(Modifier.padding(top = 8.dp).width(80.dp).height(12.dp), 4.dp)
                    }
                }
            }
        }
    }
}

@Composable
fun Dot() = Box(Modifier.padding(horizontal = 6.dp).size(3.dp).clip(CircleShape).background(Tk.Dim))
