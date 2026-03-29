package cat.daisy.config.yaml.internal

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.io.StringReader

public object DaisyMutableYamlSupport {
    public fun load(file: File): LinkedHashMap<String, Any?> {
        if (!file.exists()) {
            return linkedMapOf()
        }
        return loadConfiguration(YamlConfiguration.loadConfiguration(file))
    }

    public fun load(text: String): LinkedHashMap<String, Any?> {
        if (text.isBlank()) {
            return linkedMapOf()
        }
        return loadConfiguration(YamlConfiguration.loadConfiguration(StringReader(text)))
    }

    public fun save(
        file: File,
        root: Map<String, Any?>,
    ) {
        val yaml = YamlConfiguration()
        applyMap(yaml, root)
        file.parentFile?.mkdirs()
        yaml.save(file)
    }

    public fun deepCopy(value: Any?): Any? =
        when (value) {
            is Map<*, *> ->
                linkedMapOf<String, Any?>().apply {
                    value.forEach { (key, child) ->
                        this[key.toString()] = deepCopy(child)
                    }
                }
            is List<*> -> value.map(::deepCopy)
            else -> value
        }

    public fun flattenLeaves(
        root: Map<String, Any?>,
        path: String = "",
    ): Map<String, Any?> {
        val flattened = linkedMapOf<String, Any?>()
        root.forEach { (key, value) ->
            val childPath = if (path.isBlank()) key else "$path.$key"
            when (value) {
                is Map<*, *> -> flattened.putAll(flattenLeaves(value.entries.associate { it.key.toString() to it.value }, childPath))
                else -> flattened[childPath] = deepCopy(value)
            }
        }
        return flattened
    }

    private fun loadConfiguration(configuration: YamlConfiguration): LinkedHashMap<String, Any?> =
        configuration.getKeys(false).associateTo(linkedMapOf()) { key ->
            key to normalize(configuration.get(key))
        }

    private fun normalize(value: Any?): Any? =
        when (value) {
            is ConfigurationSection ->
                value.getKeys(false).associateTo(linkedMapOf()) { key ->
                    key to normalize(value.get(key))
                }
            is Map<*, *> ->
                value.entries.associateTo(linkedMapOf()) { entry ->
                    entry.key.toString() to normalize(entry.value)
                }
            is List<*> -> value.map(::normalize)
            else -> value
        }

    private fun applyMap(
        target: ConfigurationSection,
        root: Map<String, Any?>,
    ) {
        root.forEach { (key, value) ->
            when (value) {
                is Map<*, *> -> {
                    val section = target.createSection(key)
                    applyMap(
                        section,
                        value.entries.associate { entry ->
                            entry.key.toString() to sanitize(entry.value)
                        },
                    )
                }
                else -> target.set(key, sanitize(value))
            }
        }
    }

    private fun sanitize(value: Any?): Any? =
        when (value) {
            is Map<*, *> ->
                value.entries.associate { entry ->
                    entry.key.toString() to sanitize(entry.value)
                }
            is List<*> -> value.map(::sanitize)
            else -> value
        }
}
