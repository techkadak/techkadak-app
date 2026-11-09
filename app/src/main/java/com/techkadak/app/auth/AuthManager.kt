package com.techkadak.app.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.FirebaseException
import java.util.concurrent.TimeUnit
import com.google.firebase.auth.UserProfileChangeRequest
import com.techkadak.app.firebase.TkFirebase
import kotlinx.coroutines.tasks.await

/** App me dikhane layak user (Firebase user ka chhota roop). */
data class TkUser(
    val uid: String,
    val name: String?,
    val email: String?,
    val photoUrl: String?,
    val emailVerified: Boolean,
    val provider: String, // "google" / "password" / "phone"
    val phone: String? = null,
) {
    /** Google wale account pehle se verified hote hain; email/password wale ko mail link se verify karna. */
    val needsVerification: Boolean get() = provider == "password" && !emailVerified
    val displayName: String get() = name?.takeIf { it.isNotBlank() } ?: email?.substringBefore('@') ?: phone ?: "Tech Kadak user"
}

/**
 * Login OPTIONAL hai — bina login ke bhi poora app chalta hai (guest mode).
 * Firebase Authentication (Spark free): Google + Email/Password (email verification link).
 */
class AuthManager(private val context: Context) {
    private val tag = "TkAuth"
    private val auth: FirebaseAuth? get() = if (TkFirebase.available(context)) runCatching { FirebaseAuth.getInstance() }.getOrNull() else null

    val enabled: Boolean get() = auth != null

    /** v1.5: true when the last Google/phone sign-in created a brand-new account (Firebase isNewUser). Read once, then cleared. */
    @Volatile var lastSignInNew: Boolean = false
    fun takeNew(): Boolean = lastSignInNew.also { lastSignInNew = false }

    fun current(): TkUser? = auth?.currentUser?.let { u ->
        TkUser(
            uid = u.uid, name = u.displayName, email = u.email, photoUrl = u.photoUrl?.toString(),
            emailVerified = u.isEmailVerified,
            provider = when {
                u.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID } -> "google"
                u.providerData.any { it.providerId == PhoneAuthProvider.PROVIDER_ID } -> "phone"
                else -> "password"
            },
            phone = u.phoneNumber,
        )
    }

    fun addListener(onChange: () -> Unit) { auth?.addAuthStateListener { onChange() } }

    /** Web client id google-services.json se (gms plugin R.string.default_web_client_id banata hai). */
    fun webClientId(): String? {
        val id = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        return if (id == 0) null else context.getString(id).takeIf { it.isNotBlank() }
    }

    suspend fun signInWithGoogle(activity: Activity): Result<TkUser> = runCatching {
        val a = auth ?: error(tr("Login abhi band hai (Firebase set nahi)", "Login is unavailable (Firebase not set up)"))
        val clientId = webClientId() ?: error(tr("Google login abhi set nahi hua — Email se karo", "Google sign-in isn't set up yet — use email"))
        val cm = CredentialManager.create(activity)
        val response = try {
            // Pehle: phone pe jo Google account pehle use hua, uska bottom-sheet.
            cm.getCredential(activity, GetCredentialRequest.Builder().addCredentialOption(
                GetGoogleIdOption.Builder().setFilterByAuthorizedAccounts(false).setServerClientId(clientId).setAutoSelectEnabled(false).build(),
            ).build())
        } catch (e: NoCredentialException) {
            // Phone pe koi account nahi mila to poora "Sign in with Google" dialog.
            cm.getCredential(activity, GetCredentialRequest.Builder().addCredentialOption(GetSignInWithGoogleOption.Builder(clientId).build()).build())
        }
        val cred = response.credential
        if (cred !is CustomCredential || cred.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) error("Google se sahi jawab nahi aaya")
        val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
        val res = a.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
        lastSignInNew = res.additionalUserInfo?.isNewUser == true
        current() ?: error("Login nahi hua")
    }.onFailure { Log.w(tag, "google", it) }.mapError()

    /** Naya account: email + password → verification mail. */
    suspend fun signUp(name: String, email: String, password: String): Result<TkUser> = runCatching {
        val a = auth ?: error(tr("Login abhi band hai", "Login is unavailable"))
        val r = a.createUserWithEmailAndPassword(email.trim(), password).await()
        val u = r.user ?: error("Account nahi bana")
        if (name.isNotBlank()) u.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name.trim()).build()).await()
        u.sendEmailVerification().await()
        current()!!
    }.mapError()

    suspend fun signIn(email: String, password: String): Result<TkUser> = runCatching {
        val a = auth ?: error(tr("Login abhi band hai", "Login is unavailable"))
        a.signInWithEmailAndPassword(email.trim(), password).await()
        current()!!
    }.mapError()

    suspend fun resendVerification(): Result<Unit> = runCatching {
        (auth?.currentUser ?: error("Pehle login karo")).sendEmailVerification().await(); Unit
    }.mapError()

    /** "Maine verify kar liya": server se taaza status. */
    suspend fun reload(): Result<TkUser?> = runCatching { auth?.currentUser?.reload()?.await(); current() }.mapError()

    suspend fun resetPassword(email: String): Result<Unit> = runCatching { (auth ?: error("Login band")).sendPasswordResetEmail(email.trim()).await(); Unit }.mapError()

    // ── Phone OTP (Firebase SMS) ──
    sealed interface PhoneEvent {
        data class CodeSent(val verificationId: String, val token: PhoneAuthProvider.ForceResendingToken) : PhoneEvent
        /** Phone ne SMS khud padh liya (auto-retrieval) — code boxes me bhar do. */
        data class AutoCode(val code: String?, val credential: PhoneAuthCredential) : PhoneEvent
        data class Failed(val message: String) : PhoneEvent
    }

    fun startPhone(activity: Activity, phone: String, resend: PhoneAuthProvider.ForceResendingToken?, onEvent: (PhoneEvent) -> Unit) {
        val a = auth ?: run { onEvent(PhoneEvent.Failed(tr("Login abhi band hai", "Login is unavailable"))); return }
        a.setLanguageCode("hi")
        val b = PhoneAuthOptions.newBuilder(a).setPhoneNumber(phone).setTimeout(60L, TimeUnit.SECONDS).setActivity(activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) { onEvent(PhoneEvent.AutoCode(credential.smsCode, credential)) }
                override fun onVerificationFailed(e: FirebaseException) { Log.w(tag, "phone", e); onEvent(PhoneEvent.Failed(friendly(e))) }
                override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) { onEvent(PhoneEvent.CodeSent(id, token)) }
            })
        if (resend != null) b.setForceResendingToken(resend)
        runCatching { PhoneAuthProvider.verifyPhoneNumber(b.build()) }.onFailure { onEvent(PhoneEvent.Failed(friendly(it))) }
    }

    suspend fun signInPhone(verificationId: String, code: String): Result<TkUser> = runCatching {
        signInPhone(PhoneAuthProvider.getCredential(verificationId, code)).getOrThrow()
    }.mapError()

    suspend fun signInPhone(credential: PhoneAuthCredential): Result<TkUser> = runCatching {
        val res = (auth ?: error("Login band")).signInWithCredential(credential).await()
        lastSignInNew = res.additionalUserInfo?.isNewUser == true
        current()!!
    }.mapError()

    suspend fun signOut() {
        auth?.signOut()
        runCatching { CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest()) }
    }

    suspend fun deleteAccount(): Result<Unit> = runCatching {
        (auth?.currentUser ?: error("Login nahi hai")).delete().await()
        runCatching { CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest()) }
        Unit
    }.mapError()

    private fun <T> Result<T>.mapError(): Result<T> = recoverCatching { throw IllegalStateException(friendly(it), it) }

    companion object {
        private fun tr(hi: String, en: String) = com.techkadak.app.ui.tr(hi, en)

        /** Firebase ke errors ko seedhi Hinglish me. */
        fun friendly(e: Throwable): String = when (e) {
            is GetCredentialCancellationException -> tr("Login cancel kar diya", "Sign-in cancelled")
            is NoCredentialException -> tr("Phone me koi Google account nahi mila", "No Google account found on this phone")
            is FirebaseAuthWeakPasswordException -> tr("Password kamzor hai — kam se kam 6 akshar", "Password too weak — at least 6 characters")
            is FirebaseAuthUserCollisionException -> tr("Is email se account pehle se hai — Login karo", "An account already exists with this email — log in")
            is FirebaseAuthInvalidUserException -> tr("Ye account nahi mila", "Account not found")
            is FirebaseAuthInvalidCredentialsException -> when (e.errorCode) {
                "ERROR_INVALID_VERIFICATION_CODE" -> tr("OTP galat hai — dobara dekho", "Wrong OTP — check again")
                "ERROR_INVALID_PHONE_NUMBER" -> tr("Phone number sahi nahi hai", "Invalid phone number")
                "ERROR_SESSION_EXPIRED" -> tr("OTP purana ho gaya — dobara bhejo", "OTP expired — resend")
                else -> tr("Email ya password galat hai", "Wrong email or password")
            }
            is FirebaseAuthRecentLoginRequiredException -> tr("Security ke liye: logout karke dobara login karo, phir delete karo", "For security, log out, log in again, then delete")
            is FirebaseAuthException -> when (e.errorCode) {
                "ERROR_TOO_MANY_REQUESTS" -> tr("Bahut try ho gaye — thodi der baad", "Too many attempts — try later")
                "ERROR_QUOTA_EXCEEDED" -> tr("Aaj ke SMS khatam — Google ya Email se login karo", "Today's SMS limit is used up — use Google or email")
                "ERROR_NETWORK_REQUEST_FAILED" -> tr("Net check karo", "Check your connection")
                else -> e.localizedMessage ?: tr("Kuch gadbad hui", "Something went wrong")
            }
            is com.google.firebase.FirebaseTooManyRequestsException -> tr("Aaj ke OTP SMS khatam ho gaye — abhi Google ya Email se login karo", "Today's OTP SMS limit is used up — use Google or email for now")
            is com.google.firebase.FirebaseNetworkException -> tr("Net check karo", "Check your connection")
            else -> e.message ?: tr("Kuch gadbad hui", "Something went wrong")
        }

        /** "98xxxxxx12" / "+91 98…" -> "+9198…"; galat = null. */
        fun normalizeIndianPhone(raw: String): String? {
            val d = raw.filter { it.isDigit() || it == '+' }
            return when {
                d.startsWith("+") && d.length in 11..16 -> d
                d.length == 10 && d.first() in "6789" -> "+91$d"
                d.length == 12 && d.startsWith("91") && d[2] in "6789" -> "+$d"
                d.length == 11 && d.startsWith("0") && d[1] in "6789" -> "+91" + d.drop(1)
                else -> null
            }
        }

        fun validEmail(e: String) = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}$").matches(e.trim())
    }
}
