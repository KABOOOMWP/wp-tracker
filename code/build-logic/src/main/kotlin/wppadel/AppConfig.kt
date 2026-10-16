package wppadel

import groovy.json.JsonSlurper
import org.gradle.api.Project

/** One Android app's entry in app.json (android.<module name>) plus the shared version. */
internal class AppConfig(val applicationId: String, val name: String, val versionName: String, val versionCode: Int) {
    companion object {
        /** The [module]'s app from the text of app.json; a missing key or a wrong type throws. */
        fun parse(json: String, module: String): AppConfig {
            @Suppress("UNCHECKED_CAST")
            val root = JsonSlurper().parseText(json) as Map<String, Map<String, Any>>
            val version = root.getValue("version")

            @Suppress("UNCHECKED_CAST")
            val app = root.getValue("android").getValue(module) as Map<String, Any>
            return AppConfig(
                applicationId = app.getValue("applicationId") as String,
                name = app.getValue("name") as String,
                versionName = version.getValue("name") as String,
                // One Play app for both, so each bundle needs its own code (checked by tool/app_config.py)
                versionCode = version.getValue("build") as Int * 10 + app.getValue("versionCodeOffset") as Int,
            )
        }
    }
}

internal fun Project.appConfig(): AppConfig =
    AppConfig.parse(providers.fileContents(rootProject.layout.projectDirectory.file("../app.json")).asText.get(), name)
