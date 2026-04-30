import java.io.File
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val keystoreProperties = Properties()
val keystorePropertiesFile = file("src/main/assets/keystore.properties")
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(keystorePropertiesFile.inputStream())
}

android {
    namespace   = "com.wppadel.tracker"
    compileSdk  = 36

    defaultConfig {
        applicationId = "com.wppadel.tracker"
        minSdk        = 26
        targetSdk     = 36
        versionCode   = 20260313
        versionName   = "1.2.1"
    }

    signingConfigs {
        create("release") {
            val parentDir = keystorePropertiesFile.parentFile
            storeFile     = file(File(parentDir, keystoreProperties.getProperty("prod.keystore")))
            storePassword = keystoreProperties.getProperty("prod.storePassword")
            keyAlias      = keystoreProperties.getProperty("prod.alias")
            keyPassword   = keystoreProperties.getProperty("prod.password")
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
    }
}

dependencies {
    implementation(project(":shared"))

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)

    // Wear Compose
    implementation(libs.wear.compose.foundation)
    implementation(libs.wear.compose.material)

    // AndroidX
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.play.services.wearable)
    implementation(libs.wear.core)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
}
