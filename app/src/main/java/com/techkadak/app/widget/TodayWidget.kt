package com.techkadak.app.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider as FixedColor
import com.techkadak.app.Config
import com.techkadak.app.MainActivity
import com.techkadak.app.R
import com.techkadak.app.data.AppItem
import com.techkadak.app.data.AppRepository
import com.techkadak.app.data.Catalog
import com.techkadak.app.data.IconLoader
import com.techkadak.app.notify.Notifier

/** Home-screen widget "Aaj ka App": sabse naya (ya Remote Config ka featured) app, tap = app ka page. */
class TodayWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val apps = runCatching { AppRepository(context).loadLocal().apps }.getOrDefault(emptyList())
        val featured = context.getSharedPreferences("tk", 0).getString("rc_featured", null)
        val app = Catalog.featured(apps, featured)
        val icon = app?.iconUrl(Config.SITE_BASE)?.let { runCatching { IconLoader.load(context, it) }.getOrNull() }
        provideContent { TodayContent(app, icon) }
    }

    companion object {
        suspend fun refreshAll(context: Context) {
            if (GlanceAppWidgetManager(context).getGlanceIds(TodayWidget::class.java).isNotEmpty()) TodayWidget().updateAll(context)
        }
    }
}

private val Lime = Color(0xFF9BFF2E)
private val Ink = Color(0xFF0A0A0A)
private fun fixed(c: Color) = FixedColor(c)

@Composable
fun TodayContent(app: AppItem?, icon: Bitmap?) {
    val intent = Intent(androidx.glance.LocalContext.current, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        if (app != null) putExtra(Notifier.EXTRA_SLUG, app.slug)
    }
    Column(
        GlanceModifier.fillMaxSize().cornerRadius(26.dp).background(fixed(Color(0xFF0E0E0E))).padding(14.dp).clickable(actionStartActivity(intent)),
    ) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(GlanceModifier.cornerRadius(50.dp).background(fixed(Lime)).padding(horizontal = 9.dp, vertical = 3.dp)) {
                Text("AAJ KA APP", style = TextStyle(color = fixed(Ink), fontSize = 10.sp, fontWeight = FontWeight.Bold))
            }
            Spacer(GlanceModifier.defaultWeight())
            Image(ImageProvider(R.drawable.tk_logo), "Tech Kadak", GlanceModifier.size(22.dp).cornerRadius(11.dp))
        }
        Spacer(GlanceModifier.height(10.dp))
        if (app == null) {
            Text("Tech Kadak kholo — naye apps load honge", style = TextStyle(color = fixed(Color.White), fontSize = 13.sp))
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) Image(ImageProvider(icon), app.name, GlanceModifier.size(48.dp).cornerRadius(13.dp))
                else Box(GlanceModifier.size(48.dp).cornerRadius(13.dp).background(fixed(Color(0xFF1C1C1C))), contentAlignment = Alignment.Center) {
                    Text(app.initials, style = TextStyle(color = fixed(Lime), fontSize = 16.sp, fontWeight = FontWeight.Bold))
                }
                Spacer(GlanceModifier.width(12.dp))
                Column(GlanceModifier.defaultWeight()) {
                    Text(app.name, maxLines = 1, style = TextStyle(color = fixed(Color.White), fontSize = 17.sp, fontWeight = FontWeight.Bold))
                    Text(
                        app.pricing.uppercase() + " • " + Catalog.shortName(app.categories.firstOrNull() ?: ""), maxLines = 1,
                        style = TextStyle(color = fixed(Lime), fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    )
                }
            }
            Spacer(GlanceModifier.height(8.dp))
            Text(app.what, maxLines = 2, style = TextStyle(color = fixed(Color(0xFFBDBDBD)), fontSize = 12.sp))
        }
    }
}

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()
}
