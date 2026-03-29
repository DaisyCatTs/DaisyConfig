package cat.daisy.config.example

import cat.daisy.config.daisycore.DaisyTextConfig
import cat.daisy.config.daisycore.daisyTextConfigCodec
import cat.daisy.config.example.modules.SpawnModuleConfig
import cat.daisy.config.example.modules.StoreModuleConfig
import cat.daisy.config.example.modules.WarpModuleConfig
import cat.daisy.config.example.modules.spawnModuleConfigCodec
import cat.daisy.config.example.modules.storeModuleConfigCodec
import cat.daisy.config.example.modules.warpModuleConfigCodec
import cat.daisy.config.managed.DaisyManagedYamlFile
import cat.daisy.config.managed.DaisyYamlMigrations
import cat.daisy.config.modules.DaisyModuleDefinition
import cat.daisy.config.modules.DaisyModuleHandle
import cat.daisy.config.modules.DaisyModuleRegistry
import cat.daisy.config.modules.DaisyModules
import org.bukkit.plugin.java.JavaPlugin

class ExampleModuleRegistry(
    plugin: JavaPlugin,
) {
    private val registry: DaisyModuleRegistry =
        DaisyModules.load(plugin) {
            module(
                DaisyModuleDefinition(
                    category = "commands",
                    module = "spawn",
                    settings =
                        DaisyManagedYamlFile(
                            id = "commands/spawn/settings",
                            path = "modules/commands/spawn/settings.yml",
                            codec = spawnModuleConfigCodec,
                            currentVersion = 2,
                            migrations = listOf(DaisyYamlMigrations.move(1, 2, "spawn-delay", "spawn.delay")),
                        ),
                    lang = langFile("commands", "spawn"),
                ),
            )
            module(
                DaisyModuleDefinition(
                    category = "commands",
                    module = "warp",
                    settings =
                        DaisyManagedYamlFile(
                            id = "commands/warp/settings",
                            path = "modules/commands/warp/settings.yml",
                            codec = warpModuleConfigCodec,
                            currentVersion = 2,
                        ),
                    lang = langFile("commands", "warp"),
                ),
            )
            module(
                DaisyModuleDefinition(
                    category = "guis",
                    module = "store",
                    settings =
                        DaisyManagedYamlFile(
                            id = "guis/store/settings",
                            path = "modules/guis/store/settings.yml",
                            codec = storeModuleConfigCodec,
                        ),
                    lang = langFile("guis", "store"),
                ),
            )
        }

    val spawn: DaisyModuleHandle<SpawnModuleConfig>
        get() = registry.require("commands", "spawn")

    val warp: DaisyModuleHandle<WarpModuleConfig>
        get() = registry.require("commands", "warp")

    val store: DaisyModuleHandle<StoreModuleConfig>
        get() = registry.require("guis", "store")

    fun reloadAll() = registry.reloadAll()

    fun migrateAll() = registry.migrateAll()

    private fun langFile(
        category: String,
        module: String,
    ): DaisyManagedYamlFile<DaisyTextConfig> =
        DaisyManagedYamlFile(
            id = "$category/$module/lang",
            path = "modules/$category/$module/lang.yml",
            codec = daisyTextConfigCodec(),
        )
}
