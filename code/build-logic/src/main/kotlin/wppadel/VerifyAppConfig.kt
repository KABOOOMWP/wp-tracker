package wppadel

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import javax.inject.Inject

internal const val VERIFY_APP_CONFIG = "verifyAppConfig"

/** Fails when an app's name, id or version drifted from app.json; runs `tool/app_config.py check`. */
@CacheableTask
abstract class VerifyAppConfig @Inject constructor(private val exec: ExecOperations) : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val script: RegularFileProperty

    /** Every file the check reads: app.json, the generated App.xcconfig and the Xcode project. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val checkedFiles: ConfigurableFileCollection

    @get:OutputFile
    abstract val report: RegularFileProperty

    @TaskAction
    fun verify() {
        val output = ByteArrayOutputStream()
        val result = exec.exec {
            commandLine("python3", script.get().asFile.path, "check")
            standardOutput = output
            errorOutput = output
            isIgnoreExitValue = true
        }
        if (result.exitValue != 0) throw GradleException("app.json check failed:\n$output")
        report.get().asFile.writeText(output.toString())
    }
}
