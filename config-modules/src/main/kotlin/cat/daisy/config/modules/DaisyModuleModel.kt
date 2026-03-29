package cat.daisy.config.modules

import cat.daisy.config.DaisyConfigBundleHandle
import cat.daisy.config.daisycore.DaisyTextConfig
import cat.daisy.config.managed.DaisyManagedBundleReloadResult
import cat.daisy.config.managed.DaisyManagedYamlFile
import cat.daisy.text.DaisyTextSource

public data class DaisyModuleDefinition<S>(
    val category: String,
    val module: String,
    val settings: DaisyManagedYamlFile<S>,
    val lang: DaisyManagedYamlFile<DaisyTextConfig>? = null,
)

public data class DaisyModuleConfig<S>(
    val settings: S,
    val lang: DaisyTextConfig?,
)

public interface DaisyModuleHandle<S> : DaisyConfigBundleHandle<DaisyModuleConfig<S>> {
    public val definition: DaisyModuleDefinition<S>
    public val textSource: DaisyTextSource

    public fun migrate(): DaisyManagedBundleReloadResult<DaisyModuleConfig<S>>
}

public interface DaisyModuleRegistry {
    public fun <S> require(
        category: String,
        module: String,
    ): DaisyModuleHandle<S>

    public fun reloadAll(): DaisyManagedBundleReloadResult<Map<String, Any?>>

    public fun migrateAll(): DaisyManagedBundleReloadResult<Map<String, Any?>>
}

public interface DaisyModuleRegistryBuilder {
    public fun <S> module(definition: DaisyModuleDefinition<S>)
}
