package cat.daisy.config.modules

import cat.daisy.config.defaulted
import cat.daisy.config.daisycore.DaisyTextConfig
import cat.daisy.config.daisycore.daisyTextConfigCodec
import cat.daisy.config.intCodec
import cat.daisy.config.managed.DaisyManagedBundleReloadResult
import cat.daisy.config.managed.DaisyManagedYamlFile
import cat.daisy.config.managed.DaisyYamlMigrations
import cat.daisy.config.objectCodec
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DaisyModulesTest {
    private val spawnSectionCodec =
        objectCodec {
            SpawnSection(
                delay = defaulted("delay", intCodec(), 0),
            )
        }

    private val spawnSettingsCodec =
        objectCodec {
            SpawnSettings(
                delay = section("spawn", spawnSectionCodec).delay,
            )
        }

    @Test
    fun `module registry loads settings and lang pairs`() {
        val directory = createTempDirectory("daisy-modules-load").toFile()
        val resources =
            mapOf(
                "modules/commands/spawn/settings.yml" to "spawn:\n  delay: 3\nconfig_version: 1\n",
                "modules/commands/spawn/lang.yml" to "messages:\n  ready: <green>Ready</green>\nconfig_version: 1\n",
            )
        val registry = registry(directory, resources)

        val handle = registry.require<SpawnSettings>("commands", "spawn")
        assertEquals(3, handle.current.settings.delay)
        assertEquals("<green>Ready</green>", handle.textSource.text("messages.ready"))
    }

    @Test
    fun `module registry supports optional lang and lookup`() {
        val directory = createTempDirectory("daisy-modules-optional").toFile()
        val resources =
            mapOf(
                "modules/commands/spawn/settings.yml" to "spawn:\n  delay: 2\nconfig_version: 1\n",
                "modules/commands/spawn/lang.yml" to "messages:\n  ready: <green>Ready</green>\nconfig_version: 1\n",
                "modules/commands/warp/settings.yml" to "spawn:\n  delay: 7\nconfig_version: 1\n",
            )
        val registry =
            registry(directory, resources) {
                module(
                    DaisyModuleDefinition(
                        category = "commands",
                        module = "warp",
                        settings =
                            DaisyManagedYamlFile(
                                id = "commands/warp/settings",
                                path = "modules/commands/warp/settings.yml",
                                codec = spawnSettingsCodec,
                            ),
                    ),
                )
            }

        val handle = registry.require<SpawnSettings>("commands", "warp")
        assertEquals(7, handle.current.settings.delay)
        assertEquals(null, handle.current.lang)
    }

    @Test
    fun `module migrate all preserves previous state on failure and aggregates reports`() {
        val directory = createTempDirectory("daisy-modules-reload").toFile()
        val resources =
            mapOf(
                "modules/commands/spawn/settings.yml" to "spawn:\n  delay: 3\nconfig_version: 2\n",
                "modules/commands/spawn/lang.yml" to "messages:\n  ready: <green>Ready</green>\nconfig_version: 1\n",
            )
        val registry = registry(directory, resources)
        val spawnHandle = registry.require<SpawnSettings>("commands", "spawn")
        assertEquals(3, spawnHandle.current.settings.delay)

        File(directory, "modules/commands/spawn/settings.yml").writeText("spawn:\n  delay: nope\nconfig_version: 2\n")

        val result = registry.migrateAll()
        assertIs<DaisyManagedBundleReloadResult.Failure<Map<String, Any?>>>(result)
        assertEquals(3, spawnHandle.current.settings.delay)
        assertTrue(result.reports.isNotEmpty())
    }

    private fun registry(
        directory: File,
        resources: Map<String, String>,
        extra: DaisyModuleRegistryBuilder.() -> Unit = {},
    ): DaisyModuleRegistry {
        val builder = TestModuleRegistryBuilder()
        builder.module(
            DaisyModuleDefinition(
                category = "commands",
                module = "spawn",
                settings =
                    DaisyManagedYamlFile(
                        id = "commands/spawn/settings",
                        path = "modules/commands/spawn/settings.yml",
                        codec = spawnSettingsCodec,
                        currentVersion = 2,
                        migrations = listOf(DaisyYamlMigrations.move(1, 2, "spawn-delay", "spawn.delay")),
                    ),
                lang =
                    DaisyManagedYamlFile(
                        id = "commands/spawn/lang",
                        path = "modules/commands/spawn/lang.yml",
                        codec = daisyTextConfigCodec(),
                    ),
            ),
        )
        builder.extra()
        return cat.daisy.config.modules.internal.DaisyModuleRegistryImpl(
            rootDirectory = directory,
            resourceLoader = resources::get,
            definitions = builder.definitions,
        )
    }
}

private class TestModuleRegistryBuilder : DaisyModuleRegistryBuilder {
    val definitions = mutableListOf<DaisyModuleDefinition<*>>()

    override fun <S> module(definition: DaisyModuleDefinition<S>) {
        definitions += definition
    }
}

private data class SpawnSettings(
    val delay: Int,
)

private data class SpawnSection(
    val delay: Int,
)
