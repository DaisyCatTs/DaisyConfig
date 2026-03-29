package cat.daisy.config.example.modules

import cat.daisy.config.DaisyConfigCodec
import cat.daisy.config.defaulted
import cat.daisy.config.intCodec
import cat.daisy.config.objectCodec

data class WarpModuleConfig(
    val warmupSeconds: Int,
    val maxWarps: Int,
)

private data class WarpSection(
    val warmupSeconds: Int,
    val maxWarps: Int,
)

val warpModuleConfigCodec: DaisyConfigCodec<WarpModuleConfig> =
    objectCodec {
        val warp =
            section(
                "warp",
                objectCodec {
                    WarpSection(
                        warmupSeconds = defaulted("warmup_seconds", intCodec(), 2),
                        maxWarps = defaulted("max_warps", intCodec(), 5),
                    )
                },
            )
        WarpModuleConfig(
            warmupSeconds = warp.warmupSeconds,
            maxWarps = warp.maxWarps,
        )
    }
