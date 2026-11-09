@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.techkadak.app.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.techkadak.app.Config
import com.techkadak.app.data.AppItem
import com.techkadak.app.data.Dates
import com.techkadak.app.data.IconLoader
import com.techkadak.app.data.Kit
import com.techkadak.app.data.TkVideo
import com.techkadak.app.data.Videos
import com.techkadak.app.install.ApkSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ───────────────────────── KITS ─────────────────────────

@Composable
fun KitsRow(kits: List<Kit>, all: List<AppItem>, onKit: (Kit) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 18.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(kits, key = { it.id }) { k -> KitCard(k, k.apps(all)) { onKit(k) } }
    }
}

@Composable
fun KitCard(kit: Kit, apps: List<AppItem>, modifier: Modifier = Modifier.width(250.dp), onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier.height(170.dp).clip(shape)
            .background(Brush.linearGradient(listOf(Tk.HeroStart, Tk.Surface)))
            .border(1.dp, Tk.LimeBorder, shape)
            .pressable(onClick)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(kit.emoji, fontSize = 28.sp)
            Spacer(Modifier.weight(1f))
            Text(
                tr("${apps.size} APPS", "${apps.size} APPS"),
                Modifier.clip(RoundedCornerShape(50)).background(Tk.Lime).padding(horizontal = 9.dp, vertical = 3.dp),
                color = Tk.OnLime, fontFamily = Display, fontWeight = FontWeight.Black, fontSize = 9.sp, letterSpacing = 1.sp,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(kit.name, color = Tk.White, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(kit.tagline, color = Tk.Muted, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
            apps.take(6).forEach { a -> Box(Modifier.clip(RoundedCornerShape(10.dp)).border(2.dp, Tk.Surface, RoundedCornerShape(10.dp))) { AppIcon(a, 34.dp, 10.dp) } }
        }
    }
}

@Composable
fun KitsListScreen(kits: List<Kit>, all: List<AppItem>, onKit: (Kit) -> Unit, onBack: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { ScreenTitle(tr("Kits 🎒", "Kits 🎒"), tr("Ek tap me kaam ke apps ka set", "A set of useful apps in one tap"), onBack) }
        items(kits, key = { it.id }) { k ->
            KitCard(k, k.apps(all), Modifier.padding(horizontal = 18.dp, vertical = 6.dp).fillMaxWidth()) { onKit(k) }
        }
    }
}

/** Kit page: "Sab install karo" sirf APK wale apps queue karta hai; Play / Web wale apne button ke saath. */
@Composable
fun KitScreen(
    kit: Kit,
    all: List<AppItem>,
    sourceFor: (AppItem) -> ApkSource,
    isInstalled: (AppItem) -> Boolean,
    isNew: (AppItem) -> Boolean,
    onInstallAll: () -> Int,
    onOpen: (AppItem) -> Unit,
    onPlay: (AppItem) -> Unit,
    onLink: (String) -> Unit,
    onDownloads: () -> Unit,
    onBack: () -> Unit,
) {
    val apps = remember(kit, all) { kit.apps(all) }
    val installable = apps.filter { sourceFor(it).kind == ApkSource.Kind.APK && !isInstalled(it) && sourceFor(it).minSdk <= android.os.Build.VERSION.SDK_INT }
    var queued by remember(kit.id) { mutableStateOf<Int?>(null) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 28.dp)) {
        item { ScreenTitle(kit.name, kit.tagline, onBack) }
        item {
            Column(
                Modifier.padding(horizontal = 18.dp, vertical = 6.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(Tk.HeroStart, Tk.Surface))).border(1.dp, Tk.LimeBorder, RoundedCornerShape(24.dp)).padding(18.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(kit.emoji, fontSize = 40.sp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(tr("${apps.size} apps • ${installable.size} yahin se install", "${apps.size} apps • ${installable.size} install here"), color = Tk.White, style = MaterialTheme.typography.titleSmall)
                        Text(tr("Sab official source se, free", "All from official sources, free"), color = Tk.Muted, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(Modifier.height(14.dp))
                val q = queued
                if (q != null) {
                    LimeButton(tr("$q apps download ho rahe — dekho", "$q apps downloading — view"), Modifier.fillMaxWidth(), Icons.Rounded.Download) { onDownloads() }
                } else if (installable.isNotEmpty()) {
                    LimeButton(tr("Sab install karo (${installable.size})", "Install all (${installable.size})"), Modifier.fillMaxWidth(), Icons.Rounded.DownloadForOffline) { queued = onInstallAll() }
                } else {
                    Text(tr("Is kit ke saare installable apps phone me hain ✅", "All installable apps from this kit are on your phone ✅"), color = Tk.Lime, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        item { SectionHeader(tr("Is kit me", "In this kit")) }
        items(apps, key = { it.slug }) { a ->
            val src = sourceFor(a)
            Row(Modifier.fillMaxWidth().pressable { onOpen(a) }.padding(horizontal = 18.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                AppIcon(a, 56.dp, 16.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(a.name, color = Tk.White, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, false))
                        if (isNew(a)) { Spacer(Modifier.width(6.dp)); NewBadge() }
                    }
                    Text(a.what, color = Tk.Muted, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.width(10.dp))
                when {
                    src.kind == ApkSource.Kind.APK && isInstalled(a) -> KindPill(tr("Installed", "Installed"), true)
                    src.kind == ApkSource.Kind.APK -> KindPill(src.sourceLabel, false)
                    src.kind == ApkSource.Kind.PLAY -> SmallAction(tr("Play Store", "Play Store")) { onPlay(a) }
                    else -> SmallAction(tr("PC / Web", "PC / Web")) { (src.link ?: a.downloadLinks.firstOrNull()?.url)?.let(onLink) }
                }
            }
        }
        item {
            Text(
                tr("\"Sab install karo\" sirf un apps ko download karta hai jo yahin se install hote hain (GitHub / F-Droid / developer site). Har app ke liye Android install poochega.",
                    "\"Install all\" only downloads apps that install from here (GitHub / F-Droid / developer site). Android asks before each install."),
                Modifier.padding(horizontal = 22.dp, vertical = 16.dp), color = Tk.Dim, style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun KindPill(text: String, on: Boolean) {
    Text(
        text, Modifier.clip(RoundedCornerShape(50)).background(if (on) Tk.LimeSoft else Tk.Surface2).border(1.dp, if (on) Tk.Lime else Tk.Line, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 5.dp),
        color = if (on) Tk.Lime else Tk.Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1,
    )
}

@Composable
private fun SmallAction(text: String, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(50)).border(1.dp, Tk.Lime, RoundedCornerShape(50)).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, color = Tk.Lime, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        Spacer(Modifier.width(3.dp))
        Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, tint = Tk.Lime, modifier = Modifier.size(12.dp))
    }
}

// ───────────────────────── VIDEOS ─────────────────────────

@Composable
fun NetImage(url: String, modifier: Modifier, target: Int = 480, contentScale: ContentScale = ContentScale.Crop) {
    val ctx = LocalContext.current
    val key = if (target == 192) url else "$url#$target"
    var bmp by remember(url) { mutableStateOf(IconLoader.peek(key)) }
    LaunchedEffect(url) { if (bmp == null) bmp = IconLoader.load(ctx, url, target) }
    Box(modifier.background(Tk.Surface2), contentAlignment = Alignment.Center) {
        val b = bmp
        if (b != null) Image(b.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = contentScale)
        else Icon(Icons.Rounded.PlayCircle, null, tint = Tk.Dim, modifier = Modifier.size(36.dp))
    }
}

@Composable
fun VideoCard(v: TkVideo, modifier: Modifier = Modifier.width(260.dp), onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(modifier.clip(shape).background(Tk.Surface).border(1.dp, Tk.Line, shape).pressable(onClick)) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
            NetImage(v.thumb, Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)))))
            Box(Modifier.align(Alignment.Center).size(50.dp).clip(CircleShape).background(Tk.Lime), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.PlayArrow, tr("Video dekho", "Watch video"), tint = Tk.OnLime, modifier = Modifier.size(30.dp))
            }
            Text(
                "YOUTUBE", Modifier.align(Alignment.TopStart).padding(8.dp).clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.6f)).padding(horizontal = 8.dp, vertical = 3.dp),
                color = Color.White, fontFamily = Display, fontWeight = FontWeight.Black, fontSize = 8.sp, letterSpacing = 1.sp,
            )
        }
        Column(Modifier.padding(12.dp)) {
            Text(v.title, color = Tk.White, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text("Tech Kadak • " + Dates.pretty(v.published.take(10)), color = Tk.Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun VideosRow(videos: List<TkVideo>, onVideo: (TkVideo) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 18.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(videos.take(12), key = { it.id }) { v -> VideoCard(v) { onVideo(v) } }
    }
}

@Composable
fun VideosScreen(videos: List<TkVideo>, onVideo: (TkVideo) -> Unit, onChannel: () -> Unit, onBack: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { ScreenTitle(tr("Tech Kadak Videos ▶️", "Tech Kadak Videos ▶️"), tr("Rohit ke tutorials — apps kaise install aur use kare", "Rohit's tutorials — how to install and use apps"), onBack) }
        if (videos.isEmpty()) item { Empty(tr("Abhi videos load nahi hue — net check karo", "Videos didn't load — check your internet")) }
        items(videos, key = { it.id }) { v -> VideoCard(v, Modifier.padding(horizontal = 18.dp, vertical = 7.dp).fillMaxWidth()) { onVideo(v) } }
        item {
            Box(Modifier.padding(18.dp)) { OutlineButton(tr("YouTube channel kholo", "Open the YouTube channel"), Modifier.fillMaxWidth(), Icons.Rounded.Subscriptions) { onChannel() } }
        }
    }
}

/** Detail page: is app ka Tech Kadak video. */
@Composable
fun DetailVideos(app: AppItem, videos: List<TkVideo>, onVideo: (TkVideo) -> Unit) {
    val mine = remember(app.slug, videos) { Videos.forApp(app, videos) }
    if (mine.isEmpty()) return
    Column(Modifier.padding(vertical = 8.dp)) {
        Text(tr("Is app ka Tech Kadak video ▶️", "Tech Kadak video on this app ▶️"), Modifier.padding(horizontal = 20.dp), color = Tk.White, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(10.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(mine, key = { it.id }) { v -> VideoCard(v, Modifier.width(280.dp)) { onVideo(v) } }
        }
    }
}

// ───────────────────────── POSTER ─────────────────────────

@Composable
fun PosterDialog(app: AppItem, onShare: (PosterFormat, Bitmap) -> Unit, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    var format by remember { mutableStateOf(PosterFormat.STORY) }
    var bmp by remember(app.slug, format) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(app.slug, format) {
        val icon = app.iconUrl(Config.SITE_BASE)?.let { IconLoader.load(ctx, it, 512) }
        bmp = withContext(Dispatchers.Default) { runCatching { Poster.render(ctx, app, icon, format) }.getOrNull() }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.padding(16.dp).fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Tk.Surface).border(1.dp, Tk.LimeBorder, RoundedCornerShape(28.dp)).padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(tr("Poster share karo", "Share a poster"), color = Tk.White, style = MaterialTheme.typography.headlineSmall)
            Text(tr("WhatsApp / Instagram status ke liye ready", "Ready for WhatsApp / Instagram status"), color = Tk.Muted, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PosterFormat.entries.forEach { f -> Chip(tr(f.hi, f.en), f == format) { format = f } }
            }
            Spacer(Modifier.height(12.dp))
            val ratio = format.w.toFloat() / format.h
            Box(Modifier.fillMaxWidth(if (format == PosterFormat.STORY) 0.56f else 0.86f).aspectRatio(ratio).clip(RoundedCornerShape(16.dp)).background(Tk.Black).border(1.dp, Tk.Line, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                val b = bmp
                if (b != null) Image(b.asImageBitmap(), tr("Poster", "Poster"), Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                else CircularProgressIndicator(color = Tk.Lime, strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(14.dp))
            LimeButton(tr("Share karo", "Share"), Modifier.fillMaxWidth(), Icons.Rounded.Share) { bmp?.let { onShare(format, it) } }
            TextButton(onClick = onDismiss) { Text(tr("Band karo", "Close"), color = Tk.Muted) }
        }
    }
}
