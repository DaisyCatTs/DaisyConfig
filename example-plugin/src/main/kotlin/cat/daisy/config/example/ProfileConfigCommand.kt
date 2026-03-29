package cat.daisy.config.example

import cat.daisy.command.DaisyCommandGroup
import cat.daisy.command.DaisyCommandSet
import cat.daisy.config.DaisyConfigError
import cat.daisy.config.DaisyReloadResult
import cat.daisy.series.enchantment.DaisyEnchantments
import cat.daisy.series.material.DaisyMaterials
import cat.daisy.series.potion.DaisyPotions
import cat.daisy.series.sound.DaisySounds

@DaisyCommandSet
class ProfileConfigCommand(
    private val plugin: DaisyConfigExamplePlugin,
) : DaisyCommandGroup({
    command("profileconfig") {
        description("Open and reload the DaisyConfig Phase 2 example")

        player {
            plugin.openProfile(player)
            replyLang("messages.profile.opening", "player" to player.name)
        }

        subcommand("show") {
            player {
                val feature = plugin.profileBundle.current
                reply("<gradient:#7dd3fc:#c4b5fd>Icon</gradient>: ${DaisyMaterials.displayName(feature.ui.icon)} (${DaisyMaterials.key(feature.ui.icon)})")
                reply("<gradient:#7dd3fc:#c4b5fd>Sound</gradient>: ${DaisySounds.displayName(feature.ui.feedback.sound)}")
                reply("<gradient:#7dd3fc:#c4b5fd>Enchant</gradient>: ${DaisyEnchantments.displayName(feature.ui.enchantment)}")
                reply("<gradient:#7dd3fc:#c4b5fd>Effect</gradient>: ${DaisyPotions.displayName(feature.ui.effect)}")
                reply("<gradient:#7dd3fc:#c4b5fd>Menu rows</gradient>: ${feature.layout.menu.rows}")
                reply("<gradient:#7dd3fc:#c4b5fd>Sidebar lines</gradient>: ${feature.layout.sidebar.lines.size}")
            }
        }

        subcommand("reload") {
            player {
                val errors = buildList {
                    addAll(plugin.profileBundle.reload().errorsOrEmpty())
                    addAll(plugin.textBundle.reload().errorsOrEmpty())
                }

                if (errors.isEmpty()) {
                    replyLang("messages.profile.reloaded")
                } else {
                    reply("<red>Reload failed.</red>")
                    errors.forEach { error ->
                        reply("<gray>${error.path}: ${error.message}</gray>")
                    }
                }
            }
        }
    }
})

private fun <T> DaisyReloadResult<T>.errorsOrEmpty(): List<DaisyConfigError> =
    when (this) {
        is DaisyReloadResult.Success -> emptyList()
        is DaisyReloadResult.Failure -> errors
    }
