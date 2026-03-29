package cat.daisy.config.managed

import cat.daisy.config.intCodec
import cat.daisy.config.objectCodec
import cat.daisy.config.stringCodec
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DaisyYamlMigrationsTest {
    private val spawnCodec =
        objectCodec {
            SpawnSection(
                delay = required("delay", intCodec()),
            )
        }

    private val codec =
        objectCodec {
            MigratedConfig(
                delay = section("spawn", spawnCodec).delay,
                title = required("title", stringCodec()),
            )
        }

    @Test
    fun `migrations rename keys in order and advance version`() {
        val directory = createTempDirectory("daisy-managed-migration").toFile()
        val file = File(directory, "settings.yml")
        file.writeText("spawn-delay: 10\ntitle: Spawn\nconfig_version: 1\n")

        val result =
            DaisyManagedYaml.load(
                rootDirectory = directory,
                resourceLoader = { "spawn:\n  delay: 5\ntitle: Spawn\nconfig_version: 2\n" },
                file =
                    DaisyManagedYamlFile(
                        id = "spawn",
                        path = "settings.yml",
                        codec = codec,
                        currentVersion = 2,
                        migrations = listOf(DaisyYamlMigrations.move(1, 2, "spawn-delay", "spawn.delay")),
                    ),
            )

        assertIs<DaisyManagedReloadResult.Success<MigratedConfig>>(result)
        assertEquals(10, result.value.delay)
        assertEquals(listOf("spawn-delay" to "spawn.delay"), result.report.renamedKeys)
        assertEquals(1, result.report.versionBefore)
        assertEquals(2, result.report.versionAfter)
        val text = file.readText()
        assertTrue(text.contains("delay: 10"))
        assertTrue(text.contains("config_version: 2"))
    }

    @Test
    fun `broken migration chain is rejected`() {
        val directory = createTempDirectory("daisy-managed-chain").toFile()
        File(directory, "settings.yml").writeText("title: Spawn\nspawn:\n  delay: 5\nconfig_version: 1\n")

        val result =
            DaisyManagedYaml.load(
                rootDirectory = directory,
                resourceLoader = { "spawn:\n  delay: 5\ntitle: Spawn\nconfig_version: 3\n" },
                file =
                    DaisyManagedYamlFile(
                        id = "spawn",
                        path = "settings.yml",
                        codec = codec,
                        currentVersion = 3,
                        migrations = listOf(DaisyYamlMigrations.setDefault(1, 2, "spawn.mode", "safe")),
                    ),
            )

        assertIs<DaisyManagedReloadResult.Failure<MigratedConfig>>(result)
        assertTrue(result.errors.first().message.contains("No migration path"))
    }
}

private data class MigratedConfig(
    val delay: Int,
    val title: String,
)

private data class SpawnSection(
    val delay: Int,
)
