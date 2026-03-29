package cat.daisy.config.example

import cat.daisy.core.platform.DaisyPlatform
import org.bukkit.plugin.java.JavaPlugin

class DaisyConfigExamplePlugin : JavaPlugin() {
    lateinit var modules: ExampleModuleRegistry
        private set

    private lateinit var daisy: DaisyPlatform

    override fun onEnable() {
        modules = ExampleModuleRegistry(this)

        daisy =
            DaisyPlatform.create(this) {
                commands()
            }

        logger.info(
            "Loaded DaisyConfig managed modules: " +
                "spawn=${modules.spawn.current.settings.delaySeconds}s, " +
                "warp=${modules.warp.current.settings.maxWarps} max, " +
                "storeRows=${modules.store.current.settings.rows}",
        )
    }

    override fun onDisable() {
        if (::daisy.isInitialized) {
            daisy.close()
        }
    }
}
