package cat.daisy.config.modules

import cat.daisy.config.modules.internal.DaisyModuleRegistryImpl
import org.bukkit.plugin.java.JavaPlugin

public object DaisyModules {
    public fun load(
        plugin: JavaPlugin,
        block: DaisyModuleRegistryBuilder.() -> Unit,
    ): DaisyModuleRegistry {
        val builder = DaisyModuleRegistryBuilderImpl()
        builder.block()
        return DaisyModuleRegistryImpl(
            rootDirectory = plugin.dataFolder,
            resourceLoader = { resourcePath ->
                plugin.getResource(resourcePath)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            },
            definitions = builder.definitions.toList(),
        )
    }
}

private class DaisyModuleRegistryBuilderImpl : DaisyModuleRegistryBuilder {
    val definitions = mutableListOf<DaisyModuleDefinition<*>>()

    override fun <S> module(definition: DaisyModuleDefinition<S>) {
        definitions += definition
    }
}
