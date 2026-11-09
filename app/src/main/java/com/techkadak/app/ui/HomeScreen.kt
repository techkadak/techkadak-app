package com.techkadak.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.rounded.EmojiObjects
import androidx.compose.ui.platform.testTag
import com.techkadak.app.Config
import com.techkadak.app.R
import com.techkadak.app.data.AppItem
import com.techkadak.app.data.Catalog
import com.techkadak.app.data.HomeSection
import com.techkadak.app.data.RemoteUi
import com.techkadak.app.data.SectionPref
import kotlin.math.absoluteValue

@Composable
fun TopBar(onSearch: () -> Unit, onToggleTheme: (() -> Unit)? = null, onDownloads: (() -> Unit)? = null, activeDownloads: Int = 0, onFeedback: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.weight(1f).height(50.dp).clip(RoundedCornerShape(50)).background(Tk.Surface2).clickable(onClick = onSearch).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Search, null, tint = Tk.Muted)
            Spacer(Modifier.width(12.dp))
            Text(tr("Apps search karo…", "Search apps…"), color = Tk.Muted, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
        if (onDownloads != null) {
            Spacer(Modifier.width(6.dp))
            Box(Modifier.size(44.dp).clip(CircleShape).background(Tk.Surface2).clickable(onClick = onDownloads), contentAlignment = Alignment.Center) {
                Icon(androidx.compose.material.icons.Icons.Rounded.Download, tr("Downloads", "Downloads"), tint = if (activeDownloads > 0) Tk.Lime else Tk.White)
                if (activeDownloads > 0) Box(Modifier.align(Alignment.TopEnd).padding(6.dp).size(14.dp).clip(CircleShape).background(Tk.Lime), contentAlignment = Alignment.Center) {
                    Text("$activeDownloads", color = Tk.OnLime, fontSize = 9.sp, fontWeight = FontWeight.Black)
                }
            }
        }
        if (onFeedback != null) {
            Spacer(Modifier.width(6.dp))
            Box(Modifier.size(44.dp).clip(CircleShape).background(Tk.Surface2).clickable(onClick = onFeedback).testTag("topbar_feedback"), contentAlignment = Alignment.Center) {
                Icon(androidx.compose.material.icons.Icons.Rounded.EmojiObjects, tr("Suggestion", "Suggestion"), tint = Tk.Lime)
            }
        }
        if (onToggleTheme != null) {
            Spacer(Modifier.width(6.dp))
            Box(Modifier.size(44.dp).clip(CircleShape).background(Tk.Surface2).clickable(onClick = onToggleTheme), contentAlignment = Alignment.Center) {
                Icon(if (Tk.isLight) Icons.Rounded.DarkMode else Icons.Rounded.LightMode, tr("Light / Dark", "Light / Dark"), tint = Tk.White)
            }
        }
        Spacer(Modifier.width(6.dp))
        Image(painterResource(R.drawable.tk_logo), "Tech Kadak", Modifier.size(44.dp).clip(CircleShape).border(2.dp, Tk.Lime, CircleShape), contentScale = ContentScale.Crop)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    data: UiData,
    remote: RemoteUi,
    sections: List<SectionPref>,
    favs: List<AppItem>,
    recent: List<AppItem>,
    isNew: (AppItem) -> Boolean,
    onOpen: (AppItem) -> Unit,
    onCategory: (String) -> Unit,
    onSearch: () -> Unit,
    onRefresh: () -> Unit,
    onLink: (String) -> Unit,
    onAll: () -> Unit = {},
    onNewList: () -> Unit = {},
    onFavs: () -> Unit = {},
    onToggleTheme: (() -> Unit)? = null,
    onDownloads: (() -> Unit)? = null,
    onFeedback: (() -> Unit)? = null,
    activeDownloads: Int = 0,
    kits: List<com.techkadak.app.data.Kit> = emptyList(),
    videos: List<com.techkadak.app.data.TkVideo> = emptyList(),
    onKit: (com.techkadak.app.data.Kit) -> Unit = {},
    onKits: () -> Unit = {},
    onVideo: (com.techkadak.app.data.TkVideo) -> Unit = {},
    onVideos: () -> Unit = {},
) {
    val apps = data.apps
    Column(Modifier.fillMaxSize()) {
        TopBar(onSearch, onToggleTheme, onDownloads, activeDownloads, onFeedback)
        TkRefreshBox(refreshing = data.refreshing, onRefresh = onRefresh, modifier = Modifier.fillMaxSize()) {
            if (data.loading && apps.isEmpty()) {
                HomeSkeleton()
                return@TkRefreshBox
            }
            val cats = remember(apps) { Catalog.categories(apps, Config.CATEGORIES) }
            val featured = remember(apps, remote.featuredSlug) {
                val f = Catalog.featured(apps, remote.featuredSlug)
                listOfNotNull(f) + apps.filter { it != f }.take(4)
            }
            val top = remember(apps) { Catalog.topPicks(apps) }
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item { Chip(tr("Sab apps", "All apps"), true) { onAll() } }
                        items(cats, key = { it.first }) { (c, _) -> Chip(Catalog.shortName(c), false) { onCategory(c) } }
                    }
                }
                remote.maintenanceMessage?.let { m -> item { MaintenanceStrip(m) } }
                if (data.offlineNote) item { OfflineStrip() }
                if (remote.hasAnnouncement) item { AnnouncementStrip(remote.announcementText, remote.announcementUrl, onLink) }
                sections.filter { it.visible }.forEach { sp ->
                    when (sp.section) {
                        HomeSection.FEATURED -> item(key = "featured") { FeaturedPager(featured, onOpen) }
                        HomeSection.NEW -> {
                            item(key = "h_new") { SectionHeader(tr("Naye Apps 🔥", "New apps 🔥"), tr("Bot roz ek naya app daalta hai", "A new app every day")) { onNewList() } }
                            item(key = "r_new") { AppRow(apps.take(12), isNew, onOpen) }
                        }
                        HomeSection.KITS -> if (kits.isNotEmpty()) {
                            item(key = "h_kits") { SectionHeader(tr("Kits 🎒", "Kits 🎒"), tr("Ek tap me poora set install", "Install a whole set in one tap")) { onKits() } }
                            item(key = "r_kits") { KitsRow(kits, apps, onKit) }
                        }
                        HomeSection.VIDEOS -> if (videos.isNotEmpty()) {
                            item(key = "h_vid") { SectionHeader(tr("Tech Kadak Videos ▶️", "Tech Kadak Videos ▶️"), tr("Rohit ke naye tutorials", "Rohit's latest tutorials")) { onVideos() } }
                            item(key = "r_vid") { VideosRow(videos, onVideo) }
                        }
                        HomeSection.FAVOURITES -> if (favs.isNotEmpty()) {
                            item(key = "h_fav") { SectionHeader(tr("Meri Pasand ❤️", "My favourites ❤️"), tr("${favs.size} apps saved", "${favs.size} saved apps")) { onFavs() } }
                            item(key = "r_fav") { AppRow(favs.take(15), isNew, onOpen) }
                        }
                        HomeSection.RECENT -> if (recent.isNotEmpty()) {
                            item(key = "h_rec") { SectionHeader(tr("Haal me dekhe", "Recently viewed")) }
                            item(key = "r_rec") { AppRow(recent.take(15), isNew, onOpen) }
                        }
                        HomeSection.TOP -> {
                            item(key = "h_top") { SectionHeader(tr("Top picks", "Top picks"), tr("Tech Kadak ki pasand", "Tech Kadak's picks")) }
                            item(key = "r_top") { TopPicksGrid(top.take(6), isNew, onOpen) }
                        }
                        HomeSection.CATEGORIES -> cats.forEach { (c, n) ->
                            item(key = "h_$c") { SectionHeader(Catalog.shortName(c), "${Catalog.tagline(c)} • $n apps") { onCategory(c) } }
                            item(key = "r_$c") { AppRow(Catalog.inCategory(apps, c).take(15), isNew, onOpen) }
                        }
                        HomeSection.CHART -> {
                            item(key = "h_chart") { SectionHeader(tr("Sabse zyada kaam ke", "Most useful"), "Top chart") }
                            itemsIndexed(top.drop(6).take(8), key = { _, a -> "chart_" + a.slug }) { i, a ->
                                AppListItem(a, rank = i + 1, isNew = isNew(a)) { onOpen(a) }
                            }
                        }
                    }
                }
                item { FooterNote() }
            }
        }
    }
}

@Composable
private fun MaintenanceStrip(text: String) {
    Row(
        Modifier.padding(horizontal = 18.dp, vertical = 6.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Tk.Lime).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Build, null, tint = Tk.OnLime, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, color = Tk.OnLime, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun OfflineStrip() {
    Row(
        Modifier.padding(horizontal = 18.dp, vertical = 6.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Tk.Surface2).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.CloudOff, null, tint = Tk.Lime, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(tr("Net nahi hai — saved list dikha rahe hain. Neeche kheench ke refresh karo.", "Offline — showing the saved list. Pull down to refresh."), color = Tk.Muted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun AnnouncementStrip(text: String, url: String?, onLink: (String) -> Unit) {
    Row(
        Modifier.padding(horizontal = 18.dp, vertical = 6.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Tk.LimeSoft)
            .border(1.dp, Tk.Lime, RoundedCornerShape(14.dp)).clickable(enabled = url != null) { url?.let(onLink) }.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Campaign, null, tint = Tk.Lime, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, color = Tk.White, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun FeaturedPager(items: List<AppItem>, onOpen: (AppItem) -> Unit) {
    if (items.isEmpty()) return
    val state = rememberPagerState { items.size }
    Column {
        HorizontalPager(state, contentPadding = PaddingValues(horizontal = 18.dp), pageSpacing = 12.dp) { page ->
            val off = ((state.currentPage - page) + state.currentPageOffsetFraction).absoluteValue
            Box(Modifier.graphicsLayer { val s = 1f - 0.06f * off.coerceIn(0f, 1f); scaleY = s; alpha = 1f - 0.3f * off.coerceIn(0f, 1f) }) {
                FeaturedCard(items[page], if (page == 0) tr("AAJ KA APP", "APP OF THE DAY") else tr("NAYA APP", "NEW APP")) { onOpen(items[page]) }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.Center) {
            repeat(items.size) { i ->
                val sel = state.currentPage == i
                Box(Modifier.padding(3.dp).height(6.dp).width(if (sel) 20.dp else 6.dp).clip(CircleShape).background(if (sel) Tk.Lime else Tk.Line))
            }
        }
    }
}

@Composable
fun FeaturedCard(app: AppItem, tag: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(26.dp)
    Box(
        Modifier.fillMaxWidth().height(214.dp).clip(shape)
            .background(Brush.linearGradient(listOf(Tk.HeroStart, Tk.Surface), start = Offset(0f, 0f), end = Offset(900f, 700f)))
            .border(1.dp, Tk.LimeBorder, shape)
            .pressable(onClick),
    ) {
        // lime glow
        Box(Modifier.size(260.dp).align(Alignment.TopEnd).graphicsLayer { translationX = 120f; translationY = -120f }
            .background(Brush.radialGradient(listOf(Tk.Glow, Color.Transparent))))
        Text(
            "KADAK", Modifier.align(Alignment.TopEnd).padding(end = 12.dp, top = 6.dp).alpha(0.08f),
            color = Tk.Lime, fontFamily = Display, fontWeight = FontWeight.Black, fontSize = 40.sp,
        )
        Column(Modifier.fillMaxSize().padding(18.dp)) {
            Text(
                tag, Modifier.clip(RoundedCornerShape(50)).background(Tk.Lime).padding(horizontal = 10.dp, vertical = 4.dp),
                color = Tk.OnLime, fontFamily = Display, fontWeight = FontWeight.Black, fontSize = 10.sp, letterSpacing = 1.sp,
            )
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(app, 76.dp, 20.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(app.name, color = Tk.White, style = MaterialTheme.typography.headlineMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PriceBadge(app.pricing)
                        Spacer(Modifier.width(8.dp))
                        Text(Catalog.shortName(app.categories.firstOrNull() ?: ""), color = Tk.Muted, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(app.what, color = Tk.TextSoft, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun TopPicksGrid(apps: List<AppItem>, isNew: (AppItem) -> Boolean, onOpen: (AppItem) -> Unit) {
    // Play ke "suggested" jaisa: 3 ki 2 column list, side scroll
    val cols = apps.chunked(3)
    LazyRow(contentPadding = PaddingValues(horizontal = 0.dp)) {
        items(cols, key = { col -> col.first().slug }) { col ->
            Column(Modifier.width(320.dp)) { col.forEach { a -> AppListItem(a, isNew = isNew(a)) { onOpen(a) } } }
        }
    }
}

@Composable
private fun FooterNote() {
    Text(
        tr("Tech Kadak kisi app ki APK khud host nahi karta. Har DOWNLOAD button app ke official page (F-Droid / GitHub / Play Store / website) pe le jaata hai.", "Tech Kadak never hosts APKs. Every DOWNLOAD button opens the app's official page (F-Droid / GitHub / Play Store / website)."),
        Modifier.padding(horizontal = 24.dp, vertical = 26.dp), color = Tk.Dim, style = MaterialTheme.typography.bodySmall,
    )
}
