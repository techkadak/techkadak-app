package com.techkadak.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.techkadak.app.auth.TkUser
import com.techkadak.app.reviews.Review
import com.techkadak.app.reviews.ReviewLogic
import com.techkadak.app.reviews.ReviewRepo
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/** Own-design review icon: a lime speech bubble with a star cut out of it. */
@Composable
fun ReviewBubbleIcon(size: Dp = 24.dp, tint: Color = Tk.Lime, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val w = this.size.width; val h = this.size.height
        val bubble = Path().apply {
            addRoundRect(androidx.compose.ui.geometry.RoundRect(0.06f * w, 0.08f * h, 0.94f * w, 0.76f * h, androidx.compose.ui.geometry.CornerRadius(0.22f * w)))
            moveTo(0.26f * w, 0.70f * h); lineTo(0.20f * w, 0.95f * h); lineTo(0.50f * w, 0.74f * h); close()
        }
        drawPath(bubble, tint)
        drawStar(Offset(0.5f * w, 0.41f * h), 0.27f * w, Color(0xFF0A0A0A))
    }
}

private fun DrawScope.drawStar(c: Offset, r: Float, color: Color) {
    val p = Path()
    for (i in 0 until 10) {
        val rad = if (i % 2 == 0) r else r * 0.45f
        val a = Math.toRadians((-90 + i * 36).toDouble())
        val x = c.x + (rad * cos(a)).toFloat(); val y = c.y + (rad * sin(a)).toFloat()
        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    p.close(); drawPath(p, color)
}

@Composable
fun StarRow(rating: Float, size: Dp = 16.dp, onPick: ((Int) -> Unit)? = null) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        for (i in 1..5) {
            val on = rating >= i - 0.25f
            Canvas(Modifier.size(size).then(if (onPick != null) Modifier.clickable { onPick(i) } else Modifier)) {
                drawStar(Offset(this.size.width / 2, this.size.height / 2 + 1f), this.size.width * 0.48f, if (on) Color(0xFF9BFF2E) else Color(0x33FFFFFF))
            }
        }
    }
}

/** Reviews block on the app page. Real reviews only; writing needs a (free, optional) login. */
@Composable
fun ReviewsSection(slug: String, appName: String, user: TkUser?, onLogin: () -> Unit, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var reviews by remember(slug) { mutableStateOf<List<Review>?>(null) }
    var failed by remember(slug) { mutableStateOf(false) }
    var write by remember { mutableStateOf(false) }
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(slug, tick) {
        ReviewRepo.load(ctx, slug).onSuccess { reviews = it; failed = false }.onFailure { failed = true; if (reviews == null) reviews = emptyList() }
    }
    val list = reviews
    val sum = remember(list) { ReviewLogic.summary(list.orEmpty()) }
    val mine = user?.let { u -> list?.firstOrNull { it.uid == u.uid } }
    Column(modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ReviewBubbleIcon(26.dp)
            Spacer(Modifier.width(10.dp))
            Text(tr("Reviews", "Reviews"), Modifier.weight(1f), color = Tk.White, style = MaterialTheme.typography.titleLarge)
            if (sum.count > 0) { Text(ReviewLogic.avgLabel(sum.average), color = Tk.Lime, style = MaterialTheme.typography.titleLarge); Spacer(Modifier.width(6.dp)); StarRow(sum.average, 14.dp) }
        }
        if (sum.count > 0) Text("${sum.count} " + tr("real reviews", "real reviews"), color = Tk.Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 2.dp))
        Spacer(Modifier.height(10.dp))
        when {
            list == null -> Text(tr("Reviews aa rahe hain…", "Loading reviews…"), color = Tk.Muted, style = MaterialTheme.typography.bodyMedium)
            list.isEmpty() -> Text(
                if (failed) tr("Reviews abhi load nahi hue. Baad me dekho.", "Reviews couldn't load right now.") else ReviewLogic.EMPTY_STATE,
                color = Tk.Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("reviews_empty"),
            )
            else -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                list.take(8).forEach { r ->
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Tk.Surface).border(1.dp, Tk.Line, RoundedCornerShape(16.dp)).padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StarRow(r.rating.toFloat(), 14.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(r.name.ifBlank { tr("Tech Kadak user", "Tech Kadak user") }, Modifier.weight(1f), color = Tk.White, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(ReviewLogic.timeAgo(System.currentTimeMillis(), r.atMs), color = Tk.Dim, fontSize = 11.sp)
                        }
                        if (r.text.isNotBlank()) Text(r.text, color = Tk.TextSoft, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Box(
            Modifier.clip(RoundedCornerShape(50)).background(Tk.LimeSoft).border(1.dp, Tk.LimeBorder, RoundedCornerShape(50)).clickable { if (user == null) onLogin() else write = true }
                .padding(horizontal = 18.dp, vertical = 10.dp).testTag("review_write_btn"),
        ) {
            Text(if (mine != null) tr("Apna review badlo", "Edit your review") else tr("Review likho", "Write a review") + if (user == null) tr(" (login chahiye)", " (login needed)") else "", color = Tk.Lime, fontSize = 14.sp)
        }
    }
    if (write && user != null) ReviewDialog(appName, user.displayName.takeIf { it != "Tech Kadak user" }.orEmpty(), mine, onDismiss = { write = false }) { rating, text, name ->
        var err: String? = null
        val r = ReviewRepo.submit(ctx, slug, user.uid, name, rating, text)
        r.onFailure { e ->
            err = if ((e as? com.google.firebase.firestore.FirebaseFirestoreException)?.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED)
                tr("Abhi review nahi ja raha (verify kiya hua login chahiye)", "Can't post yet (a verified login is needed)")
            else e.message?.takeIf { it.isNotBlank() && it != "offline" } ?: tr("Internet check karo", "Check your internet")
        }
        if (r.isSuccess) { write = false; tick++ }
        err
    }
}

@Composable
private fun ReviewDialog(appName: String, defaultName: String, existing: Review?, onDismiss: () -> Unit, onSend: suspend (Int, String, String) -> String?) {
    var rating by remember { mutableIntStateOf(existing?.rating ?: 0) }
    var text by remember { mutableStateOf(existing?.text.orEmpty()) }
    var name by remember { mutableStateOf(existing?.name?.takeIf { it.isNotBlank() } ?: defaultName) }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    Dialog(onDismissRequest = { if (!busy) onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.padding(20.dp).fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(Tk.Surface).border(1.dp, Tk.LimeBorder, RoundedCornerShape(26.dp)).padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { ReviewBubbleIcon(26.dp); Spacer(Modifier.width(10.dp)); Text(appName, color = Tk.White, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            Spacer(Modifier.height(14.dp))
            StarRow(rating.toFloat(), 38.dp) { rating = it }
            Spacer(Modifier.height(12.dp))
            Field(text, { if (it.length <= ReviewLogic.TEXT_MAX) text = it }, tr("Kaisa laga? (optional)", "How was it? (optional)"), singleLine = false, minHeight = 90.dp, tag = "review_text")
            Text("${text.length}/${ReviewLogic.TEXT_MAX}", color = Tk.Dim, fontSize = 11.sp, modifier = Modifier.align(Alignment.End))
            Spacer(Modifier.height(6.dp))
            Field(name, { if (it.length <= ReviewLogic.NAME_MAX) name = it }, tr("Naam (optional)", "Name (optional)"), singleLine = true)
            err?.let { Text(it, color = Color(0xFFFF6B6B), fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp)) }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f).clip(RoundedCornerShape(50)).border(1.dp, Tk.Line, RoundedCornerShape(50)).clickable(enabled = !busy) { onDismiss() }.padding(vertical = 12.dp), contentAlignment = Alignment.Center) { Text(tr("Baad me", "Later"), color = Tk.White) }
                Box(Modifier.weight(1f).clip(RoundedCornerShape(50)).background(Tk.Lime).clickable(enabled = !busy) {
                    val c = ReviewLogic.check(rating, text)
                    if (c is ReviewLogic.Check.Bad) { err = tr(c.hi, c.en); return@clickable }
                    busy = true; err = null
                    scope.launch { err = onSend(rating, text, name); busy = false }
                }.padding(vertical = 12.dp).testTag("review_send"), contentAlignment = Alignment.Center) {
                    Text(if (busy) "…" else tr("Bhejo", "Post"), color = Tk.OnLime, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
internal fun Field(value: String, onChange: (String) -> Unit, hint: String, singleLine: Boolean, minHeight: Dp = 44.dp, tag: String? = null) {
    Box(
        Modifier.fillMaxWidth().heightIn(min = minHeight).clip(RoundedCornerShape(16.dp)).background(Tk.Surface2).border(1.dp, Tk.Line, RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart,
    ) {
        if (value.isEmpty()) Text(hint, color = Tk.Dim, fontSize = 14.sp)
        BasicTextField(
            value, onChange, singleLine = singleLine, textStyle = TextStyle(color = Tk.White, fontSize = 14.sp), cursorBrush = SolidColor(Tk.Lime),
            keyboardOptions = KeyboardOptions(imeAction = if (singleLine) ImeAction.Done else ImeAction.Default),
            modifier = Modifier.fillMaxWidth().then(if (tag != null) Modifier.testTag(tag) else Modifier),
        )
    }
}

/** Feedback screen: "Aap kya feature chahte ho?" (max 500 chars, optional name/contact, 3 per day per install). */
@Composable
fun FeedbackScreen(onBack: () -> Unit, defaultName: String = "") {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    var name by remember { mutableStateOf(defaultName) }
    var contact by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    val left = remember(done) { (ReviewLogic.FEEDBACK_PER_DAY - ReviewRepo.usedToday(ctx)).coerceAtLeast(0) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, tr("Wapas", "Back"), tint = Tk.White) }
            Text(tr("Suggestion", "Suggestion"), color = Tk.White, style = MaterialTheme.typography.titleLarge)
        }
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text(tr("Aap kya feature chahte ho?", "What feature do you want?"), color = Tk.White, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.testTag("feedback_title"))
            Spacer(Modifier.height(6.dp))
            Text(tr("Jo bhi chahiye seedha likho. Owner khud padhta hai.", "Write whatever you need. The owner reads it himself."), color = Tk.Muted, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(16.dp))
            if (done) {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Tk.LimeSoft).border(1.dp, Tk.LimeBorder, RoundedCornerShape(20.dp)).padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🙏", fontSize = 34.sp)
                    Text(tr("Shukriya! Mil gaya.", "Thank you! Received."), color = Tk.White, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 6.dp))
                    Text(tr("Owner isse dekhega.", "The owner will see it."), color = Tk.Muted, style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                Field(text, { if (it.length <= ReviewLogic.FEEDBACK_MAX) text = it }, tr("Apna idea likho…", "Write your idea…"), singleLine = false, minHeight = 140.dp, tag = "feedback_text")
                Text("${text.length}/${ReviewLogic.FEEDBACK_MAX}", color = Tk.Dim, fontSize = 11.sp, modifier = Modifier.align(Alignment.End).padding(top = 4.dp))
                Spacer(Modifier.height(8.dp))
                Field(name, { if (it.length <= ReviewLogic.NAME_MAX) name = it }, tr("Naam (optional)", "Name (optional)"), singleLine = true)
                Spacer(Modifier.height(8.dp))
                Field(contact, { if (it.length <= ReviewLogic.CONTACT_MAX) contact = it }, tr("Contact: email / Telegram (optional)", "Contact: email / Telegram (optional)"), singleLine = true)
                err?.let { Text(it, color = Color(0xFFFF6B6B), fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp)) }
                Spacer(Modifier.height(16.dp))
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(if (left > 0) Tk.Lime else Tk.Surface2).clickable(enabled = !busy && left > 0) {
                        val c = ReviewLogic.checkFeedback(text)
                        if (c is ReviewLogic.Check.Bad) { err = tr(c.hi, c.en); return@clickable }
                        busy = true; err = null
                        scope.launch {
                            ReviewRepo.sendFeedback(ctx, text, name, contact).onSuccess { done = true }.onFailure { err = it.message?.takeIf { m -> m != "offline" } ?: tr("Abhi feedback band hai, baad me try karo", "Feedback is unavailable right now") }
                            busy = false
                        }
                    }.padding(vertical = 14.dp).testTag("feedback_send"),
                    contentAlignment = Alignment.Center,
                ) { Text(if (busy) "…" else tr("Bhejo", "Send"), color = if (left > 0) Tk.OnLime else Tk.Dim, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) }
                Text(tr("Aaj $left feedback bacha hai (roz 3)", "$left left today (3 per day)"), color = Tk.Dim, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}
