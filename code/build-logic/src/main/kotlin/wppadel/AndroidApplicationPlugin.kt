package wppadel

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** The watch and phone apps: name, id and version from app.json, and one release key for both. */
class AndroidApplicationPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.application")
        configureAndroid()

        val app = appConfig()
        val key = releaseKey()
        extensions.configure<ApplicationExtension> {
            defaultConfig {
                applicationId = app.applicationId
                targetSdk = libs.intVersion("android-targetSdk")
                versionCode = app.versionCode
                versionName = app.versionName
                resValue("string", "app_name", app.name)
            }
            buildFeatures.resValues = true
            if (key is ReleaseKey.Found) {
                signingConfigs.create("release") {
                    storeFile = key.storeFile
                    storePassword = key.storePassword
                    keyAlias = key.alias
                    keyPassword = key.keyPassword
                }
            }
            buildTypes.getByName("release") {
                signingConfig = signingConfigs.findByName("release")
                isMinifyEnabled = true
                isShrinkResources = true
                proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), rootProject.file("android/proguard-rules.pro"))
            }
        }

        tasks.named("preBuild") { dependsOn(":$VERIFY_APP_CONFIG") }
        // Without a key a release would come out unsigned; Android Studio's signing wizard injects its own
        val injected = providers.gradleProperty("android.injected.signing.store.file").isPresent
        val missing = when (key) {
            is ReleaseKey.Found -> null
            is ReleaseKey.Incomplete -> key.reason
            ReleaseKey.Missing -> "No release key: add code/$KEYSTORE_PROPERTIES or set $RELEASE_KEY_ENV (see README, Release signing)"
        }.takeUnless { injected }
        if (missing != null) {
            tasks.matching { it.name == "packageRelease" || it.name == "signReleaseBundle" }.configureEach {
                doFirst { throw GradleException(missing) }
            }
        }
    }
}
