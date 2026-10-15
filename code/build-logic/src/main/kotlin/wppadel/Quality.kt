package wppadel

import com.diffplug.gradle.spotless.SpotlessExtension
import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** ktlint (through Spotless) and detekt over every source set of the project; both run in `check`. */
internal fun Project.configureQuality(
    kotlinSources: List<String> = listOf("src/**/*.kt"),
    gradleScripts: List<String> = listOf("*.gradle.kts"),
    detektSources: List<String> = listOf("src"),
) {
    pluginManager.apply("com.diffplug.spotless")
    extensions.configure<SpotlessExtension> {
        val ktlint = libs.version("ktlint")
        // The style lives in the repository's .editorconfig, which IDEs read too
        val editorConfig = rootDir.resolve("../.editorconfig")
        kotlin {
            target(kotlinSources)
            ktlint(ktlint).setEditorConfigPath(editorConfig)
        }
        kotlinGradle {
            target(gradleScripts)
            ktlint(ktlint).setEditorConfigPath(editorConfig)
        }
    }

    pluginManager.apply("dev.detekt")
    extensions.configure<DetektExtension> {
        source.setFrom(detektSources)
        config.setFrom(rootDir.resolve("config/detekt/detekt.yml"))
        buildUponDefaultConfig.set(true)
        parallel.set(true)
    }
}
