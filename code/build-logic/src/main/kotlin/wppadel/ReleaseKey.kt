package wppadel

import org.gradle.api.Project
import java.io.File
import java.util.Properties

internal const val KEYSTORE_PROPERTIES = "android/keystore.properties"

/** Where each part of the key comes from: a keystore.properties key and the environment variable CI sets instead. */
private enum class KeyPart(val property: String, val env: String) {
    STORE("prod.keystore", "WPPADEL_KEYSTORE_FILE"),
    STORE_PASSWORD("prod.storePassword", "WPPADEL_KEYSTORE_PASSWORD"),
    ALIAS("prod.alias", "WPPADEL_KEY_ALIAS"),
    KEY_PASSWORD("prod.password", "WPPADEL_KEY_PASSWORD"),
}

internal val RELEASE_KEY_ENV: List<String> = KeyPart.entries.map { it.env }

/** The upload key both apps sign with (the Wear Data Layer only links apps signed with one key), or why there is none yet. */
internal sealed interface ReleaseKey {
    /** Every part is set: release builds sign with it. */
    class Found(val storeFile: File, val storePassword: String, val alias: String, val keyPassword: String) : ReleaseKey

    /** A key is configured but a part is unset or empty; only a release build fails, with [reason]. */
    class Incomplete(val reason: String) : ReleaseKey

    /** Neither keystore.properties nor the environment names a key. */
    data object Missing : ReleaseKey

    companion object {
        /** From keystore.properties ([propertiesText], null when absent), else [env]; a relative path is relative to [propertiesFile]. */
        fun from(propertiesText: String?, env: Map<String, String?>, propertiesFile: File): ReleaseKey {
            val fromFile = propertiesText != null
            val parts = if (fromFile) {
                val properties = Properties().apply { load(propertiesText.reader()) }
                KeyPart.entries.associateWith { properties.getProperty(it.property) }
            } else {
                KeyPart.entries.associateWith { env[it.env] }
            }
            val name: (KeyPart) -> String = if (fromFile) KeyPart::property else KeyPart::env
            val where =
                if (fromFile) "in code/$KEYSTORE_PROPERTIES" else "in the environment (the release key needs all of $RELEASE_KEY_ENV)"
            val unset = parts.filterValues { it == null }.keys
            val empty = parts.filterValues { it != null && it.isBlank() }.keys
            return when {
                !fromFile && unset.size == parts.size -> Missing
                unset.isNotEmpty() -> Incomplete("Release key incomplete: ${unset.joinToString(transform = name)} not set $where")
                empty.isNotEmpty() -> Incomplete("Release key incomplete: ${empty.joinToString(transform = name)} empty $where")
                else -> found(parts.mapValues { it.value!! }, propertiesFile)
            }
        }

        private fun found(parts: Map<KeyPart, String>, propertiesFile: File) = Found(
            storeFile = propertiesFile.parentFile.resolve(parts.getValue(KeyPart.STORE)),
            storePassword = parts.getValue(KeyPart.STORE_PASSWORD),
            alias = parts.getValue(KeyPart.ALIAS),
            keyPassword = parts.getValue(KeyPart.KEY_PASSWORD),
        )
    }
}

/** Reads code/android/keystore.properties (git-ignored), else the environment. */
internal fun Project.releaseKey(): ReleaseKey {
    val file = rootProject.layout.projectDirectory.file(KEYSTORE_PROPERTIES)
    return ReleaseKey.from(
        propertiesText = providers.fileContents(file).asText.orNull,
        env = RELEASE_KEY_ENV.associateWith { providers.environmentVariable(it).orNull },
        propertiesFile = file.asFile,
    )
}
