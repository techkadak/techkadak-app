package com.techkadak.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.techkadak.app.Config
import com.techkadak.app.data.AppItem
import com.techkadak.app.data.Catalog
import com.techkadak.app.data.Dates
import com.techkadak.app.data.PriceFilter
import com.techkadak.app.data.SortMode
import androidx.compose.material.icons.automirrored.rounded.Sort

fun categoryIcon(c: String): ImageVector = when (c) {
    "Android Apps" -> Icons.Rounded.Android
    "AI Tools" -> Icons.Rounded.AutoAwesome
    "Open Source/GitHub" -> Icons.Rounded.Code
    "Paid ka FREE Jawab" -> Icons.Rounded.Savings
    "Student Tech" -> Icons.Rounded.School
    else -> Icons.Rounded.Apps
}

@Composable
fun ScreenTitle(title: String, subtitle: String? = null, onBack: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(start = if (onBack != null) 4.dp else 18.dp, end = 18.dp, top = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, tr("Wapas", "Back"), tint = Tk.White) }
        Column {
            Text(title, style = MaterialTheme.typography.headlineMedium, color = Tk.White)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Tk.Muted)
        }
    }
}

@Composable
fun CategoriesScreen(apps: List<AppItem>, onCategory: (String) -> Unit) {
    val cats = remember(apps) { Catalog.categories(apps, Config.CATEGORIES) }
    LazyVerticalGrid(GridCells.Fixed(2), Modifier.fillMaxSize(), contentPadding = PaddingValues(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item(span = { GridItemSpan(2) }) { ScreenTitle("Categories", tr("${apps.size} free apps, category wise", "${apps.size} free apps by category")) }
        items(cats, key = { it.first }) { (c, n) ->
            val sample = remember(apps, c) { Catalog.inCategory(apps, c).take(3) }
            val first = cats.firstOrNull()?.first == c
            val shape = RoundedCornerShape(22.dp)
            Column(
                Modifier.fillMaxWidth().height(170.dp).clip(shape)
                    .background(if (first) Brush.linearGradient(listOf(Tk.Lime, Tk.LimeDeep)) else Brush.linearGradient(listOf(Tk.Surface2, Tk.Surface)))
                    .border(1.dp, if (first) Tk.Lime else Tk.Line, shape)
                    .pressable { onCategory(c) }.padding(14.dp),
            ) {
                Icon(categoryIcon(c), null, tint = if (first) Tk.OnLime else Tk.Lime, modifier = Modifier.size(30.dp))
                Spacer(Modifier.weight(1f))
                Text(Catalog.shortName(c), color = if (first) Tk.OnLime else Tk.White, fontFamily = Display, fontWeight = FontWeight.Black, fontSize = 15.sp, lineHeight = 18.sp, maxLines = 2)
                Text("$n apps", color = if (first) Tk.OnLimeMuted else Tk.Muted, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) { sample.forEach { AppIcon(it, 28.dp, 8.dp) } }
            }
        }
        item(span = { GridItemSpan(2) }) {
            Text(tr("Naye category website pe aate hi yahan khud dikh jaate hain.", "New categories from the website show up here automatically."), Modifier.padding(10.dp), color = Tk.Dim, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** Sort (Naya / A-Z) + filter (Free / Freemium / Open-source) + optional category chips. */
@Composable
fun SortFilterBar(
    sort: SortMode, onSort: (SortMode) -> Unit,
    price: PriceFilter, onPrice: (PriceFilter) -> Unit,
    categories: List<String>? = null, category: String? = null, onCategory: (String?) -> Unit = {},
) {
    Column {
        LazyRow(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            item {
                Row(
                    Modifier.clip(RoundedCornerShape(50)).background(Tk.Surface2).border(1.dp, Tk.Line, RoundedCornerShape(50))
                        .clickable { onSort(if (sort == SortMode.NEW) SortMode.AZ else SortMode.NEW) }.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Sort, null, tint = Tk.Lime, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(tr(sort.hi, sort.en), color = Tk.White, style = MaterialTheme.typography.labelLarge)
                }
            }
            items(PriceFilter.entries, key = { it.name }) { f -> Chip(tr(f.hi, f.en), f == price) { onPrice(f) } }
        }
        if (categories != null) {
            LazyRow(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Chip(tr("Har category", "All categories"), category == null) { onCategory(null) } }
                items(categories) { c -> Chip(Catalog.shortName(c), c == category) { onCategory(c) } }
            }
        }
    }
}

@Composable
fun CategoryScreen(
    cat: String, apps: List<AppItem>, isNew: (AppItem) -> Boolean, onBack: () -> Unit, onOpen: (AppItem) -> Unit,
    sort: SortMode = SortMode.NEW, onSort: (SortMode) -> Unit = {},
) {
    val list = remember(apps, cat) { Catalog.inCategory(apps, cat) }
    var price by remember { mutableStateOf(PriceFilter.ALL) }
    val shown = remember(list, price, sort) { Catalog.sort(Catalog.filter(list, null, price), sort) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { ScreenTitle(Catalog.shortName(cat), "${Catalog.tagline(cat)} • ${list.size} apps", onBack) }
        item { SortFilterBar(sort, onSort, price, { price = it }) }
        if (price == PriceFilter.ALL && sort == SortMode.NEW && list.isNotEmpty()) {
            item { Box(Modifier.padding(horizontal = 18.dp, vertical = 10.dp)) { FeaturedCard(list.first(), tr("NAYA IS CATEGORY ME", "NEWEST HERE")) { onOpen(list.first()) } } }
        }
        styledAppList(shown, isNew, "c_", ranked = sort == SortMode.NEW, onOpen = onOpen)
        if (shown.isEmpty()) item { Empty(tr("Is filter me abhi koi app nahi", "No apps for this filter yet")) }
    }
}

/** "Sab apps": poori list — sort + category + price filter. */
@Composable
fun AllAppsScreen(
    apps: List<AppItem>, isNew: (AppItem) -> Boolean, onBack: () -> Unit, onOpen: (AppItem) -> Unit,
    sort: SortMode, onSort: (SortMode) -> Unit, initialCategory: String? = null,
) {
    var price by remember { mutableStateOf(PriceFilter.ALL) }
    var cat by remember { mutableStateOf(initialCategory) }
    val cats = remember(apps) { Catalog.categories(apps, Config.CATEGORIES).map { it.first } }
    val shown = remember(apps, price, sort, cat) { Catalog.sort(Catalog.filter(apps, cat, price), sort) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { ScreenTitle(tr("Sab apps", "All apps"), tr("${shown.size} of ${apps.size} apps", "${shown.size} of ${apps.size} apps"), onBack) }
        item { SortFilterBar(sort, onSort, price, { price = it }, cats, cat) { cat = it } }
        styledAppList(shown, isNew, "a_", onOpen = onOpen)
        if (shown.isEmpty()) item { Empty(tr("Is filter me abhi koi app nahi", "No apps for this filter yet")) }
    }
}

/** ❤️ tab: Favourites + Recently viewed. */
@Composable
fun FavouritesScreen(
    favs: List<AppItem>, recent: List<AppItem>, isNew: (AppItem) -> Boolean, onOpen: (AppItem) -> Unit,
    onClearRecent: () -> Unit, onExplore: () -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { ScreenTitle(tr("Meri Pasand", "Favourites"), tr("Dil dabao, app yahan save", "Tap the heart to save an app")) }
        if (favs.isEmpty()) {
            item {
                Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp).clip(RoundedCornerShape(22.dp)).background(Tk.Surface).border(1.dp, Tk.Line, RoundedCornerShape(22.dp)).padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.FavoriteBorder, null, tint = Tk.Lime, modifier = Modifier.size(44.dp))
                    Spacer(Modifier.height(10.dp))
                    Text(tr("Abhi koi favourite nahi", "No favourites yet"), color = Tk.White, style = MaterialTheme.typography.titleMedium)
                    Text(tr("Kisi bhi app ke page pe ❤️ dabao — wo yahan aa jayega.", "Tap ❤️ on any app page and it shows up here."), color = Tk.Muted, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(14.dp))
                    LimeButton(tr("APPS DEKHO", "EXPLORE APPS"), icon = Icons.Rounded.Explore, onClick = onExplore)
                }
            }
        } else {
            items(favs, key = { "f_" + it.slug }) { a -> AppListItem(a, isNew = isNew(a), fav = true) { onOpen(a) } }
        }
        if (recent.isNotEmpty()) {
            item {
                Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 8.dp, top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(tr("Haal me dekhe", "Recently viewed"), Modifier.weight(1f), color = Tk.White, style = MaterialTheme.typography.titleLarge)
                    Text(tr("Saaf karo", "Clear"), Modifier.clickable(onClick = onClearRecent).padding(10.dp), color = Tk.Lime, style = MaterialTheme.typography.labelLarge)
                }
            }
            item { Spacer(Modifier.height(6.dp)) }
            item { AppRow(recent, isNew, onOpen, listLimit = 10) }
        }
    }
}

@Composable
fun Empty(text: String) {
    Column(Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Rounded.SearchOff, null, tint = Tk.Lime, modifier = Modifier.size(42.dp))
        Spacer(Modifier.height(10.dp))
        Text(text, color = Tk.Muted, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun SearchScreen(
    apps: List<AppItem>,
    recent: List<String>,
    initial: String,
    autoFocus: Boolean,
    isNew: (AppItem) -> Boolean,
    onQueryDone: (String) -> Unit,
    onClearRecent: () -> Unit,
    onOpen: (AppItem) -> Unit,
    onCategory: (String) -> Unit,
    initialSubmitted: Boolean = initial.isNotBlank(),
    onBack: (() -> Unit)? = null,
    sort: SortMode = SortMode.NEW,
    onSort: (SortMode) -> Unit = {},
) {
    var price by remember { mutableStateOf(PriceFilter.ALL) }
    var q by rememberSaveable { mutableStateOf(initial) }
    var submitted by rememberSaveable { mutableStateOf(initialSubmitted) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(autoFocus) { if (autoFocus) runCatching { focus.requestFocus() } }
    val suggestions = remember(q, apps) { Catalog.suggestions(apps, q) }
    val results = remember(q, apps, price, sort) {
        if (q.isBlank()) emptyList() else Catalog.sort(Catalog.filter(apps.filter { it.matches(q) }, null, price), sort)
    }
    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) IconButton(onClick = onBack, Modifier.padding(start = 4.dp)) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, tr("Wapas", "Back"), tint = Tk.White) }
        Row(
            Modifier.padding(start = if (onBack != null) 0.dp else 14.dp, end = 14.dp, top = 10.dp, bottom = 10.dp).fillMaxWidth().height(52.dp).clip(RoundedCornerShape(50))
                .background(Tk.Surface2).border(1.5.dp, Tk.Lime, RoundedCornerShape(50)).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Search, null, tint = Tk.Lime)
            Spacer(Modifier.width(12.dp))
            Box(Modifier.weight(1f)) {
                if (q.isEmpty()) Text(tr("App ka naam, kaam ya paid app…", "App name, task or paid app…"), color = Tk.Muted, style = MaterialTheme.typography.bodyLarge)
                BasicTextField(
                    q, { q = it; submitted = false }, Modifier.fillMaxWidth().focusRequester(focus), singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = Tk.White), cursorBrush = SolidColor(Tk.Lime),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { submitted = true; onQueryDone(q) }),
                )
            }
            if (q.isNotEmpty()) IconButton(onClick = { q = ""; submitted = false }) { Icon(Icons.Rounded.Close, "Clear", tint = Tk.Muted) }
        }
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            if (q.isBlank()) {
                if (recent.isNotEmpty()) {
                    item {
                        Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 8.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Recent", Modifier.weight(1f), color = Tk.White, style = MaterialTheme.typography.titleMedium)
                            Text(tr("Clear", "Clear"), Modifier.clickable(onClick = onClearRecent).padding(10.dp), color = Tk.Lime, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    items(recent) { r -> SuggestionRow(Icons.Rounded.History, r) { q = r; submitted = true } }
                }
                item { Text(tr("Log ye dhundh rahe hain", "Popular searches"), Modifier.padding(start = 18.dp, top = 16.dp, bottom = 10.dp), color = Tk.White, style = MaterialTheme.typography.titleMedium) }
                item {
                    FlowChips(listOf("PDF", "Photo editor", "AI", "Notes", "Video", "Privacy", "Music", "Keyboard", "Free ka jawab")) { q = it; submitted = true; onQueryDone(it) }
                }
                item { Text(tr("Category se dhundho", "Browse by category"), Modifier.padding(start = 18.dp, top = 18.dp, bottom = 6.dp), color = Tk.White, style = MaterialTheme.typography.titleMedium) }
                items(Catalog.categories(apps, Config.CATEGORIES), key = { it.first }) { (c, n) ->
                    SuggestionRow(categoryIcon(c), "${Catalog.shortName(c)}  ·  $n") { onCategory(c) }
                }
            } else if (!submitted && suggestions.isNotEmpty()) {
                items(suggestions, key = { "s_" + it.slug }) { a ->
                    Row(Modifier.fillMaxWidth().clickable { onQueryDone(q); onOpen(a) }.padding(horizontal = 18.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        AppIcon(a, 36.dp, 10.dp)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(a.name, color = Tk.White, style = MaterialTheme.typography.titleSmall)
                            Text(Catalog.shortName(a.categories.firstOrNull() ?: ""), color = Tk.Muted, style = MaterialTheme.typography.bodySmall)
                        }
                        Icon(Icons.Rounded.NorthWest, null, tint = Tk.Dim, modifier = Modifier.size(18.dp))
                    }
                }
                item { SuggestionRow(Icons.Rounded.Search, tr("\"$q\" ke sab results (${results.size})", "All results for \"$q\" (${results.size})")) { submitted = true; onQueryDone(q) } }
            } else {
                item { SortFilterBar(sort, onSort, price, { price = it }) }
                item { Text(tr("${results.size} apps mile", "${results.size} apps found"), Modifier.padding(start = 18.dp, top = 4.dp, bottom = 6.dp), color = Tk.Muted, style = MaterialTheme.typography.bodySmall) }
                styledAppList(results, isNew, "r_", onOpen = onOpen)
                if (results.isEmpty()) item { Empty(tr("\"$q\" ka koi app nahi mila", "No app found for \"$q\"")) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowChips(items: List<String>, onClick: (String) -> Unit) {
    FlowRow(Modifier.padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { Chip(it, false) { onClick(it) } }
    }
}

@Composable
private fun SuggestionRow(icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Tk.Muted, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(16.dp))
        Text(text, color = Tk.White, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** "Naye" tab: bot ne kis din kya daala + app ka apna update. */
@Composable
fun NewScreen(apps: List<AppItem>, isNew: (AppItem) -> Boolean, updateCard: @Composable () -> Unit, onOpen: (AppItem) -> Unit, onBack: (() -> Unit)? = null) {
    val days = remember(apps) { Catalog.byDay(apps) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { ScreenTitle(tr("Naye Apps", "New apps"), tr("Har din 9 baje ek naya free app", "A new free app every day at 9 AM"), onBack) }
        item { updateCard() }
        days.forEachIndexed { i, (day, list) ->
            item(key = "d_$day") {
                Text(
                    when (i) { 0 -> "Latest • ${Dates.pretty(day)}"; else -> Dates.pretty(day).ifEmpty { tr("Pehle ke", "Earlier") } },
                    Modifier.padding(start = 18.dp, top = 18.dp, bottom = 4.dp), color = if (i == 0) Tk.Lime else Tk.White, style = MaterialTheme.typography.titleMedium,
                )
            }
            items(list, key = { "n_" + it.slug }) { a -> AppListItem(a, isNew = isNew(a)) { onOpen(a) } }
        }
    }
}
