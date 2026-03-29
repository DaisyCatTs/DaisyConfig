package cat.daisy.config.example

import cat.daisy.config.DaisyConfigCodec
import cat.daisy.config.DaisyValidation
import cat.daisy.config.defaulted
import cat.daisy.config.intCodec
import cat.daisy.config.itemFlagsCodec
import cat.daisy.config.listCodec
import cat.daisy.config.materialCodec
import cat.daisy.config.objectCodec
import cat.daisy.config.potionEffectCodec
import cat.daisy.config.soundCodec
import cat.daisy.config.stringCodec
import cat.daisy.config.validate
import cat.daisy.config.enchantmentCodec
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.potion.PotionEffectType

data class ExampleProfileUiConfig(
    val icon: Material,
    val flags: Set<ItemFlag>,
    val enchantment: Enchantment,
    val effect: PotionEffectType,
    val feedback: ExampleFeedbackConfig,
)

data class ExampleFeedbackConfig(
    val sound: Sound,
    val message: String,
)

data class ExampleProfileLayoutConfig(
    val menu: ExampleProfileMenuConfig,
    val sidebar: ExampleProfileSidebarConfig,
    val tablist: ExampleProfileTablistConfig,
)

data class ExampleProfileMenuConfig(
    val rows: Int,
    val profileSlot: Int,
    val borderMaterial: Material,
)

data class ExampleProfileSidebarConfig(
    val title: String,
    val lines: List<String>,
)

data class ExampleProfileTablistConfig(
    val header: String,
    val footer: String,
)

data class ExampleProfileFeatureConfig(
    val ui: ExampleProfileUiConfig,
    val layout: ExampleProfileLayoutConfig,
)

val exampleFeedbackCodec: DaisyConfigCodec<ExampleFeedbackConfig> =
    objectCodec {
        ExampleFeedbackConfig(
            sound = required("sound", soundCodec()),
            message = defaulted("message", stringCodec(), "<green>Profile updated.</green>"),
        )
    }

val exampleProfileUiCodec: DaisyConfigCodec<ExampleProfileUiConfig> =
    objectCodec {
        ExampleProfileUiConfig(
            icon = required("icon", materialCodec()),
            flags = defaulted("flags", itemFlagsCodec(), emptySet()),
            enchantment = required("enchantment", enchantmentCodec()),
            effect = required("effect", potionEffectCodec()),
            feedback = section("feedback", exampleFeedbackCodec),
        )
    }

private val exampleProfileMenuCodec: DaisyConfigCodec<ExampleProfileMenuConfig> =
    objectCodec {
        ExampleProfileMenuConfig(
            rows = defaulted("rows", intCodec(), 3),
            profileSlot = defaulted("profile_slot", intCodec(), 13),
            borderMaterial = defaulted("border_material", materialCodec(), Material.GRAY_STAINED_GLASS_PANE),
        )
    }.validate { config ->
        buildList {
            addAll(DaisyValidation.intRange("menu.rows", config.rows, 1, 6))
            addAll(
                DaisyValidation.require(
                    condition = config.profileSlot in 0 until (config.rows * 9),
                    path = "menu.profile_slot",
                    message = "Slot must be within the menu size for ${config.rows} rows.",
                ),
            )
        }
    }

private val exampleProfileSidebarCodec: DaisyConfigCodec<ExampleProfileSidebarConfig> =
    objectCodec {
        ExampleProfileSidebarConfig(
            title = defaulted("title", stringCodec(), "<gradient:#7dd3fc:#c4b5fd>Profile</gradient>"),
            lines = defaulted("lines", listCodec(stringCodec()), emptyList()),
        )
    }.validate { config ->
        buildList {
            addAll(DaisyValidation.notBlank("sidebar.title", config.title))
            addAll(
                DaisyValidation.require(
                    condition = config.lines.size <= 15,
                    path = "sidebar.lines",
                    message = "Sidebar supports up to 15 lines.",
                ),
            )
        }
    }

private val exampleProfileTablistCodec: DaisyConfigCodec<ExampleProfileTablistConfig> =
    objectCodec {
        ExampleProfileTablistConfig(
            header = defaulted("header", stringCodec(), "<gradient:#7dd3fc:#c4b5fd>Profile</gradient>"),
            footer = defaulted("footer", stringCodec(), "<gray>DaisyConfig bundle example</gray>"),
        )
    }.validate { config ->
        buildList {
            addAll(DaisyValidation.notBlank("tablist.header", config.header))
            addAll(DaisyValidation.notBlank("tablist.footer", config.footer))
        }
    }

val exampleProfileLayoutCodec: DaisyConfigCodec<ExampleProfileLayoutConfig> =
    objectCodec {
        ExampleProfileLayoutConfig(
            menu = section("menu", exampleProfileMenuCodec),
            sidebar = section("sidebar", exampleProfileSidebarCodec),
            tablist = section("tablist", exampleProfileTablistCodec),
        )
    }
