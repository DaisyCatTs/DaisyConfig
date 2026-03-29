package cat.daisy.config.modules.internal

import cat.daisy.config.DaisyConfigError
import cat.daisy.config.DaisyReloadResult
import cat.daisy.config.daisycore.DaisyTextConfig
import cat.daisy.config.modules.DaisyModuleConfig
import cat.daisy.config.modules.DaisyModuleDefinition
import cat.daisy.config.modules.DaisyModuleHandle
import cat.daisy.config.modules.DaisyModuleRegistry
import cat.daisy.config.managed.DaisyManagedYaml
import cat.daisy.config.managed.DaisyManagedBundleReloadResult
import cat.daisy.config.managed.DaisyManagedReloadResult
import cat.daisy.config.managed.asReloadResult
import cat.daisy.text.DaisyTextSource
import java.io.File

internal class DaisyModuleRegistryImpl(
    private val rootDirectory: File,
    private val resourceLoader: (String) -> String?,
    definitions: List<DaisyModuleDefinition<*>>,
) : DaisyModuleRegistry {
    private val handles: Map<String, DaisyModuleHandle<*>> =
        definitions.associate { definition ->
            moduleId(definition.category, definition.module) to DaisyModuleHandleImpl(definition, rootDirectory, resourceLoader)
        }

    @Suppress("UNCHECKED_CAST")
    override fun <S> require(
        category: String,
        module: String,
    ): DaisyModuleHandle<S> =
        handles[moduleId(category, module)] as? DaisyModuleHandle<S>
            ?: error("Unknown Daisy module '$category/$module'.")

    override fun reloadAll(): DaisyManagedBundleReloadResult<Map<String, Any?>> =
        migrateAll()

    override fun migrateAll(): DaisyManagedBundleReloadResult<Map<String, Any?>> {
        val warnings = mutableListOf<cat.daisy.config.DaisyConfigWarning>()
        val errors = mutableListOf<DaisyConfigError>()
        val reports = mutableListOf<cat.daisy.config.managed.DaisyManagedMigrationReport>()
        val values = linkedMapOf<String, Any?>()
        val previous = handles.mapValuesTo(linkedMapOf()) { (_, handle) -> handle.current }

        handles.forEach { (id, handle) ->
            when (val result = handle.migrate()) {
                is DaisyManagedBundleReloadResult.Success -> {
                    values[id] = result.value
                    warnings += result.warnings
                    reports += result.reports
                }
                is DaisyManagedBundleReloadResult.Failure -> {
                    errors += result.errors
                    reports += result.reports
                }
            }
        }

        return if (errors.isEmpty()) {
            DaisyManagedBundleReloadResult.Success(values, warnings, reports)
        } else {
            DaisyManagedBundleReloadResult.Failure(errors, previous, reports)
        }
    }
}

private class DaisyModuleHandleImpl<S>(
    override val definition: DaisyModuleDefinition<S>,
    private val rootDirectory: File,
    private val resourceLoader: (String) -> String?,
) : DaisyModuleHandle<S> {
    override lateinit var current: DaisyModuleConfig<S>
        private set

    override val textSource: DaisyTextSource =
        object : DaisyTextSource {
            override fun text(key: String): String? = current.lang?.text(key)

            override fun textList(key: String): List<String> = current.lang?.textList(key).orEmpty()
        }

    init {
        when (val result = migrate()) {
            is DaisyManagedBundleReloadResult.Success<*> -> current = result.value as DaisyModuleConfig<S>
            is DaisyManagedBundleReloadResult.Failure<*> -> {
                val message = result.errors.joinToString { "${it.path}: ${it.message}" }
                error("Failed to load module ${definition.category}/${definition.module}: $message")
            }
        }
    }

    override fun reload(): DaisyReloadResult<DaisyModuleConfig<S>> = migrate().asReloadResult()

    @Suppress("UNCHECKED_CAST")
    override fun migrate(): DaisyManagedBundleReloadResult<DaisyModuleConfig<S>> {
        val settingsResult = DaisyManagedYaml.load(rootDirectory, resourceLoader, definition.settings)
        val langResult = definition.lang?.let { DaisyManagedYaml.load(rootDirectory, resourceLoader, it) }

        val reports =
            buildList {
                add(settingsResult.report())
                langResult?.let { add(it.report()) }
            }

        val warnings = mutableListOf<cat.daisy.config.DaisyConfigWarning>()
        val errors = mutableListOf<DaisyConfigError>()

        val settings =
            when (settingsResult) {
                is DaisyManagedReloadResult.Success<*> -> {
                    warnings += settingsResult.warnings
                    settingsResult.value as S
                }
                is DaisyManagedReloadResult.Failure<*> -> {
                    errors += settingsResult.errors
                    null
                }
            }

        val lang =
            when (langResult) {
                null -> null
                is DaisyManagedReloadResult.Success<*> -> {
                    warnings += langResult.warnings
                    langResult.value as DaisyTextConfig
                }
                is DaisyManagedReloadResult.Failure<*> -> {
                    errors += langResult.errors
                    null
                }
            }

        return if (errors.isEmpty() && settings != null) {
            val value: DaisyModuleConfig<S> = DaisyModuleConfig(settings = settings, lang = lang)
            current = value
            DaisyManagedBundleReloadResult.Success<DaisyModuleConfig<S>>(
                value = value,
                warnings = warnings,
                reports = reports,
            )
        } else {
            DaisyManagedBundleReloadResult.Failure<DaisyModuleConfig<S>>(
                errors = errors,
                previousValue = currentOrNull(),
                reports = reports,
            )
        }
    }

    private fun currentOrNull(): DaisyModuleConfig<S>? = if (::current.isInitialized) current else null
}

private fun moduleId(
    category: String,
    module: String,
): String = "$category/$module"

private fun <T> DaisyManagedReloadResult<T>.report(): cat.daisy.config.managed.DaisyManagedMigrationReport =
    when (this) {
        is DaisyManagedReloadResult.Success -> report
        is DaisyManagedReloadResult.Failure -> report
    }
