package wppadel

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AppConfigTest {
    private val json = """
        {
          "version": { "name": "1.2.3", "build": 7 },
          "apple": { "team": "ABCDE12345" },
          "android": {
            "phone": { "name": "WP Padel Board", "applicationId": "ch.example.padel", "versionCodeOffset": 1 },
            "watch": { "name": "WP Padel Tracker", "applicationId": "ch.example.padel", "versionCodeOffset": 2 }
          }
        }
    """.trimIndent()

    @Test
    fun `reads the module's app and the shared version`() {
        val phone = AppConfig.parse(json, "phone")
        assertEquals("ch.example.padel", phone.applicationId)
        assertEquals("WP Padel Board", phone.name)
        assertEquals("1.2.3", phone.versionName)
    }

    @Test
    fun `versionCode is build times ten plus the module's offset`() {
        assertEquals(71, AppConfig.parse(json, "phone").versionCode)
        assertEquals(72, AppConfig.parse(json, "watch").versionCode)
    }

    @Test
    fun `a module without an entry fails naming it`() {
        val error = assertFailsWith<NoSuchElementException> { AppConfig.parse(json, "tablet") }
        assertEquals("Key tablet is missing in the map.", error.message)
    }

    @Test
    fun `a missing key fails naming it`() {
        val error = assertFailsWith<NoSuchElementException> { AppConfig.parse(json.replace("\"build\": 7", "\"number\": 7"), "phone") }
        assertEquals("Key build is missing in the map.", error.message)
    }

    @Test
    fun `a build that is not an integer fails`() {
        assertFailsWith<ClassCastException> { AppConfig.parse(json.replace("\"build\": 7", "\"build\": \"7\""), "phone") }
    }

    @Test
    fun `the repository's app json parses for both modules`() {
        val repo = generateSequence(File("").absoluteFile) { it.parentFile }.first { it.resolve("app.json").isFile }
        val text = repo.resolve("app.json").readText()
        val phone = AppConfig.parse(text, "phone")
        val watch = AppConfig.parse(text, "watch")
        assertEquals(phone.applicationId, watch.applicationId)
        assertEquals(phone.versionName, watch.versionName)
        assertEquals(phone.versionCode / 10, watch.versionCode / 10)
    }
}
