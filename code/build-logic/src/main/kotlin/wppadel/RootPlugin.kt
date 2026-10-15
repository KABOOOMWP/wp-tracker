package wppadel

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register

/** The root project: the app.json check, and ktlint and detekt over the build scripts and build-logic. */
class RootPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        val repo = layout.projectDirectory.dir("..")
        tasks.register<VerifyAppConfig>(VERIFY_APP_CONFIG) {
            script.set(repo.file("tool/app_config.py"))
            checkedFiles.from(
                repo.file("app.json"),
                layout.projectDirectory.file("apple/Config/App.xcconfig"),
                layout.projectDirectory.file("apple/WPPadel.xcodeproj/project.pbxproj"),
            )
            report.set(layout.buildDirectory.file("$VERIFY_APP_CONFIG/report.txt"))
        }
        configureQuality(
            kotlinSources = listOf("build-logic/src/**/*.kt"),
            gradleScripts = listOf("*.gradle.kts", "build-logic/*.gradle.kts"),
            detektSources = listOf("build-logic/src"),
        )
    }
}
