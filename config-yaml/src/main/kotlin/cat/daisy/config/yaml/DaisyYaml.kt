package cat.daisy.config.yaml

import cat.daisy.config.DaisyConfigCodec
import cat.daisy.config.DaisyConfigHandle
import cat.daisy.config.DaisyConfigNode
import cat.daisy.config.DaisyReloadResult
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

public object DaisyYaml {
    public fun <T> load(
        file: File,
        codec: DaisyConfigCodec<T>,
    ): DaisyReloadResult<T> {
        val yaml = YamlConfiguration.loadConfiguration(file)
        val result = codec.decode(YamlConfigNode(yaml), "")
        return when (result) {
            is cat.daisy.config.DaisyDecodeResult.Success -> DaisyReloadResult.Success(result.value, result.warnings)
            is cat.daisy.config.DaisyDecodeResult.Failure -> DaisyReloadResult.Failure(result.errors, null)
        }
    }

    public fun <T> handle(
        file: File,
        codec: DaisyConfigCodec<T>,
    ): DaisyConfigHandle<T> {
        val initial = load(file, codec)
        require(initial is DaisyReloadResult.Success<T>) {
            val errors = (initial as DaisyReloadResult.Failure).errors.joinToString { "${it.path}: ${it.message}" }
            "Failed to load config ${file.name}: $errors"
        }
        return DaisyYamlConfigHandle(file = file, codec = codec, currentValue = initial.value)
    }
}

private class DaisyYamlConfigHandle<T>(
    private val file: File,
    private val codec: DaisyConfigCodec<T>,
    currentValue: T,
) : DaisyConfigHandle<T> {
    override var current: T = currentValue
        private set

    override fun reload(): DaisyReloadResult<T> {
        val yaml = YamlConfiguration.loadConfiguration(file)
        val result = codec.decode(YamlConfigNode(yaml), "")
        return when (result) {
            is cat.daisy.config.DaisyDecodeResult.Success -> {
                current = result.value
                DaisyReloadResult.Success(result.value, result.warnings)
            }
            is cat.daisy.config.DaisyDecodeResult.Failure -> DaisyReloadResult.Failure(result.errors, current)
        }
    }
}

public fun JavaPlugin.configFile(name: String): File = File(dataFolder, name)

public fun <T> JavaPlugin.loadYamlConfig(
    name: String,
    codec: DaisyConfigCodec<T>,
): DaisyReloadResult<T> = DaisyYaml.load(configFile(name), codec)

public fun <T> JavaPlugin.yamlConfigHandle(
    name: String,
    codec: DaisyConfigCodec<T>,
): DaisyConfigHandle<T> = DaisyYaml.handle(configFile(name), codec)

public fun JavaPlugin.ensureDefaultConfigResource(
    resourcePath: String,
    destinationName: String = resourcePath,
) {
    val destination = configFile(destinationName)
    destination.parentFile?.mkdirs()
    if (!destination.exists()) {
        saveResource(resourcePath, false)
    }
}

private class YamlConfigNode(
    private val value: Any?,
) : DaisyConfigNode {
    override fun isNull(): Boolean = value == null

    override fun asString(): String? = value as? String

    override fun asInt(): Int? = when (value) {
        is Int -> value
        is Number -> value.toInt()
        else -> null
    }

    override fun asLong(): Long? = when (value) {
        is Long -> value
        is Number -> value.toLong()
        else -> null
    }

    override fun asDouble(): Double? = when (value) {
        is Double -> value
        is Number -> value.toDouble()
        else -> null
    }

    override fun asBoolean(): Boolean? = value as? Boolean

    override fun get(key: String): DaisyConfigNode =
        when (value) {
            is ConfigurationSection -> YamlConfigNode(value.get(key))
            is Map<*, *> -> YamlConfigNode(value[key])
            else -> YamlConfigNode(null)
        }

    override fun entries(): Map<String, DaisyConfigNode> =
        when (value) {
            is ConfigurationSection -> value.getKeys(false).associateWith { YamlConfigNode(value.get(it)) }
            is Map<*, *> -> value.entries.associate { it.key.toString() to YamlConfigNode(it.value) }
            else -> emptyMap()
        }

    override fun elements(): List<DaisyConfigNode> = (value as? List<*>)?.map(::YamlConfigNode).orEmpty()
}
