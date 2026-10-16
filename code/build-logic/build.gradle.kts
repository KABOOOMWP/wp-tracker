plugins {
    `kotlin-dsl`
}

// The plugins are applied (with their versions) by the root build, so compile against them only
dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.spotless.gradlePlugin)
    compileOnly(libs.detekt.gradlePlugin)
    compileOnly(libs.kover.gradlePlugin)

    testImplementation(libs.kotlin.test.junit)
}

gradlePlugin {
    plugins {
        register("root") {
            id = libs.plugins.wppadel.root.get().pluginId
            implementationClass = "wppadel.RootPlugin"
        }
        register("androidApplication") {
            id = libs.plugins.wppadel.android.application.get().pluginId
            implementationClass = "wppadel.AndroidApplicationPlugin"
        }
        register("androidLibrary") {
            id = libs.plugins.wppadel.android.library.get().pluginId
            implementationClass = "wppadel.AndroidLibraryPlugin"
        }
        register("kotlinMultiplatform") {
            id = libs.plugins.wppadel.kotlin.multiplatform.get().pluginId
            implementationClass = "wppadel.KotlinMultiplatformPlugin"
        }
    }
}
