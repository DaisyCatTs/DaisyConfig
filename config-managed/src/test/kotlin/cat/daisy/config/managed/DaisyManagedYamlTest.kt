package cat.daisy.config.managed

import cat.daisy.config.DaisyReloadResult
import cat.daisy.config.defaulted
import cat.daisy.config.intCodec
import cat.daisy.config.objectCodec
import cat.daisy.config.stringCodec
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DaisyManagedYamlTest {
    private val codec =
        objectCodec {
            ExampleConfig(
                title = required("title", stringCodec()),
                rows = defaulted("rows", intCodec(), 3),
            )
        }

    @Test
    fun `managed load creates file from bundled default`() {
        val directory = createTempDirectory("daisy-managed-create").toFile()

        val result =
            DaisyManagedYaml.load(
                rootDirectory = directory,
                resourceLoader = { resource ->
                    if (resource == "settings.yml") "title: Ready\nrows: 5\nconfig_version: 1\n" else null
                },
                file = managedFile(),
            )

        assertIs<DaisyManagedReloadResult.Success<ExampleConfig>>(result)
        assertTrue(result.report.createdFromDefault)
        assertTrue(result.report.wroteToDisk)
        assertEquals("Ready", result.value.title)
        assertTrue(File(directory, "settings.yml").exists())
    }

    @Test
    fun `managed load merges missing keys and preserves user overrides`() {
        val directory = createTempDirectory("daisy-managed-merge").toFile()
        val file = File(directory, "settings.yml")
        file.writeText(
            """
            title: Custom
            unknown: keep-me
            config_version: 1
            """.trimIndent(),
        )

        val result =
            DaisyManagedYaml.load(
                rootDirectory = directory,
                resourceLoader = {
                    """
                    title: Default
                    rows: 6
                    nested:
                      enabled: true
                    config_version: 1
                    """.trimIndent()
                },
                file = managedFile(),
            )

        assertIs<DaisyManagedReloadResult.Success<ExampleConfig>>(result)
        assertEquals("Custom", result.value.title)
        assertEquals(6, result.value.rows)
        assertTrue(result.report.mergedMissingKeys.contains("rows"))
        val text = file.readText()
        assertTrue(text.contains("unknown: keep-me"))
        assertTrue(text.contains("rows: 6"))
        assertTrue(text.contains("title: Custom"))
    }

    @Test
    fun `missing version key is treated as version one and written back`() {
        val directory = createTempDirectory("daisy-managed-version").toFile()
        val file = File(directory, "settings.yml")
        file.writeText("title: Legacy\n")

        val result =
            DaisyManagedYaml.load(
                rootDirectory = directory,
                resourceLoader = { "title: Default\nconfig_version: 1\n" },
                file = managedFile(),
            )

        assertIs<DaisyManagedReloadResult.Success<ExampleConfig>>(result)
        assertEquals(null, result.report.versionBefore)
        assertEquals(1, result.report.versionAfter)
        assertTrue(file.readText().contains("config_version: 1"))
    }

    @Test
    fun `managed handle preserves last good value on reload failure`() {
        val directory = createTempDirectory("daisy-managed-handle").toFile()
        val file = File(directory, "settings.yml")
        file.writeText("title: Valid\nrows: 2\nconfig_version: 1\n")

        val handle =
            DaisyManagedYaml.handle(
                rootDirectory = directory,
                resourceLoader = { "title: Default\nrows: 3\nconfig_version: 1\n" },
                file = managedFile(),
            )

        file.writeText("title: Broken\nrows: nope\nconfig_version: 1\n")

        val result = handle.migrate()
        assertIs<DaisyManagedReloadResult.Failure<ExampleConfig>>(result)
        assertEquals("Valid", handle.current.title)
        assertEquals("Valid", result.previousValue?.title)
    }

    @Test
    fun `managed load does not rewrite untouched file`() {
        val directory = createTempDirectory("daisy-managed-stable").toFile()
        val file = File(directory, "settings.yml")
        file.writeText("title: Stable\nrows: 3\nconfig_version: 1\n")

        val first =
            DaisyManagedYaml.load(
                rootDirectory = directory,
                resourceLoader = { "title: Stable\nrows: 3\nconfig_version: 1\n" },
                file = managedFile(),
            )
        val second =
            DaisyManagedYaml.load(
                rootDirectory = directory,
                resourceLoader = { "title: Stable\nrows: 3\nconfig_version: 1\n" },
                file = managedFile(),
            )

        assertIs<DaisyManagedReloadResult.Success<ExampleConfig>>(first)
        assertIs<DaisyManagedReloadResult.Success<ExampleConfig>>(second)
        assertFalse(second.report.wroteToDisk)
    }

    private fun managedFile(
        migrations: List<DaisyYamlMigration> = emptyList(),
        currentVersion: Int = 1,
    ): DaisyManagedYamlFile<ExampleConfig> =
        DaisyManagedYamlFile(
            id = "settings",
            path = "settings.yml",
            codec = codec,
            currentVersion = currentVersion,
            migrations = migrations,
        )
}

private data class ExampleConfig(
    val title: String,
    val rows: Int,
)
