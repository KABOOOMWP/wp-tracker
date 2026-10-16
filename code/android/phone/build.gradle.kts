import kotlinx.kover.gradle.plugin.dsl.CoverageUnit

plugins {
    alias(libs.plugins.wppadel.android.application)
}

android {
    namespace = "ch.wastedpotential.padel.phone"
}

dependencies {
    implementation(project(":android:common"))

    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.androidx.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(testFixtures(project(":android:common")))
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(testFixtures(project(":android:common")))
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.espresso.core)
}

// Coverage floor for koverVerify, just below the measured value; raise it when coverage grows
kover.reports.verify.rule {
    minBound(90, CoverageUnit.LINE)
    minBound(90, CoverageUnit.BRANCH)
}
