plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.openfy.music"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.openfy.music"
        minSdk = 26
        targetSdk = 35
        versionCode = 9
        versionName = "1.3.5"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val keystorePath = project.findProperty("OPENFY_KEYSTORE_FILE") as? String
                ?: System.getenv("OPENFY_KEYSTORE_FILE")
                ?: "keystore/openfy-release.jks"
            val sPassword = project.findProperty("OPENFY_KEYSTORE_PASSWORD") as? String
                ?: System.getenv("OPENFY_KEYSTORE_PASSWORD")
            val kAlias = project.findProperty("OPENFY_KEY_ALIAS") as? String
                ?: System.getenv("OPENFY_KEY_ALIAS")
            val kPassword = project.findProperty("OPENFY_KEY_PASSWORD") as? String
                ?: System.getenv("OPENFY_KEY_PASSWORD")

            if (sPassword != null && kAlias != null && kPassword != null) {
                storeFile = file(keystorePath)
                storePassword = sPassword
                keyAlias = kAlias
                keyPassword = kPassword
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            val signingCfg = signingConfigs.getByName("release")
            if (signingCfg.storeFile != null && signingCfg.storeFile!!.exists()) {
                signingConfig = signingCfg
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            val signingCfg = signingConfigs.getByName("release")
            if (signingCfg.storeFile != null && signingCfg.storeFile!!.exists()) {
                signingConfig = signingCfg
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)

    // Media3 / ExoPlayer for RVX Audio Playback
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.common)

    // Networking, Image Loading & JSON
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)
    implementation(libs.coil.compose)
    implementation(libs.kotlinx.coroutines.android)

    // NewPipeExtractor for battle-tested YouTube stream resolution & cipher deobfuscation
    implementation("com.github.TeamNewPipe:NewPipeExtractor:v0.27.1")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    debugImplementation(libs.androidx.compose.ui.tooling)
}
