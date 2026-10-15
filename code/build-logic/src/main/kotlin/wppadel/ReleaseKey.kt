package wppadel

import org.gradle.api.GradleException
import org.gradle.api.Project
import java.io.File
import java.util.Properties

/** The upload key both apps sign with; the Wear Data Layer only links apps signed with the same key. */
internal class ReleaseKey(val storeFile: File, val storePassword: String, val alias: String, val keyPassword: String) {
    /** False while keystore.properties leaves a password empty, to be filled in only for a release. */
    val isComplete: Boolean get() = storePassword.isNotBlank() && keyPassword.isNotBlank()
}

internal const val KEYSTORE_PROPERTIES = "android/keystore.properties"

/** Where each part of the key comes from: a keystore.properties key and the environment variable CI sets instead. */
private enum class KeyPart(val property: String, val env: String) {
    STORE("prod.keystore", "WPPADEL_KEYSTORE_FILE"),
    STORE_PASSWORD("prod.storePassword", "WPPADEL_KEYSTORE_PASSWORD"),
    ALIAS("prod.alias", "WPPADEL_KEY_ALIAS"),
    KEY_PASSWORD("prod.password", "WPPADEL_KEY_PASSWORD"),
}

internal val RELEASE_KEY_ENV: List<String> = KeyPart.entries.map { it.env }

/** Reads code/android/keystore.properties (git-ignored), else the environment; null when neither has a key. */
internal fun Project.releaseKey(): ReleaseKey? {
    val file = rootProject.layout.projectDirectory.file(KEYSTORE_PROPERTIES)
    val text = providers.fileContents(file).asText.orNull
    val value: (KeyPart) -> String
    if (text != null) {
        val properties = Properties().apply { load(text.reader()) }
        value = { properties.getProperty(it.property) ?: throw GradleException("$KEYSTORE_PROPERTIES has no ${it.property}") }
    } else {
        val env = KeyPart.entries.associateWith { providers.environmentVariable(it.env).orNull }
        if (env.values.all { it == null }) return null
        value = { env[it] ?: throw GradleException("${it.env} is not set (the release key needs all of $RELEASE_KEY_ENV)") }
    }
    return ReleaseKey(
        // A relative keystore path is relative to keystore.properties
        storeFile = file.asFile.parentFile.resolve(value(KeyPart.STORE)),
        storePassword = value(KeyPart.STORE_PASSWORD),
        alias = value(KeyPart.ALIAS),
        keyPassword = value(KeyPart.KEY_PASSWORD),
    )
}
