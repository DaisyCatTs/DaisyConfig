# DaisyConfig Migration

## Why DaisyConfig exists

DaisyConfig replaces the plugin-local layer that usually grows around:

- raw Bukkit YAML lookups
- manual nested-section traversal
- fragile reload code
- repeated enum parsing wrappers
- one-off DaisyCore text adapters

## Replace raw config lookups

Before:

```kotlin
val icon = plugin.config.getString("icon") ?: "STONE"
val title = plugin.config.getString("sidebar_title") ?: "Profile"
```

After:

```kotlin
val config = handle.current
val icon = config.icon
val title = config.sidebarTitle
```

## Replace manual enum parsing

Before:

```kotlin
val material = DaisyMaterials.parse(plugin.config.getString("icon") ?: "stone")
```

After:

```kotlin
icon = required("icon", materialCodec())
```

## Replace fragile reload logic

Before:

- reload file
- parse fields manually
- hope the runtime does not end up half-updated

After:

```kotlin
when (val result = handle.reload()) {
    is DaisyReloadResult.Success -> logger.info("Reloaded.")
    is DaisyReloadResult.Failure -> logger.warning(result.errors.joinToString { "${it.path}: ${it.message}" })
}
```

## Replace ad hoc DaisyCore text adapters

Before:

- plugin-local `DaisyTextSource`
- manual flattening
- repeated `getStringList(...)`

After:

```kotlin
val langHandle = yamlTextConfigHandle(plugin, "lang.yml")
val textSource = langHandle.current.asDaisyTextSource()
```

