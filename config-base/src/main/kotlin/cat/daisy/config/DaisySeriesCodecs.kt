package cat.daisy.config

import cat.daisy.series.attribute.DaisyAttributes
import cat.daisy.series.biome.DaisyBiomes
import cat.daisy.series.blockface.DaisyBlockFaces
import cat.daisy.series.damagecause.DaisyDamageCauses
import cat.daisy.series.difficulty.DaisyDifficulties
import cat.daisy.series.enchantment.DaisyEnchantments
import cat.daisy.series.entity.DaisyEntities
import cat.daisy.series.gamemode.DaisyGameModes
import cat.daisy.series.itemflag.DaisyItemFlags
import cat.daisy.series.material.DaisyMaterials
import cat.daisy.series.operation.DaisyOperations
import cat.daisy.series.particle.DaisyParticles
import cat.daisy.series.patterntype.DaisyPatternTypes
import cat.daisy.series.potion.DaisyPotions
import cat.daisy.series.sound.DaisySounds
import cat.daisy.series.statistic.DaisyStatistics
import cat.daisy.series.villagerprofession.DaisyVillagerProfessions
import org.bukkit.Difficulty
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.Statistic
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.block.Biome
import org.bukkit.block.BlockFace
import org.bukkit.block.banner.PatternType
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.EntityType
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.entity.Villager
import org.bukkit.inventory.ItemFlag
import org.bukkit.potion.PotionEffectType

public fun materialCodec(): DaisyConfigCodec<Material> = parserBackedCodec("material", DaisyMaterials::parseOrNull)

public fun soundCodec(): DaisyConfigCodec<Sound> = parserBackedCodec("sound", DaisySounds::parseOrNull)

public fun biomeCodec(): DaisyConfigCodec<Biome> = parserBackedCodec("biome", DaisyBiomes::parseOrNull)

public fun entityTypeCodec(): DaisyConfigCodec<EntityType> = parserBackedCodec("entity type", DaisyEntities::parseOrNull)

public fun villagerProfessionCodec(): DaisyConfigCodec<Villager.Profession> =
    parserBackedCodec("villager profession", DaisyVillagerProfessions::parseOrNull)

public fun attributeCodec(): DaisyConfigCodec<Attribute> = parserBackedCodec("attribute", DaisyAttributes::parseOrNull)

public fun gameModeCodec(): DaisyConfigCodec<GameMode> = parserBackedCodec("game mode", DaisyGameModes::parseOrNull)

public fun difficultyCodec(): DaisyConfigCodec<Difficulty> = parserBackedCodec("difficulty", DaisyDifficulties::parseOrNull)

public fun blockFaceCodec(): DaisyConfigCodec<BlockFace> = parserBackedCodec("block face", DaisyBlockFaces::parseOrNull)

public fun damageCauseCodec(): DaisyConfigCodec<EntityDamageEvent.DamageCause> =
    parserBackedCodec("damage cause", DaisyDamageCauses::parseOrNull)

public fun operationCodec(): DaisyConfigCodec<AttributeModifier.Operation> =
    parserBackedCodec("operation", DaisyOperations::parseOrNull)

public fun patternTypeCodec(): DaisyConfigCodec<PatternType> =
    parserBackedCodec("pattern type", DaisyPatternTypes::parseOrNull)

public fun particleCodec(): DaisyConfigCodec<Particle> = parserBackedCodec("particle", DaisyParticles::parseOrNull)

public fun statisticCodec(): DaisyConfigCodec<Statistic> = parserBackedCodec("statistic", DaisyStatistics::parseOrNull)

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
