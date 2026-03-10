import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
}

kotlin {
    // ── Android (Wear OS) ─────────────────────────────────────────────────
    androidTarget {
        compilations.all {
            kotlinOptions { jvmTarget = "11" }
        }
    }

    // ── JVM (unit tests + desktop tooling) ───────────────────────────────
    jvm()

    // ── Apple Watch (XCFramework for Swift/Xcode) ─────────────────────────
    // watchosArm64()       → arm64_32  (Series 4 – Series 10, Ultra 1/2)
    // watchosDeviceArm64() → arm64     (future full-64-bit devices; required
    //                                   for Xcode "Any watchOS Device" builds)
    // watchosSimulatorArm64() → arm64  (Apple Silicon simulator)
    // watchosX64()         → x86_64    (Intel simulator)
    val xcf = XCFramework("Shared")
    listOf(
        watchosArm64(),
        watchosDeviceArm64(),
        watchosSimulatorArm64(),
        watchosX64()
    ).forEach {
        it.binaries.framework {
            baseName = "Shared"
            xcf.add(this)
        }
    }

    sourceSets {
        commonMain.dependencies {
            // No external dependencies – pure Kotlin
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace  = "com.wptracker.shared"
    compileSdk = 35
    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
