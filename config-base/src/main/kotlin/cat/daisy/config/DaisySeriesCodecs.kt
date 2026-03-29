package cat.daisy.config

import cat.daisy.series.enchantment.DaisyEnchantments
import cat.daisy.series.itemflag.DaisyItemFlags
import cat.daisy.series.material.DaisyMaterials
import cat.daisy.series.potion.DaisyPotions
import cat.daisy.series.sound.DaisySounds
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.potion.PotionEffectType

public fun materialCodec(): DaisyConfigCodec<Material> = parserBackedCodec("material", DaisyMaterials::parseOrNull)

public fun soundCodec(): DaisyConfigCodec<Sound> = parserBackedCodec("sound", DaisySounds::parseOrNull)

public fun itemFlagCodec(): DaisyConfigCodec<ItemFlag> = parserBackedCodec("item flag", DaisyItemFlags::parseOrNull)

public fun itemFlagsCodec(): DaisyConfigCodec<Set<ItemFlag>> = setCodec(itemFlagCodec())

public fun enchantmentCodec(): DaisyConfigCodec<Enchantment> = parserBackedCodec("enchantment", DaisyEnchantments::parseOrNull)

public fun potionEffectCodec(): DaisyConfigCodec<PotionEffectType> = parserBackedCodec("potion effect", DaisyPotions::parseOrNull)

private fun <T> parserBackedCodec(
    type: String,
    parser: (String) -> T?,
): DaisyConfigCodec<T> =
    DaisyConfigCodec { node, path ->
        val text = node.asString()
        when {
            text == null -> DaisyDecodeResult.Failure(listOf(DaisyConfigError(path, "Expected $type string.")))
            else -> parser(text)?.let { DaisyDecodeResult.Success(it) }
                ?: DaisyDecodeResult.Failure(listOf(DaisyConfigError(path, "Unknown $type '$text'.")))
        }
    }

