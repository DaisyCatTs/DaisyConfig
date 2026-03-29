package cat.daisy.config.example

import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.potion.PotionEffectType

data class ExampleProfileUiConfig(
    val icon: Material,
    val feedbackSound: Sound,
    val flags: Set<ItemFlag>,
    val enchantment: Enchantment,
    val effect: PotionEffectType,
    val sidebarTitle: String,
)

