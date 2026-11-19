import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    jvm("desktop") {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    val xcf = XCFramework("OpenfyKit")
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "OpenfyKit"
            xcf.add(this)
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)

            // Ktor 3 Client & Content Negotiation
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)

            // Kotlinx Serialization & Coroutines
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)

            // Coil 3 Image Loading
            implementation(libs.coil3.compose)
            implementation(libs.coil3.network.ktor3)

        }

        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.kotlinx.coroutines.android)
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.ktor.client.okhttp)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.coroutines.swing)
                implementation(libs.okhttp)
                implementation(libs.gson)

                // OpenJFX for desktop media streaming (Windows, macOS Intel & Apple Silicon)
                implementation("org.openjfx:javafx-base:21.0.5")
                implementation("org.openjfx:javafx-graphics:21.0.5")
                implementation("org.openjfx:javafx-controls:21.0.5")
                implementation("org.openjfx:javafx-media:21.0.5")
                implementation("org.openjfx:javafx-base:21.0.5:win")
                implementation("org.openjfx:javafx-graphics:21.0.5:win")
                implementation("org.openjfx:javafx-controls:21.0.5:win")
                implementation("org.openjfx:javafx-media:21.0.5:win")
                implementation("org.openjfx:javafx-base:21.0.5:mac")
                implementation("org.openjfx:javafx-graphics:21.0.5:mac")
                implementation("org.openjfx:javafx-controls:21.0.5:mac")
                implementation("org.openjfx:javafx-media:21.0.5:mac")
                implementation("org.openjfx:javafx-base:21.0.5:mac-aarch64")
                implementation("org.openjfx:javafx-graphics:21.0.5:mac-aarch64")
                implementation("org.openjfx:javafx-controls:21.0.5:mac-aarch64")
                implementation("org.openjfx:javafx-media:21.0.5:mac-aarch64")
            }
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}

android {
    namespace = "com.openfy.music"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.openfy.music"
        minSdk = 26
        targetSdk = 35
        versionCode = 8
        versionName = "1.3.4"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

compose.desktop {
    application {
        mainClass = "com.openfy.music.MainKt"

        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Pkg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe
            )
            packageName = "Openfy"
            packageVersion = "1.3.4"
            description = "Openfy - Spotify Desktop Client"
            copyright = "© 2026 Openfy"
            vendor = "Openfy"

            windows {
                menuGroup = "Openfy"
                upgradeUuid = "6f4a3c2b-1a9e-4d8c-bf5e-8e7c6d5b4a3a"
                shortcut = true
                menu = true
            }

            macOS {
                bundleID = "com.openfy.music"
                dockName = "Openfy"
                iconFile.set(project.file("src/desktopMain/resources/icon.icns"))
            }
        }
    }
}
