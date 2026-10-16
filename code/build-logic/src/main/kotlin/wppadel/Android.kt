package wppadel

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** What the watch app, the phone app and android/common share. */
internal fun Project.configureAndroid() {
    pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
    extensions.configure<CommonExtension> {
        compileSdk = libs.intVersion("android-compileSdk")
        defaultConfig.apply {
            minSdk = libs.intVersion("android-minSdk")
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
        val java = JavaVersion.toVersion(libs.version("jvm"))
        compileOptions.sourceCompatibility = java
        compileOptions.targetCompatibility = java
        buildFeatures.compose = true
        // android.util.Log is a stub on the JVM; the unit tests of FileTextStore reach it on a failed write
        testOptions.unitTests.isReturnDefaultValues = true
        lint.apply {
            warningsAsErrors = true
            abortOnError = true
        }
    }
    configureKotlin()
    configureQuality()
    configureCoverage()
}
