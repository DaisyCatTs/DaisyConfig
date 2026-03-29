package cat.daisy.config.yaml

import cat.daisy.config.DaisyConfigBundleHandle
import cat.daisy.config.DaisyConfigCodec
import cat.daisy.config.DaisyConfigError
import cat.daisy.config.DaisyReloadResult
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

public interface DaisyYamlBundleReader {
    public fun <T> file(
        name: String,
        codec: DaisyConfigCodec<T>,
    ): T
}

public object DaisyYamlBundle {
    public fun <T> load(
        directory: File,
        factory: DaisyYamlBundleReader.() -> T,
    ): DaisyReloadResult<T> = DaisyYamlBundleLoader(directory, factory).load()

    public fun <T> handle(
        directory: File,
        factory: DaisyYamlBundleReader.() -> T,
    ): DaisyConfigBundleHandle<T> {
        val initial = load(directory, factory)
        require(initial is DaisyReloadResult.Success<T>) {
            val errors = (initial as DaisyReloadResult.Failure).errors.joinToString { "${it.path}: ${it.message}" }
            "Failed to load config bundle from ${directory.path}: $errors"
        }
        return DaisyYamlBundleHandle(directory, factory, initial.value)
    }
}

private class DaisyYamlBundleHandle<T>(
    private val directory: File,
    private val factory: DaisyYamlBundleReader.() -> T,
    currentValue: T,
) : DaisyConfigBundleHandle<T> {
    override var current: T = currentValue
        private set

    override fun reload(): DaisyReloadResult<T> {
        val result = DaisyYamlBundle.load(directory, factory)
        return when (result) {
            is DaisyReloadResult.Success -> {
                current = result.value
                result
            }
            is DaisyReloadResult.Failure -> DaisyReloadResult.Failure(result.errors, current)
        }
    }
}

private class DaisyYamlBundleLoader<T>(
    private val directory: File,
    private val factory: DaisyYamlBundleReader.() -> T,
) : DaisyYamlBundleReader {
    private val values = linkedMapOf<String, Any?>()
    private val warnings = mutableListOf<cat.daisy.config.DaisyConfigWarning>()
    private val errors = mutableListOf<DaisyConfigError>()

    @Suppress("UNCHECKED_CAST")
    override fun <T> file(
        name: String,
        codec: DaisyConfigCodec<T>,
    ): T {
        if (values.containsKey(name)) {
            return values.getValue(name) as T
        }

        val file = File(directory, name)
        val result = DaisyYaml.load(file, codec)
        return when (result) {
            is DaisyReloadResult.Success -> {
                warnings += result.warnings
                values[name] = result.value
                result.value
            }
            is DaisyReloadResult.Failure -> {
                errors += result.errors.prefixBundleFile(name)
                throw DaisyYamlBundleAbort
            }
        }
    }

    fun load(): DaisyReloadResult<T> =
        try {
            val value = factory()
            if (errors.isEmpty()) {
                DaisyReloadResult.Success(value, warnings)
            } else {
                DaisyReloadResult.Failure(errors, null)
            }
        } catch (_: DaisyYamlBundleAbort) {
            DaisyReloadResult.Failure(errors, null)
        }
}

private data object DaisyYamlBundleAbort : RuntimeException()

private fun List<DaisyConfigError>.prefixBundleFile(name: String): List<DaisyConfigError> =
    map { error ->
        val path = if (error.path.isBlank()) name else "$name.${error.path}"
        error.copy(path = path)
    }

public fun <T> JavaPlugin.yamlConfigBundleHandle(factory: DaisyYamlBundleReader.() -> T): DaisyConfigBundleHandle<T> =
    DaisyYamlBundle.handle(dataFolder, factory)
