package wppadel

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ReleaseKeyTest {
    private val propertiesFile = File("/repo/code/android/keystore.properties")
    private val noEnv = RELEASE_KEY_ENV.associateWith { null }
    private val fullEnv = mapOf(
        "WPPADEL_KEYSTORE_FILE" to "/ci/upload.keystore",
        "WPPADEL_KEYSTORE_PASSWORD" to "store-secret",
        "WPPADEL_KEY_ALIAS" to "upload",
        "WPPADEL_KEY_PASSWORD" to "key-secret",
    )
    private val fullProperties = """
        prod.keystore=keystore/upload.keystore
        prod.storePassword=store-secret
        prod.alias=upload
        prod.password=key-secret
    """.trimIndent()

    @Test
    fun `a complete properties file is a key with its path relative to the file`() {
        val key = assertIs<ReleaseKey.Found>(ReleaseKey.from(fullProperties, noEnv, propertiesFile))
        assertEquals(File("/repo/code/android/keystore/upload.keystore"), key.storeFile)
        assertEquals("store-secret", key.storePassword)
        assertEquals("upload", key.alias)
        assertEquals("key-secret", key.keyPassword)
    }

    @Test
    fun `an absolute keystore path stays absolute`() {
        val key = assertIs<ReleaseKey.Found>(ReleaseKey.from(fullProperties.replace("keystore/", "/keys/"), noEnv, propertiesFile))
        assertEquals(File("/keys/upload.keystore"), key.storeFile)
    }

    @Test
    fun `the properties file wins over the environment`() {
        val key = assertIs<ReleaseKey.Found>(ReleaseKey.from(fullProperties, fullEnv, propertiesFile))
        assertEquals("upload", key.alias)
        assertEquals(File("/repo/code/android/keystore/upload.keystore"), key.storeFile)
    }

    @Test
    fun `a complete environment is a key`() {
        val key = assertIs<ReleaseKey.Found>(ReleaseKey.from(null, fullEnv, propertiesFile))
        assertEquals(File("/ci/upload.keystore"), key.storeFile)
        assertEquals("key-secret", key.keyPassword)
    }

    @Test
    fun `neither file nor environment is missing`() {
        assertEquals(ReleaseKey.Missing, ReleaseKey.from(null, noEnv, propertiesFile))
    }

    @Test
    fun `empty passwords in the file are incomplete and name the file`() {
        val blank = fullProperties.replace("store-secret", "").replace("key-secret", "")
        val key = assertIs<ReleaseKey.Incomplete>(ReleaseKey.from(blank, noEnv, propertiesFile))
        assertEquals("Release key incomplete: prod.storePassword, prod.password empty in code/android/keystore.properties", key.reason)
    }

    @Test
    fun `a property left out of the file is incomplete, not an error`() {
        val key = assertIs<ReleaseKey.Incomplete>(ReleaseKey.from(fullProperties.replace("prod.alias=upload", ""), noEnv, propertiesFile))
        assertEquals("Release key incomplete: prod.alias not set in code/android/keystore.properties", key.reason)
    }

    @Test
    fun `a partly set environment is incomplete and names the variables`() {
        val key = assertIs<ReleaseKey.Incomplete>(ReleaseKey.from(null, noEnv + ("WPPADEL_KEY_ALIAS" to "upload"), propertiesFile))
        assertEquals(
            "Release key incomplete: WPPADEL_KEYSTORE_FILE, WPPADEL_KEYSTORE_PASSWORD, WPPADEL_KEY_PASSWORD not set in the environment " +
                "(the release key needs all of $RELEASE_KEY_ENV)",
            key.reason,
        )
    }

    @Test
    fun `empty environment passwords are incomplete and name the variables, not the file`() {
        val key = assertIs<ReleaseKey.Incomplete>(ReleaseKey.from(null, fullEnv + ("WPPADEL_KEY_PASSWORD" to ""), propertiesFile))
        assertEquals(
            "Release key incomplete: WPPADEL_KEY_PASSWORD empty in the environment (the release key needs all of $RELEASE_KEY_ENV)",
            key.reason,
        )
    }
}
