import kotlinx.kover.gradle.plugin.dsl.CoverageUnit

plugins {
    alias(libs.plugins.wppadel.android.library)
}

// Android code the watch and phone apps share: the board UI, file storage and the Data Layer link
android {
    namespace = "ch.wastedpotential.padel.common"
    // MemoryChannel, the in-memory link every Android test uses
    testFixtures.enable = true
}

dependencies {
    api(project(":shared"))
    api(platform(libs.compose.bom))
    api(libs.compose.foundation)
    api(libs.compose.ui)
    implementation(libs.androidx.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.play.services.wearable)

    testFixturesApi(project(":shared"))

    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.kotlin.test.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.core)
}

// Coverage floor for koverVerify, just below the measured value; raise it when coverage grows
kover.reports.verify.rule {
    minBound(95, CoverageUnit.LINE)
    minBound(99, CoverageUnit.BRANCH)
}
