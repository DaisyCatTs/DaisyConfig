package cat.daisy.config.managed

import cat.daisy.config.DaisyReloadResult
import cat.daisy.config.managed.internal.ManagedYamlEngine
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

public object DaisyManagedYaml {
    public fun <T> load(
        plugin: JavaPlugin,
        file: DaisyManagedYamlFile<T>,
    ): DaisyManagedReloadResult<T> = engine(plugin).load(file).result

    public fun <T> handle(
        plugin: JavaPlugin,
        file: DaisyManagedYamlFile<T>,
    ): DaisyManagedConfigHandle<T> {
        val engine = engine(plugin)
        val initial = engine.load(file)
        require(initial.result is DaisyManagedReloadResult.Success<T>) {
            val errors = (initial.result as DaisyManagedReloadResult.Failure).errors.joinToString { "${it.path}: ${it.message}" }
            "Failed to load managed config ${file.path}: $errors"
        }
        return DaisyManagedYamlHandle(
            engine = engine,
            file = file,
            currentValue = (initial.result as DaisyManagedReloadResult.Success).value,
            currentMetadata = initial.metadata,
        )
    }

    public fun <T> load(
        rootDirectory: File,
        resourceLoader: (String) -> String?,
        file: DaisyManagedYamlFile<T>,
    ): DaisyManagedReloadResult<T> = ManagedYamlEngine(rootDirectory, resourceLoader).load(file).result

    public fun <T> handle(
        rootDirectory: File,
        resourceLoader: (String) -> String?,
        file: DaisyManagedYamlFile<T>,
    ): DaisyManagedConfigHandle<T> {
        val engine = ManagedYamlEngine(rootDirectory, resourceLoader)
        val initial = engine.load(file)
        require(initial.result is DaisyManagedReloadResult.Success<T>) {
            val errors = (initial.result as DaisyManagedReloadResult.Failure).errors.joinToString { "${it.path}: ${it.message}" }
            "Failed to load managed config ${file.path}: $errors"
        }
        return DaisyManagedYamlHandle(
            engine = engine,
            file = file,
            currentValue = (initial.result as DaisyManagedReloadResult.Success).value,
            currentMetadata = initial.metadata,
        )
    }

    private fun engine(plugin: JavaPlugin): ManagedYamlEngine =
        ManagedYamlEngine(plugin.dataFolder) { resourcePath ->
            plugin.getResource(resourcePath)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
        }
}

private class DaisyManagedYamlHandle<T>(
    private val engine: ManagedYamlEngine,
    override val file: DaisyManagedYamlFile<T>,
    currentValue: T,
    currentMetadata: DaisyManagedConfigMetadata,
) : DaisyManagedConfigHandle<T> {
    override var current: T = currentValue
        private set

    override var metadata: DaisyManagedConfigMetadata = currentMetadata
        private set

    override fun reload(): DaisyReloadResult<T> = migrate().asReloadResult()

    override fun migrate(): DaisyManagedReloadResult<T> {
        val outcome = engine.load(file, current)
        metadata = outcome.metadata
        return when (val result = outcome.result) {
            is DaisyManagedReloadResult.Success -> {
                current = result.value
                result
            }
            is DaisyManagedReloadResult.Failure -> DaisyManagedReloadResult.Failure(result.errors, current, result.report)
        }
    }
}

public fun <T> JavaPlugin.managedYamlConfigHandle(file: DaisyManagedYamlFile<T>): DaisyManagedConfigHandle<T> =
    DaisyManagedYaml.handle(this, file)
