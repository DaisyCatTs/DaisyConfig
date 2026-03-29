package cat.daisy.config.example
import cat.daisy.config.DaisyConfigHandle
import cat.daisy.config.defaulted
import cat.daisy.config.daisycore.asDaisyTextSource
import cat.daisy.config.daisycore.yamlTextConfigHandle
import cat.daisy.config.enchantmentCodec
import cat.daisy.config.itemFlagsCodec
import cat.daisy.config.materialCodec
import cat.daisy.config.objectCodec
import cat.daisy.config.potionEffectCodec
import cat.daisy.config.soundCodec
import cat.daisy.config.stringCodec
import cat.daisy.config.yaml.ensureDefaultConfigResource
import cat.daisy.config.yaml.yamlConfigHandle
import cat.daisy.core.platform.DaisyPlatform
import org.bukkit.plugin.java.JavaPlugin

class DaisyConfigExamplePlugin : JavaPlugin() {
    lateinit var profileConfig: DaisyConfigHandle<ExampleProfileUiConfig>
        private set

    private lateinit var daisy: DaisyPlatform

    override fun onEnable() {
        ensureDefaultConfigResource("profile-ui.yml")
        ensureDefaultConfigResource("lang.yml")

        val codec =
            objectCodec {
                ExampleProfileUiConfig(
                    icon = required("icon", materialCodec()),
                    feedbackSound = required("feedback_sound", soundCodec()),
                    flags = defaulted("flags", itemFlagsCodec(), emptySet()),
                    enchantment = required("enchantment", enchantmentCodec()),
                    effect = required("effect", potionEffectCodec()),
                    sidebarTitle = defaulted("sidebar_title", stringCodec(), "<gradient:#7dd3fc:#c4b5fd>Profile</gradient>"),
                )
            }

        profileConfig = yamlConfigHandle("profile-ui.yml", codec)
        val langConfig = yamlTextConfigHandle(this, "lang.yml")

        daisy =
            DaisyPlatform.create(this) {
                messages(langConfig.current.asDaisyTextSource())
                commands()
                menus()
                scoreboards()
                tablists()
            }
    }

    override fun onDisable() {
        daisy.close()
    }
}
