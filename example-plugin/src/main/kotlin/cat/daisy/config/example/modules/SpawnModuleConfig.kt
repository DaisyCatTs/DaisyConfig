package cat.daisy.config.example.modules

import cat.daisy.config.DaisyConfigCodec
import cat.daisy.config.defaulted
import cat.daisy.config.intCodec
import cat.daisy.config.objectCodec
import cat.daisy.config.stringCodec

data class SpawnModuleConfig(
    val delaySeconds: Int,
    val bypassPermission: String,
)

private data class SpawnSection(
    val delay: Int,
    val bypassPermission: String,
)

val spawnModuleConfigCodec: DaisyConfigCodec<SpawnModuleConfig> =
    objectCodec {
        val spawn =
            section(
                "spawn",
                objectCodec {
                    SpawnSection(
                        delay = defaulted("delay", intCodec(), 3),
                        bypassPermission = defaulted("bypass_permission", stringCodec(), "example.spawn.bypass"),
                    )
                },
            )
        SpawnModuleConfig(
            delaySeconds = spawn.delay,
            bypassPermission = spawn.bypassPermission,
        )
    }
