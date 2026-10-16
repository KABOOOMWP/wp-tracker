import kotlinx.kover.gradle.plugin.dsl.CoverageUnit
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    alias(libs.plugins.wppadel.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    explicitApi()

    // ── Android (Wear OS and phone) ───────────────────────────────────────
    android {
        namespace = "ch.wastedpotential.padel.shared"
        // Runs commonTest on the Android host too, as the watch and phone run it
        withHostTest {}
    }

    // ── JVM (unit tests) ──────────────────────────────────────────────────
    jvm()

    // ── Apple: watch (arm64_32, arm64, simulator) and iPhone (device, simulator) ──
    // Xcode builds this through embedAndSignAppleFrameworkForXcode; the XCFramework is optional
    val xcf = XCFramework("Shared")
    listOf(
        watchosArm64(),
        watchosDeviceArm64(),
        watchosSimulatorArm64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach {
        it.binaries.framework {
            baseName = "Shared"
            xcf.add(this)
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

// Coverage floor for koverVerify, just below the measured value; raise it when coverage grows
kover.reports.verify.rule {
    minBound(99, CoverageUnit.LINE)
    minBound(90, CoverageUnit.BRANCH)
}
