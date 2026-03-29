package cat.daisy.config.example

import cat.daisy.command.DaisyCommandGroup
import cat.daisy.command.DaisyCommandSet
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
        description("Shows the current DaisyConfig example values")

        player {
            val config = plugin.profileConfig.current
            replyMm("<gradient:#7dd3fc:#c4b5fd>Icon</gradient>: ${DaisyMaterials.displayName(config.icon)} (${DaisyMaterials.key(config.icon)})")
            replyMm("<gradient:#7dd3fc:#c4b5fd>Sound</gradient>: ${DaisySounds.displayName(config.feedbackSound)}")
            replyMm("<gradient:#7dd3fc:#c4b5fd>Enchant</gradient>: ${DaisyEnchantments.displayName(config.enchantment)}")
            replyMm("<gradient:#7dd3fc:#c4b5fd>Effect</gradient>: ${DaisyPotions.displayName(config.effect)}")
        }

        subcommand("reload") {
            player {
                when (val result = plugin.profileConfig.reload()) {
                    is DaisyReloadResult.Success -> replyMm("<green>Reloaded profile-ui.yml.</green>")
                    is DaisyReloadResult.Failure -> replyMm("<red>Reload failed:</red> <gray>${result.errors.joinToString { "${it.path}: ${it.message}" }}</gray>")
                }
            }
        }
    }
})
