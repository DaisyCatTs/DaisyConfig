package cat.daisy.config.example.modules

import cat.daisy.config.DaisyConfigCodec
import cat.daisy.config.defaulted
import cat.daisy.config.intCodec
import cat.daisy.config.materialCodec
import cat.daisy.config.objectCodec
import cat.daisy.config.soundCodec
import org.bukkit.Material
import org.bukkit.Sound

data class StoreModuleConfig(
    val rows: Int,
    val icon: Material,
    val openSound: Sound,
)

private data class StoreSection(
    val rows: Int,
    val icon: Material,
    val openSound: Sound,
)

val storeModuleConfigCodec: DaisyConfigCodec<StoreModuleConfig> =
    objectCodec {
        val store =
            section(
                "store",
                objectCodec {
                    StoreSection(
                        rows = defaulted("rows", intCodec(), 3),
                        icon = defaulted("icon", materialCodec(), Material.EMERALD),
                        openSound = defaulted("open_sound", soundCodec(), Sound.BLOCK_ENDER_CHEST_OPEN),
                    )
                },
            )
        StoreModuleConfig(
            rows = store.rows,
            icon = store.icon,
            openSound = store.openSound,
        )
    }
