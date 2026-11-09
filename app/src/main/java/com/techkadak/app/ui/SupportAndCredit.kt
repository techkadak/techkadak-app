package com.techkadak.app.ui

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.techkadak.app.BuildConfig
import com.techkadak.app.R
import com.techkadak.app.data.UpiLink

/** v1.5: "Support Tech Kadak ☕ Rs 50" — optional, UPI app se. Hidden if the build has no valid UPI ID. */
@Composable
fun SupportCard(modifier: Modifier = Modifier) {
    if (!UpiLink.isValidId(BuildConfig.UPI_ID)) return
    val ctx = LocalContext.current
    var noApp by remember { mutableStateOf(false) }
    // UPI app ka jawab: Status=SUCCESS hi "Shukriya" dikhata hai. Payment ki pakki khabar nahi deta.
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        val st = res.data?.getStringExtra("Status") ?: res.data?.getStringExtra("status")
        if (st.equals("success", ignoreCase = true)) Toast.makeText(ctx, tr("Shukriya! ☕", "Thank you! ☕"), Toast.LENGTH_LONG).show()
    }
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Tk.Surface2).clickable {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(UpiLink.payUri(BuildConfig.UPI_ID)))
            try { launcher.launch(intent) } catch (e: ActivityNotFoundException) { noApp = true }
        }.padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.LocalCafe, null, tint = Tk.Lime); Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(tr("Support Tech Kadak ☕ Rs 50", "Support Tech Kadak ☕ Rs 50"), color = Tk.White, style = MaterialTheme.typography.titleSmall)
            Text(
                tr("Bilkul optional. Tap karoge to UPI app me Rs 50 pehle se bhara milega. Koi pop-up ya paywall nahi.",
                    "Totally optional. Tap and your UPI app opens with Rs 50 filled in. No pop-ups, no paywall."),
                color = Tk.Muted, style = MaterialTheme.typography.bodySmall,
            )
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = Tk.Dim)
    }
    if (noApp) {
        AlertDialog(
            onDismissRequest = { noApp = false },
            title = { Text(tr("UPI app nahi mila", "No UPI app found")) },
            text = {
                Text(tr("Is phone pe koi UPI app nahi mila. Ye UPI ID copy karke kisi bhi UPI app me daal sakte ho: ${BuildConfig.UPI_ID}",
                    "No UPI app on this phone. You can copy this UPI ID into any UPI app: ${BuildConfig.UPI_ID}"))
            },
            confirmButton = {
                TextButton(onClick = {
                    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("UPI ID", BuildConfig.UPI_ID))
                    Toast.makeText(ctx, tr("ID copy ho gaya", "ID copied"), Toast.LENGTH_SHORT).show()
                    noApp = false
                }) { Text(tr("ID copy karo", "Copy ID")) }
            },
            dismissButton = { TextButton(onClick = { noApp = false }) { Text(tr("Band karo", "Close")) } },
        )
    }
}

/** v1.5: About QR card — white rounded plate, lime border; QR opens the latest release page (official GitHub). */
const val RELEASE_URL = "https://github.com/techkadak/techkadak-app/releases/latest"

@Composable
fun ReleaseQrCard(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Tk.Surface2).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            tr("App download karo", "Download the app"),
            color = Tk.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Box(
            Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White).border(3.dp, Tk.Lime, RoundedCornerShape(20.dp))
                .clickable { runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(RELEASE_URL))) } }
                .padding(12.dp),
        ) {
            Image(
                painterResource(R.drawable.about_qr),
                contentDescription = tr("Latest release ka QR code", "QR code for the latest release"),
                modifier = Modifier.size(176.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            tr("Scan karo, seedha GitHub se download", "Scan to download straight from GitHub"),
            color = Tk.Muted, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = {
                val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("Tech Kadak link", RELEASE_URL))
                Toast.makeText(ctx, tr("Link copy ho gaya", "Link copied"), Toast.LENGTH_SHORT).show()
            }) { Icon(Icons.Rounded.ContentCopy, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text(tr("Link copy", "Copy link")) }
            Button(
                onClick = {
                    val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "Tech Kadak app: $RELEASE_URL") }
                    runCatching { ctx.startActivity(Intent.createChooser(send, null)) }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Tk.Lime, contentColor = Color.Black),
            ) { Icon(Icons.Rounded.Share, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text(tr("Share", "Share")) }
        }
    }
}

/** v1.5: "Created by Rohit Saini" — pro studio credit line (letter-spaced, small). */
@Composable
fun CreditLine(modifier: Modifier = Modifier) {
    Text(
        "Created by Rohit Saini", modifier = modifier, color = Tk.Dim,
        fontSize = 11.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center,
    )
}
