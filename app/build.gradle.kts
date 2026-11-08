plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.ppp65isking.ytmusicfortv"
    compileSdk = 36

    defaultConfig {
        // PLACEHOLDER — Anthony has not picked a name/package yet.
        applicationId = "com.ppp65isking.ytmusicfortv"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0-scaffold"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // AGP 9.1 bundles Kotlin 2.2.10 — no kotlin-android plugin; configure the
    // compiler through the kotlin extension instead of kotlinOptions.
    kotlin {
        compilerOptions {
            jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
        }
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // Compose BOM 2026.06.01 = the Jun-2026 release pairing with
    // tv-material 1.1.0 (May 2026) on compileSdk 36. (2026.08.00+ wants
    // compileSdk 37 — do not bump the BOM without bumping compileSdk.)
    val composeBom = platform("androidx.compose:compose-bom:2026.06.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.tv:tv-material:1.1.0")
    implementation("androidx.tv:tv-foundation:1.0.0")

    // Media3 — audio-only playback (ExoPlayer + HLS + MediaSession).
    implementation("androidx.media3:media3-exoplayer:1.11.1")
    implementation("androidx.media3:media3-session:1.11.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.11.1")

    // InnerTube calls: OkHttp directly + kotlinx-serialization-json tree
    // parsing (JsonElement, no @Serializable plugin needed). Retrofit is
    // available if the call surface grows, but is not used yet.
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

    // Album art / thumbnails.
    implementation("io.coil-kt:coil-compose:2.7.0")
}
