package cat.daisy.config.example

import cat.daisy.config.DaisyConfigBundleHandle
import cat.daisy.config.daisycore.DaisyTextConfig
import cat.daisy.config.daisycore.asDaisyTextSource
import cat.daisy.config.daisycore.daisyTextConfigCodec
import cat.daisy.config.daisycore.mergeTextConfigs
import cat.daisy.config.yaml.ensureDefaultConfigResources
import cat.daisy.config.yaml.yamlConfigBundleHandle
import cat.daisy.core.platform.DaisyPlatform
import cat.daisy.menu.openMenu
import cat.daisy.scoreboard.sidebar
import cat.daisy.tablist.tablist
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

class DaisyConfigExamplePlugin : JavaPlugin() {
    lateinit var profileBundle: DaisyConfigBundleHandle<ExampleProfileFeatureConfig>
        private set

    lateinit var textBundle: DaisyConfigBundleHandle<DaisyTextConfig>
        private set

    private lateinit var daisy: DaisyPlatform

    override fun onEnable() {
        ensureDefaultConfigResources(
            "profile-ui.yml",
            "profile-layout.yml",
            "lang.yml",
            "profile-text.yml",
        )

        profileBundle =
            yamlConfigBundleHandle {
                ExampleProfileFeatureConfig(
                    ui = file("profile-ui.yml", exampleProfileUiCodec),
                    layout = file("profile-layout.yml", exampleProfileLayoutCodec),
                )
            }

        textBundle =
            yamlConfigBundleHandle {
                mergeTextConfigs(
                    file("lang.yml", daisyTextConfigCodec()),
                    file("profile-text.yml", daisyTextConfigCodec()),
                )
            }

        daisy =
            DaisyPlatform.create(this) {
                messages(textBundle.asDaisyTextSource())
                commands()
                menus()
                scoreboards()
                tablists()
            }
    }

    override fun onDisable() {
        daisy.close()
    }

    fun openProfile(player: Player) {
        val ui = profileBundle.current.ui
        val layout = profileBundle.current.layout
        val texts = textBundle.current

        player.openMenu(title = text("menus.profile.title"), rows = layout.menu.rows) {
            background(layout.menu.borderMaterial) {
                name(" ")
            }

            slot(layout.menu.profileSlot) {
                item(ui.icon) {
                    name(text("menus.profile.card.name"), player)
                    lore(texts.textList("menus.profile.card.lore"), player)
                    enchant(ui.enchantment)
                    flags(*ui.flags.toTypedArray())
                }
                message(ui.feedback.message)
                closeOnClick()
            }
        }

        daisy.scoreboards?.show(
            player,
            sidebar {
                title(layout.sidebar.title)
                layout.sidebar.lines.forEachIndexed { index, line ->
                    line("line_$index") { text(line) }
                }
            },
        )

        daisy.tablists?.show(
            player,
            tablist {
                header(layout.tablist.header)
                footer(layout.tablist.footer)
            },
        )

        player.playSound(player.location, ui.feedback.sound, 1f, 1f)
    }

    fun text(key: String): String = textBundle.current.text(key) ?: key
}
