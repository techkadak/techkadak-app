# org.json + platform classes only; nothing reflective in app code.
-dontwarn org.jetbrains.annotations.**

# Credential Manager (Google login) — play-services provider reflection se load hota hai
-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }
-dontwarn com.google.android.gms.**
