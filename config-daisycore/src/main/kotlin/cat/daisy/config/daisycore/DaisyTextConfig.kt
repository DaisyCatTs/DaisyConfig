package cat.daisy.config.daisycore

import cat.daisy.config.DaisyConfigCodec
import cat.daisy.config.DaisyConfigBundleHandle
import cat.daisy.config.DaisyConfigHandle
import cat.daisy.config.DaisyDecodeResult
import cat.daisy.config.yaml.yamlConfigHandle
import cat.daisy.text.DaisyTextSource
import org.bukkit.plugin.java.JavaPlugin

public interface DaisyTextConfig {
    public fun text(key: String): String?

    public fun textList(key: String): List<String>
}

public fun DaisyTextConfig.asDaisyTextSource(): DaisyTextSource =
    object : DaisyTextSource {
        override fun text(key: String): String? = this@asDaisyTextSource.text(key)

        override fun textList(key: String): List<String> = this@asDaisyTextSource.textList(key)
    }

public fun DaisyConfigHandle<DaisyTextConfig>.asDaisyTextSource(): DaisyTextSource =
    object : DaisyTextSource {
        override fun text(key: String): String? = current.text(key)

        override fun textList(key: String): List<String> = current.textList(key)
    }

public fun DaisyConfigBundleHandle<DaisyTextConfig>.asDaisyTextSource(): DaisyTextSource =
    object : DaisyTextSource {
        override fun text(key: String): String? = current.text(key)

        override fun textList(key: String): List<String> = current.textList(key)
    }

public fun yamlTextConfigHandle(
    plugin: JavaPlugin,
    name: String = "lang.yml",
): DaisyConfigHandle<DaisyTextConfig> = plugin.yamlConfigHandle(name, daisyTextConfigCodec())

public fun daisyTextConfigCodec(): DaisyConfigCodec<DaisyTextConfig> =
    DaisyConfigCodec { node, _ ->
        DaisyDecodeResult.Success(MapBackedDaisyTextConfig(flattenTextConfig(node)))
    }

public fun mergeTextConfigs(vararg configs: DaisyTextConfig): DaisyTextConfig =
    object : DaisyTextConfig {
        private val merged = configs.toList()

        override fun text(key: String): String? =
            merged
                .asReversed()
                .firstNotNullOfOrNull { it.text(key) }

        override fun textList(key: String): List<String> =
            merged
                .asReversed()
                .firstNotNullOfOrNull { config ->
                    config.textList(key).takeIf { it.isNotEmpty() }
                }
                .orEmpty()
    }

private class MapBackedDaisyTextConfig(
    private val values: Map<String, Any?>,
) : DaisyTextConfig {
    override fun text(key: String): String? = values[key] as? String

    override fun textList(key: String): List<String> = (values[key] as? List<*>)?.mapNotNull { it as? String }.orEmpty()
}

private fun flattenTextConfig(
    node: cat.daisy.config.DaisyConfigNode,
    path: String = "",
): Map<String, Any?> {
    if (node.isNull()) return emptyMap()
    node.asString()?.let { return mapOf(path to it).filterKeys { it.isNotBlank() } }
    if (node.elements().isNotEmpty()) {
        val list = node.elements().mapNotNull { it.asString() }
        return mapOf(path to list).filterKeys { it.isNotBlank() }
    }
    return node.entries().flatMap { (key, child) ->
        flattenTextConfig(child, if (path.isBlank()) key else "$path.$key").entries
    }.associate { it.toPair() }
}
