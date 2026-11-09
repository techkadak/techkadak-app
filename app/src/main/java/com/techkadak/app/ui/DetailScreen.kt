@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.techkadak.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.techkadak.app.Config
import com.techkadak.app.data.AppItem
import com.techkadak.app.data.Catalog
import com.techkadak.app.data.Dates

@Composable
fun DetailScreen(
    app: AppItem,
    all: List<AppItem>,
    isNew: (AppItem) -> Boolean,
    onBack: () -> Unit,
    onLink: (String) -> Unit,
    onShare: (AppItem) -> Unit,
    onOpen: (AppItem) -> Unit,
    isFav: Boolean = false,
    onFav: () -> Unit = {},
    onWeb: (String) -> Unit = onLink,
    install: InstallUiState? = null,
    installActions: InstallActions = InstallActions(),
    botClock: Float? = null,
    videos: List<com.techkadak.app.data.TkVideo> = emptyList(),
    onVideo: (com.techkadak.app.data.TkVideo) -> Unit = {},
    onPoster: (() -> Unit)? = null,
    user: com.techkadak.app.auth.TkUser? = null,
    onLogin: () -> Unit = {},
) {
    var shown by remember(app.slug) { mutableStateOf(false) }
    LaunchedEffect(app.slug) { shown = true }
    val links = app.downloadLinks
    val primary = links.firstOrNull()
    val similar = remember(app.slug, all) {
        all.filter { it.slug != app.slug && it.categories.any { c -> c in app.categories } }.take(12)
    }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, tr("Wapas", "Back"), tint = Tk.White) }
            Spacer(Modifier.weight(1f))
            FavButton(isFav, onToggle = onFav)
            IconButton(onClick = { onShare(app) }) { Icon(Icons.Rounded.Share, "Share", tint = Tk.White) }
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            AnimatedVisibility(shown, enter = fadeIn() + slideInVertically { it / 8 }) {
                Column {
                    // Header
                    Row(Modifier.padding(horizontal = 20.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        AppIcon(app, 92.dp, 22.dp)
                        Spacer(Modifier.width(18.dp))
                        Column(Modifier.weight(1f)) {
                            Text(app.name, style = MaterialTheme.typography.headlineMedium, color = Tk.White)
                            Spacer(Modifier.height(4.dp))
                            Text(Catalog.developer(app), color = Tk.Lime, style = MaterialTheme.typography.titleSmall)
                            Text(app.categories.joinToString(" • ") { Catalog.shortName(it) }, color = Tk.Muted, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    // Stats row (Play jaisa)
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Stat(app.pricing.uppercase(), tr("Price", "Price"), Modifier.weight(1f))
                        StatDivider()
                        Stat(if (Catalog.isOpenSource(app)) "OPEN" else "—", tr("Source code", "Source code"), Modifier.weight(1f))
                        StatDivider()
                        Stat(shortPlatform(app.platforms), "Platform", Modifier.weight(1f))
                        StatDivider()
                        Stat(Dates.pretty(app.added).substringBeforeLast(' ').ifEmpty { "—" }, tr("Add hua", "Added"), Modifier.weight(1f))
                    }
                    if (install != null) {
                        InstallCard(install, installActions, Modifier.padding(horizontal = 20.dp), botClock)
                    } else if (primary != null) {
                        LimeButton("DOWNLOAD", Modifier.padding(horizontal = 20.dp).fillMaxWidth(), Icons.Rounded.Download) { onLink(primary.url) }
                        Text(
                            "${Catalog.downloadLabel(primary)} • " + tr("official link", "official link"),
                            Modifier.fillMaxWidth().padding(top = 6.dp), textAlign = TextAlign.Center, color = Tk.Muted, style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    // chips
                    FlowRow(Modifier.padding(horizontal = 20.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        InfoChip(Icons.Rounded.Sell, app.pricing)
                        if (Catalog.isOpenSource(app)) InfoChip(Icons.Rounded.Code, "Open-source")
                        if (app.links.any { it.label.equals("F-Droid", true) }) InfoChip(Icons.Rounded.Shield, tr("F-Droid pe", "On F-Droid"))
                        if (app.verified != null) InfoChip(Icons.Rounded.Verified, tr("Check kiya ", "Checked ") + Dates.pretty(app.verified))
                        if (isNew(app)) InfoChip(Icons.Rounded.NewReleases, tr("Naya", "New"))
                    }
                    if (install != null && links.isNotEmpty()) {
                        Text(tr("Official links", "Official links"), Modifier.padding(start = 20.dp, top = 4.dp, bottom = 8.dp), color = Tk.Muted, style = MaterialTheme.typography.labelMedium)
                        FlowRow(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            links.forEach { l -> OutlineButton(l.label, icon = Icons.AutoMirrored.Rounded.OpenInNew) { onLink(l.url) } }
                        }
                    } else if (links.size > 1) {
                        Text(tr("Aur jagah se lo", "Other official sources"), Modifier.padding(start = 20.dp, top = 4.dp, bottom = 8.dp), color = Tk.Muted, style = MaterialTheme.typography.labelMedium)
                        FlowRow(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            links.drop(1).forEach { l -> OutlineButton(l.label, icon = Icons.AutoMirrored.Rounded.OpenInNew) { onLink(l.url) } }
                        }
                    }
                }
            }
            ScreenshotCarousel(app.screenshots, Modifier.padding(top = 12.dp))
            HorizontalDivider(Modifier.padding(top = 20.dp), color = Tk.Line)
            ReviewsSection(app.slug, app.name, user, onLogin)
            HorizontalDivider(color = Tk.Line)
            Section(tr("Is app ke baare me", "About this app")) {
                Text(app.what, color = Tk.White, style = MaterialTheme.typography.bodyLarge)
            }
            if (app.replaces != null) {
                Section("Paid ka FREE jawab") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("PAID ❌", color = Tk.Muted, style = MaterialTheme.typography.labelLarge)
                        Text("  ${app.replaces}  →  ", color = Tk.White, style = MaterialTheme.typography.bodyMedium)
                        Text("FREE ✅", color = Tk.Lime, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            app.priceNote?.let { Section(tr("Paisa", "Pricing")) { Text(it, color = Tk.White, style = MaterialTheme.typography.bodyMedium) } }
            app.limit?.let { lim ->
                Row(
                    Modifier.padding(horizontal = 20.dp, vertical = 8.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(Tk.Surface2).border(1.dp, Tk.Line, RoundedCornerShape(16.dp)).padding(14.dp),
                ) {
                    Text("⚠️", fontSize = 16.sp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Limit", color = Tk.Lime, style = MaterialTheme.typography.labelLarge)
                        Text(lim, color = Tk.White, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            app.platforms?.let { Section(tr("Kahan chalega", "Platforms")) { Text(it, color = Tk.White, style = MaterialTheme.typography.bodyMedium) } }
            app.instagramPost?.let { p ->
                Box(Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                    OutlineButton(tr("Instagram post dekho", "See the Instagram post"), Modifier.fillMaxWidth(), Icons.Rounded.PlayCircle) { onLink(p.url) }
                }
            }
            DetailVideos(app, videos, onVideo)
            if (onPoster != null) {
                Box(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                    LimeButton(tr("Poster share karo", "Share a poster"), Modifier.fillMaxWidth(), Icons.Rounded.AutoAwesome) { onPoster() }
                }
            }
            Box(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                OutlineButton(tr("Website pe poori detail", "Full details on website"), Modifier.fillMaxWidth(), Icons.Rounded.Language) { onWeb(app.pageUrl(Config.SITE_BASE)) }
            }
            if (similar.isNotEmpty()) {
                SectionHeader(tr("Isi tarah ke apps", "Similar apps"))
                AppRow(similar, isNew, onOpen)
            }
            Text(
                tr("Hum APK khud host nahi karte — DOWNLOAD hamesha app ke official page pe le jaata hai. Install se pehle permissions dekh lena.", "We never host APKs — DOWNLOAD always opens the app's official page. Check permissions before installing."),
                Modifier.padding(horizontal = 24.dp, vertical = 24.dp), color = Tk.Dim, style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun shortPlatform(p: String?): String {
    if (p == null) return "—"
    val l = p.lowercase()
    val n = listOf("android", "ios", "windows", "mac", "linux", "web").count { l.contains(it) }
    return when {
        n >= 3 -> "SAB PE"
        l.contains("android") -> "ANDROID"
        l.contains("web") -> "WEB"
        else -> p.substringBefore(',').substringBefore('/').trim().uppercase().take(8)
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Tk.White, fontFamily = Display, fontWeight = FontWeight.Black, fontSize = 13.sp, maxLines = 1)
        Spacer(Modifier.height(3.dp))
        Text(label, color = Tk.Muted, style = MaterialTheme.typography.bodySmall, maxLines = 1)
    }
}

@Composable
private fun StatDivider() = Box(Modifier.width(1.dp).height(28.dp).background(Tk.Line))

@Composable
private fun InfoChip(icon: ImageVector, text: String) {
    Row(
        Modifier.clip(RoundedCornerShape(10.dp)).background(Tk.Surface2).border(1.dp, Tk.Line, RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = Tk.Lime, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, color = Tk.White, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
        Text(title, color = Tk.White, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        content()
    }
}
