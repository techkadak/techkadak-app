plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Firebase: app/google-services.json ho tabhi Firebase ON hota hai. Na ho to app bina Firebase ke bhi poora chalta hai.
val firebaseJson = file("google-services.json")
val firebaseOn = firebaseJson.exists()
if (firebaseOn) apply(plugin = "com.google.gms.google-services")

// v1.5: Rs 50 UPI support button. ID state/secrets se build-time me aata hai (kabhi repo me nahi). Missing/invalid = button chhupa rahega.
val upiId: String = run {
    val raw = (project.findProperty("TK_UPI_ID") as String?)?.takeIf { it.isNotBlank() }
        ?: rootProject.file("../../state/secrets/rohit-upi-id.txt").takeIf { it.exists() }?.readText()?.lineSequence()?.firstOrNull()
        ?: ""
    raw.trim().filter { it.isLetterOrDigit() || it == '@' || it == '.' || it == '-' || it == '_' }
}

fun prop(name: String, def: String) = (project.findProperty(name) as String?)?.takeIf { it.isNotBlank() } ?: def

// Release signing: env vars (GitHub Actions) — keystore kabhi repo me commit mat karna.
val ksPath: String? = System.getenv("TK_KEYSTORE_PATH")
val hasSigning = ksPath != null && file(ksPath).exists()

android {
    namespace = "com.techkadak.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "in.techkadak.app"
        minSdk = 24
        targetSdk = 34
        versionCode = prop("VERSION_CODE", "10800").toInt()
        versionName = prop("VERSION_NAME", "1.8.0")
        buildConfigField("String", "SITE_BASE", "\"${prop("TK_SITE_BASE", "https://techkadak.github.io").trimEnd('/')}\"")
        buildConfigField("String", "GITHUB_OWNER", "\"${prop("TK_GITHUB_OWNER", "techkadak")}\"")
        buildConfigField("String", "GITHUB_REPO", "\"${prop("TK_GITHUB_REPO", "techkadak-app")}\"")
        buildConfigField("boolean", "FIREBASE_ENABLED", firebaseOn.toString())
        buildConfigField("String", "UPI_ID", "\"${upiId}\"")
        resourceConfigurations += listOf("en")
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        if (hasSigning) {
            create("release") {
                storeFile = file(ksPath!!)
                storePassword = System.getenv("TK_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("TK_KEY_ALIAS")
                keyPassword = System.getenv("TK_KEY_PASSWORD")
                enableV1Signing = true
                enableV2Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasSigning) signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    packaging { resources { excludes += listOf("/META-INF/{AL2.0,LGPL2.1}", "DebugProbesKt.bin", "kotlin/**", "META-INF/*.version") } }
    // MainActivity ComponentActivity hai (FragmentActivity nahi) — purane fragment wala lint warning yahan laagu nahi.
    lint { disable += "InvalidFragmentVersionForActivityResult" }
    dependenciesInfo { includeInApk = false; includeInBundle = false }
    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            it.systemProperty("roborazzi.test.record", "true")
            it.systemProperty("tk.siteDir", rootProject.file("../techkadak-site-free").absolutePath)
            it.systemProperty("tk.iconsDir", file("build/test-icons").absolutePath)
            it.systemProperty("tk.shotsDir", rootProject.file("screens-raw").absolutePath)
            it.maxHeapSize = "2g"
            it.testLogging { events("passed", "failed", "skipped"); showStandardStreams = true }
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation(platform("com.google.firebase:firebase-bom:33.4.0"))
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-config")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    // v1.1: Settings (DataStore), widget (Glance), splash, login (Firebase Auth + Credential Manager)
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.glance:glance-appwidget:1.1.0")
    implementation("androidx.glance:glance-material3:1.1.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("com.google.firebase:firebase-auth")
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")
    // v1.2: owner dashboard stats (Firestore, Spark free) + Google Analytics (anonymous) + app foreground/background hook
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-analytics")
    implementation("androidx.lifecycle:lifecycle-process:2.8.6")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    testImplementation("org.robolectric:robolectric:4.13")
    testImplementation("androidx.test:core-ktx:1.6.1")
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.29.0")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.29.0")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
