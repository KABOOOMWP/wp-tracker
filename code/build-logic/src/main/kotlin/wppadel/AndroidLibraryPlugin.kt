package wppadel

import org.gradle.api.Plugin
import org.gradle.api.Project

/** An Android library with Compose (android/common). */
class AndroidLibraryPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.library")
        configureAndroid()
    }
}
