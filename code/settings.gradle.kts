pluginManagement {
    // Convention plugins: what the modules share (SDK levels, JDK, lint, ktlint, detekt, signing)
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "WPPadel"
include(":shared")
include(":android:common", ":android:watch", ":android:phone")
