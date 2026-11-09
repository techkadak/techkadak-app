package com.techkadak.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.techkadak.app.R
import com.techkadak.app.auth.AuthManager
import com.techkadak.app.auth.TkUser
import com.techkadak.app.data.IconLoader
import kotlinx.coroutines.delay

class AuthActions(
    val onGoogle: () -> Unit = {},
    val onSignUp: (name: String, email: String, pass: String) -> Unit = { _, _, _ -> },
    val onLogin: (email: String, pass: String) -> Unit = { _, _ -> },
    val onForgot: (email: String) -> Unit = {},
    val onSkip: () -> Unit = {},
    val onPhone: (() -> Unit)? = null,
)

/** Login / Sign up — OPTIONAL. "Abhi skip karo" se bina account ke poora app. */
@Composable
fun LoginScreen(enabled: Boolean, busy: Boolean, error: String?, info: String?, act: AuthActions, startWithEmail: Boolean = false, onBack: (() -> Unit)? = null) {
    var email by remember { mutableStateOf(startWithEmail) }
    var signUp by remember { mutableStateOf(true) }
    var name by remember { mutableStateOf("") }
    var mail by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var show by remember { mutableStateOf(false) }
    var localErr by remember { mutableStateOf<String?>(null) }
    Box(Modifier.fillMaxSize().background(Tk.Black)) {
        Box(Modifier.size(380.dp).align(Alignment.TopStart).graphicsLayer { translationX = -180f; translationY = -140f }.background(Brush.radialGradient(listOf(Tk.Glow, Color.Transparent))))
        Column(Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) IconButton(onClick = onBack, Modifier.offset(x = (-12).dp)) { Icon(Icons.Rounded.Close, tr("Band karo", "Close"), tint = Tk.White) }
                Spacer(Modifier.weight(1f))
                Text(
                    tr("Abhi skip karo →", "Skip for now →"), Modifier.clip(RoundedCornerShape(50)).background(Tk.Surface2).clickable(onClick = act.onSkip).padding(horizontal = 14.dp, vertical = 8.dp),
                    color = Tk.White, style = MaterialTheme.typography.labelLarge,
                )
            }
            Spacer(Modifier.height(22.dp))
            Image(painterResource(R.drawable.tk_logo), null, Modifier.size(72.dp).clip(RoundedCornerShape(22.dp)).border(2.dp, Tk.Lime, RoundedCornerShape(22.dp)), contentScale = ContentScale.Crop)
            Spacer(Modifier.height(18.dp))
            Text(
                if (!email) tr("Account banao\n(optional) ✨", "Create an account\n(optional) ✨") else if (signUp) tr("Email se\nsign up", "Sign up\nwith email") else tr("Wapas\naao 👋", "Welcome\nback 👋"),
                color = Tk.White, fontFamily = Display, fontWeight = FontWeight.Black, fontSize = 30.sp, lineHeight = 36.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                tr("Login se favourites aur profile tumhare account se jud jaate hain. Bina login bhi app poora chalega — browse, search, download sab.",
                    "An account keeps your favourites and profile. Without one the app still works fully — browse, search and download."),
                color = Tk.Muted, style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(22.dp))
            if (!enabled) Notice(Icons.Rounded.CloudOff, tr("Login abhi band hai (Firebase set nahi). Skip karke app use karo.", "Login is unavailable right now. Skip and use the app."))
            AnimatedContent(email, label = "auth") { isEmail ->
                Column {
                    if (!isEmail) {
                        AuthButton(tr("Google se continue", "Continue with Google"), Icons.Rounded.AccountCircle, primary = true, enabled = enabled && !busy, onClick = act.onGoogle)
                        Spacer(Modifier.height(12.dp))
                        AuthButton(tr("Email se continue", "Continue with email"), Icons.Rounded.Email, primary = false, enabled = enabled && !busy) { email = true }
                        act.onPhone?.let { ph ->
                            Spacer(Modifier.height(12.dp))
                            AuthButton(tr("Phone number (OTP)", "Phone number (OTP)"), Icons.Rounded.Smartphone, primary = false, enabled = enabled && !busy, onClick = ph)
                        }
                    } else {
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(Tk.Surface2).padding(4.dp)) {
                            Seg(tr("Naya account", "Sign up"), signUp, Modifier.weight(1f)) { signUp = true; localErr = null }
                            Seg(tr("Login", "Log in"), !signUp, Modifier.weight(1f)) { signUp = false; localErr = null }
                        }
                        Spacer(Modifier.height(16.dp))
                        if (signUp) { Field(name, { name = it }, tr("Naam", "Name"), Icons.Rounded.Person); Spacer(Modifier.height(10.dp)) }
                        Field(mail, { mail = it.trim() }, "Email", Icons.Rounded.AlternateEmail, KeyboardType.Email)
                        Spacer(Modifier.height(10.dp))
                        Field(pass, { pass = it }, tr("Password (6+ akshar)", "Password (6+ chars)"), Icons.Rounded.Lock, KeyboardType.Password, secret = !show) {
                            IconButton(onClick = { show = !show }) { Icon(if (show) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, null, tint = Tk.Muted) }
                        }
                        Spacer(Modifier.height(16.dp))
                        LimeButton(
                            if (busy) tr("RUKO…", "WAIT…") else if (signUp) tr("ACCOUNT BANAO", "CREATE ACCOUNT") else tr("LOGIN KARO", "LOG IN"),
                            Modifier.fillMaxWidth(), Icons.AutoMirrored.Rounded.ArrowForward,
                        ) {
                            localErr = when {
                                busy || !enabled -> null
                                !AuthManager.validEmail(mail) -> tr("Sahi email daalo", "Enter a valid email")
                                pass.length < 6 -> tr("Password kam se kam 6 akshar", "Password needs 6+ characters")
                                else -> { if (signUp) act.onSignUp(name, mail, pass) else act.onLogin(mail, pass); null }
                            }
                        }
                        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(tr("← Google se", "← Use Google"), Modifier.clickable { email = false }.padding(8.dp), color = Tk.Muted, style = MaterialTheme.typography.labelLarge)
                            if (!signUp) Text(tr("Password bhool gaye?", "Forgot password?"), Modifier.clickable {
                                if (AuthManager.validEmail(mail)) act.onForgot(mail) else localErr = tr("Pehle email likho", "Type your email first")
                            }.padding(8.dp), color = Tk.Lime, style = MaterialTheme.typography.labelLarge)
                        }
                        if (signUp) Text(
                            tr("Sign up ke baad tumhare email pe verification link aayega 📩", "After sign-up we email you a verification link 📩"),
                            Modifier.padding(top = 4.dp), color = Tk.Dim, style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 14.dp).height(3.dp).clip(CircleShape), color = Tk.Lime, trackColor = Tk.Surface2)
            (localErr ?: error)?.let { Spacer(Modifier.height(12.dp)); Notice(Icons.Rounded.ErrorOutline, it, warn = true) }
            info?.let { Spacer(Modifier.height(12.dp)); Notice(Icons.Rounded.CheckCircle, it) }
            Spacer(Modifier.height(28.dp))
            Text(
                tr("Hum tumhara password kabhi nahi dekhte. Login Firebase (Google) se hota hai. Account kabhi bhi Profile se delete kar sakte ho.",
                    "We never see your password. Sign-in is handled by Firebase. You can delete your account any time from Profile."),
                color = Tk.Dim, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

/**
 * "Mail check karo" — email verification (Firebase link). ROHIT ke reference wala animated card:
 * boxes wait karte hain (scan glow), verify hote hi ghoom ke ek lime square me milte hain ✅.
 */
@Composable
fun VerifyEmailScreen(
    email: String, busy: Boolean, error: String?, info: String?, verified: Boolean,
    onResend: () -> Unit, onCheck: () -> Unit, onWrongEmail: () -> Unit, onSkip: () -> Unit, onOpenMail: () -> Unit, onContinue: () -> Unit,
    cooldownStart: Int = 30, successOverride: Float? = null, focusOverride: Int? = null,
) {
    var cooldown by remember { mutableIntStateOf(cooldownStart) }
    LaunchedEffect(cooldown) { if (cooldown > 0) { delay(1000); cooldown-- } }
    var scan by remember { mutableIntStateOf(0) }
    LaunchedEffect(verified) { while (!verified) { delay(650); scan = (scan + 1) % 4 } }
    val anim = rememberSuccessProgress(verified)
    val success = successOverride ?: anim
    LaunchedEffect(success) { if (success != null && success >= 1f && successOverride == null) { delay(500); onContinue() } }
    Column(Modifier.fillMaxSize().background(Tk.Black).systemBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text(tr("Baad me →", "Later →"), Modifier.clip(RoundedCornerShape(50)).background(Tk.Surface2).clickable(onClick = onSkip).padding(horizontal = 14.dp, vertical = 8.dp), color = Tk.White, style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.height(18.dp))
        Text("TECH KADAK", color = Tk.Lime, fontFamily = Display, fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 3.sp)
        Text(tr("Email verification", "Email verification"), color = Tk.White, fontFamily = Display, fontWeight = FontWeight.Black, fontSize = 24.sp)
        Spacer(Modifier.height(18.dp))
        VerifyCard(
            if (success != null) tr("Verify ho gaya! 🎉", "Verified! 🎉") else tr("Mail check karo 📩", "Check your mail 📩"),
            if (success != null) tr("Account ready hai — chalo shuru karein", "Your account is ready") else tr("Verification link bheja hai: $email", "We sent a verification link to $email"),
        ) {
            val slots = if (success != null) List(4) { "✓" } else List(4) { "" }
            OtpBoxes(slots, focusOverride ?: scan, success)
            if (success == null) {
                AutofillChip(tr("Mail · Tech Kadak", "Mail · Tech Kadak"), tr("Link pe tap karke wapas aao", "Tap the link, then come back"), tr("Inbox kholo", "Open inbox"), onOpenMail)
                Spacer(Modifier.height(14.dp))
                LimeButton(if (busy) tr("CHECK HO RAHA…", "CHECKING…") else tr("MAINE VERIFY KAR LIYA", "I'VE VERIFIED"), Modifier.fillMaxWidth(), Icons.Rounded.Verified) { if (!busy) onCheck() }
                Spacer(Modifier.height(12.dp))
                Text(
                    if (cooldown > 0) tr("Mail nahi aaya? ${cooldown}s me dobara bhejo", "Didn't get it? Resend in ${cooldown}s")
                    else tr("Mail nahi aaya? Dobara bhejo", "Didn't get it? Resend now"),
                    Modifier.clip(RoundedCornerShape(50)).clickable(enabled = cooldown == 0 && !busy) { onResend(); cooldown = 30 }.padding(8.dp),
                    color = if (cooldown == 0) Tk.Lime else Tk.Muted, style = MaterialTheme.typography.labelLarge,
                )
            }
        }
        if (success == null) {
            Spacer(Modifier.height(14.dp))
            Text(tr("Spam / Promotions folder bhi dekhna. Link 1 baar tap karna kaafi hai.", "Check Spam / Promotions too. One tap on the link is enough."), color = Tk.Dim, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        }
        error?.let { Spacer(Modifier.height(12.dp)); Notice(Icons.Rounded.ErrorOutline, it, warn = true) }
        info?.let { Spacer(Modifier.height(12.dp)); Notice(Icons.Rounded.CheckCircle, it) }
        if (success == null) Text(tr("Galat email? Dusre email se banao", "Wrong email? Use another one"), Modifier.padding(top = 10.dp).clickable(onClick = onWrongEmail).padding(8.dp), color = Tk.Muted, style = MaterialTheme.typography.labelLarge)
    }
}


/** Phone number + 6-digit OTP (Firebase SMS) — ROHIT ke reference wala animated verify card. */
@Composable
fun PhoneLoginScreen(
    codeSent: Boolean, phone: String, busy: Boolean, error: String?, info: String?, autoCode: String?, verified: Boolean,
    onSend: (String, Boolean) -> Unit, onVerify: (String) -> Unit, onBack: () -> Unit, onSkip: () -> Unit, onContinue: () -> Unit,
    codeOverride: String? = null, successOverride: Float? = null,
) {
    var number by remember { mutableStateOf("") }
    var code by remember { mutableStateOf(codeOverride ?: "") }
    LaunchedEffect(autoCode) { if (autoCode != null && autoCode.length == 6) code = autoCode }
    var cooldown by remember(codeSent) { mutableIntStateOf(if (codeSent) 30 else 0) }
    LaunchedEffect(cooldown) { if (cooldown > 0) { delay(1000); cooldown-- } }
    val anim = rememberSuccessProgress(verified)
    val success = successOverride ?: anim
    LaunchedEffect(success) { if (success != null && success >= 1f && successOverride == null) { delay(500); onContinue() } }
    val clip = androidx.compose.ui.platform.LocalClipboardManager.current
    val clipCode = remember(codeSent) { runCatching { clip.getText()?.text?.let { Regex("\\b(\\d{6})\\b").find(it)?.groupValues?.get(1) } }.getOrNull() }
    val focus = remember { androidx.compose.ui.focus.FocusRequester() }
    Column(Modifier.fillMaxSize().background(Tk.Black).systemBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, Modifier.offset(x = (-12).dp)) { Icon(Icons.Rounded.Close, tr("Wapas", "Back"), tint = Tk.White) }
            Spacer(Modifier.weight(1f))
            Text(tr("Abhi skip karo →", "Skip for now →"), Modifier.clip(RoundedCornerShape(50)).background(Tk.Surface2).clickable(onClick = onSkip).padding(horizontal = 14.dp, vertical = 8.dp), color = Tk.White, style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.height(18.dp))
        Text("TECH KADAK", color = Tk.Lime, fontFamily = Display, fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 3.sp)
        Text(tr("Phone se login", "Phone login"), color = Tk.White, fontFamily = Display, fontWeight = FontWeight.Black, fontSize = 24.sp)
        Spacer(Modifier.height(18.dp))
        if (!codeSent) {
            VerifyCard(tr("Apna number daalo", "Enter your number"), tr("6-digit OTP SMS se aayega", "We'll text you a 6-digit OTP")) {
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth().height(58.dp).clip(RoundedCornerShape(16.dp)).background(Tk.Black).border(1.dp, if (number.isNotEmpty()) Tk.LimeBorder else Tk.Line, RoundedCornerShape(16.dp)).padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("🇮🇳 +91", color = Tk.White, style = MaterialTheme.typography.titleMedium)
                    Box(Modifier.padding(horizontal = 12.dp).width(1.dp).height(26.dp).background(Tk.Line))
                    Box(Modifier.weight(1f)) {
                        if (number.isEmpty()) Text("98765 43210", color = Tk.Dim, style = MaterialTheme.typography.titleMedium)
                        BasicTextField(
                            number, { number = it.filter(Char::isDigit).take(10) }, Modifier.fillMaxWidth(), singleLine = true,
                            textStyle = MaterialTheme.typography.titleMedium.copy(color = Tk.White, letterSpacing = 1.sp), cursorBrush = SolidColor(Tk.Lime),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                LimeButton(if (busy) tr("BHEJ RAHE…", "SENDING…") else tr("OTP BHEJO", "SEND OTP"), Modifier.fillMaxWidth(), Icons.AutoMirrored.Rounded.ArrowForward) { if (!busy) onSend(number, false) }
                Text(tr("Normal SMS charges lag sakte hain. Number sirf login ke liye.", "Standard SMS rates may apply. Your number is only used to log in."),
                    Modifier.padding(top = 10.dp), color = Tk.Dim, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            }
        } else {
            VerifyCard(
                if (success != null) tr("Verify ho gaya! 🎉", "Verified! 🎉") else tr("Number verify karo", "Verify your number"),
                if (success != null) tr("Login ho gaya — chalo shuru karein", "You're in") else tr("6-digit code bheja hai $phone pe", "Enter the 6-digit code sent to $phone"),
            ) {
                val slots = List(6) { i -> code.getOrNull(i)?.toString() ?: "" }
                Box(contentAlignment = Alignment.Center) {
                    OtpBoxes(slots, code.length.coerceAtMost(5), success, boxSize = 42)
                    if (success == null) BasicTextField(
                        code, { v -> val d = v.filter(Char::isDigit).take(6); code = d; if (d.length == 6) onVerify(d) },
                        Modifier.matchParentSize().focusRequester(focus).graphicsLayer { alpha = 0f },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    )
                }
                LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
                if (success == null) {
                    val chipCode = autoCode ?: clipCode
                    if (chipCode != null) {
                        AutofillChip(tr("Message · abhi", "Message · now"), "OTP $chipCode " + tr("tumhara verification code hai", "is your verification code"), tr("Paste", "Paste")) {
                            code = chipCode; onVerify(chipCode)
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                    LimeButton(if (busy) tr("CHECK HO RAHA…", "CHECKING…") else tr("VERIFY KARO", "VERIFY"), Modifier.fillMaxWidth(), Icons.Rounded.Verified) { if (!busy && code.length == 6) onVerify(code) }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        if (cooldown > 0) tr("Code nahi aaya? ${cooldown}s me dobara bhejo", "Didn't receive the code? Resend in ${cooldown}s") else tr("Code nahi aaya? Dobara bhejo", "Didn't receive it? Resend"),
                        Modifier.clip(RoundedCornerShape(50)).clickable(enabled = cooldown == 0 && !busy) { onSend(phone, true); cooldown = 30 }.padding(8.dp),
                        color = if (cooldown == 0) Tk.Lime else Tk.Muted, style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
        error?.let { Spacer(Modifier.height(12.dp)); Notice(Icons.Rounded.ErrorOutline, it, warn = true) }
        if (success == null) info?.let { Spacer(Modifier.height(12.dp)); Notice(Icons.Rounded.CheckCircle, it) }
    }
}

/** Profile tab ka account card: guest = login ka button; login = naam, photo, email, verified badge. */
@Composable
fun AccountCard(user: TkUser?, enabled: Boolean, onLogin: () -> Unit, onVerify: () -> Unit, onLogout: () -> Unit, onDelete: () -> Unit) {
    var confirmDelete by remember { mutableStateOf(false) }
    Column(Modifier.padding(horizontal = 14.dp, vertical = 6.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Tk.Surface).border(1.dp, Tk.Line, RoundedCornerShape(22.dp)).padding(16.dp)) {
        if (user == null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(52.dp).clip(CircleShape).background(Tk.Surface2), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.PersonOutline, null, tint = Tk.Muted, modifier = Modifier.size(28.dp)) }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(tr("Guest mode", "Guest mode"), color = Tk.White, style = MaterialTheme.typography.titleMedium)
                    Text(tr("Login optional hai — Google ya Email se", "Login is optional — Google or email"), color = Tk.Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(12.dp))
            LimeButton(tr("LOGIN / SIGN UP", "LOG IN / SIGN UP"), Modifier.fillMaxWidth(), Icons.Rounded.Login, onClick = onLogin)
            if (!enabled) Text(tr("(Login abhi band hai)", "(Login unavailable)"), color = Tk.Dim, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(user, 56)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(user.displayName, color = Tk.White, style = MaterialTheme.typography.titleMedium, maxLines = 1, modifier = Modifier.weight(1f, fill = false))
                        if (!user.needsVerification) { Spacer(Modifier.width(6.dp)); Icon(Icons.Rounded.Verified, tr("Verified", "Verified"), tint = Tk.Lime, modifier = Modifier.size(18.dp)) }
                    }
                    Text(user.email ?: user.phone ?: "", color = Tk.Muted, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                    Text(
                        if (user.provider == "google") tr("Google account • verified", "Google account • verified")
                        else if (user.provider == "phone") tr("Phone verified ✅", "Phone verified ✅")
                        else if (user.emailVerified) tr("Email verified ✅", "Email verified ✅") else tr("Email verify nahi hua", "Email not verified"),
                        color = if (user.needsVerification) Tk.White else Tk.Lime, style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            if (user.needsVerification) {
                Spacer(Modifier.height(10.dp))
                LimeButton(tr("EMAIL VERIFY KARO", "VERIFY EMAIL"), Modifier.fillMaxWidth(), Icons.Rounded.MarkEmailUnread, onClick = onVerify)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlineButton(tr("Logout", "Log out"), Modifier.weight(1f), Icons.AutoMirrored.Rounded.Logout, onClick = onLogout)
                Text(
                    tr("Account delete", "Delete account"), Modifier.align(Alignment.CenterVertically).clickable { confirmDelete = true }.padding(8.dp),
                    color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false }, containerColor = Tk.Surface,
        title = { Text(tr("Account delete karein?", "Delete account?"), color = Tk.White) },
        text = { Text(tr("Tumhara Tech Kadak login hamesha ke liye hat jayega. App phir bhi guest mode me chalega.", "Your Tech Kadak login is removed for good. The app keeps working in guest mode."), color = Tk.Muted) },
        confirmButton = { TextButton({ confirmDelete = false; onDelete() }) { Text(tr("Haan, delete", "Yes, delete"), color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton({ confirmDelete = false }) { Text(tr("Nahi", "Cancel"), color = Tk.Lime) } },
    )
}

@Composable
fun Avatar(user: TkUser, sizeDp: Int) {
    val ctx = LocalContext.current
    var bmp by remember(user.photoUrl) { mutableStateOf(user.photoUrl?.let { IconLoader.peek(it) }) }
    LaunchedEffect(user.photoUrl) { val u = user.photoUrl; if (bmp == null && u != null && u.startsWith("https://")) bmp = IconLoader.load(ctx, u) }
    Box(Modifier.size(sizeDp.dp).clip(CircleShape).background(Tk.Lime).border(2.dp, Tk.Lime, CircleShape), contentAlignment = Alignment.Center) {
        val b = bmp
        if (b != null) androidx.compose.foundation.Image(b.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Text(user.displayName.take(1).uppercase(), color = Tk.OnLime, fontFamily = Display, fontWeight = FontWeight.Black, fontSize = (sizeDp * 0.42f).sp)
    }
}

@Composable
private fun AuthButton(text: String, icon: ImageVector, primary: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Row(
        Modifier.fillMaxWidth().height(56.dp).clip(shape)
            .background(if (primary) Tk.White else Tk.Surface2).border(1.dp, if (primary) Tk.White else Tk.Line, shape)
            .then(if (enabled) Modifier.pressable(onClick) else Modifier.graphicsLayer { alpha = 0.45f }).padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = if (primary) Tk.Black else Tk.Lime, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, color = if (primary) Tk.Black else Tk.White, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun Seg(text: String, sel: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Text(
        text, modifier.clip(RoundedCornerShape(50)).background(if (sel) Tk.Lime else Color.Transparent).clickable(onClick = onClick).padding(vertical = 10.dp),
        color = if (sel) Tk.OnLime else Tk.Muted, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center,
    )
}

@Composable
private fun Field(
    value: String, onChange: (String) -> Unit, hint: String, icon: ImageVector, type: KeyboardType = KeyboardType.Text,
    secret: Boolean = false, trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(16.dp)).background(Tk.Surface2).border(1.dp, if (value.isNotEmpty()) Tk.LimeBorder else Tk.Line, RoundedCornerShape(16.dp)).padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = Tk.Lime, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(hint, color = Tk.Muted, style = MaterialTheme.typography.bodyLarge)
            BasicTextField(
                value, onChange, Modifier.fillMaxWidth(), singleLine = true, textStyle = MaterialTheme.typography.bodyLarge.copy(color = Tk.White),
                cursorBrush = SolidColor(Tk.Lime), keyboardOptions = KeyboardOptions(keyboardType = type),
                visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
            )
        }
        trailing?.invoke()
    }
}

@Composable
fun Notice(icon: ImageVector, text: String, warn: Boolean = false) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (warn) MaterialTheme.colorScheme.error.copy(alpha = 0.12f) else Tk.LimeSoft).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = if (warn) MaterialTheme.colorScheme.error else Tk.Lime, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, color = Tk.White, style = MaterialTheme.typography.bodySmall)
    }
}
