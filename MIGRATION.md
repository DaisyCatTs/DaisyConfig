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

## Replace manual nested section traversal

Before:

```kotlin
val section = plugin.config.getConfigurationSection("feedback")
val sound = DaisySounds.parse(section?.getString("sound") ?: "entity_player_levelup")
val message = section?.getString("message") ?: "<green>Saved.</green>"
```

After:

```kotlin
data class FeedbackConfig(
    val sound: Sound,
    val message: String,
)

val feedbackCodec =
    objectCodec {
        FeedbackConfig(
            sound = required("sound", soundCodec()),
            message = defaulted("message", stringCodec(), "<green>Saved.</green>"),
        )
    }

val profileCodec =
    objectCodec {
        ProfileUiConfig(
            icon = required("icon", materialCodec()),
            feedback = section("feedback", feedbackCodec),
        )
    }
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

## Replace ad hoc multi-file reload orchestration

Before:

- reload `profile-ui.yml`
- reload `lang.yml`
- hope your runtime state stays coherent if one succeeds and one fails

After:

```kotlin
val featureBundle =
    plugin.yamlConfigBundleHandle {
        ProfileFeatureConfig(
            ui = file("profile-ui.yml", profileUiCodec),
            layout = file("profile-layout.yml", profileLayoutCodec),
        )
    }
```

`DaisyConfigBundleHandle<T>` gives one logical current value and preserves the previous good state on reload failure.

## Replace scattered validation checks

Before:

- blank checks near command handlers
- row bounds checks near menu builders
- slot bounds checks in ad hoc utility code

After:

```kotlin
val menuCodec =
    objectCodec {
        MenuConfig(
            rows = defaulted("rows", intCodec(), 3),
            profileSlot = defaulted("profile_slot", intCodec(), 13),
        )
    }.validate { config ->
        buildList {
            addAll(DaisyValidation.intRange("menu.rows", config.rows, 1, 6))
            addAll(
                DaisyValidation.require(
                    condition = config.profileSlot in 0 until (config.rows * 9),
                    path = "menu.profile_slot",
                    message = "Slot must be within the menu size.",
                ),
            )
        }
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

For multiple text packs:

```kotlin
val textBundle =
    plugin.yamlConfigBundleHandle {
        mergeTextConfigs(
            file("lang.yml", daisyTextConfigCodec()),
            file("profile-text.yml", daisyTextConfigCodec()),
        )
    }

val textSource = textBundle.asDaisyTextSource()
```

## Replace repetitive first-run resource copying

Before:

```kotlin
ensureDefaultConfigResource("config.yml")
ensureDefaultConfigResource("lang.yml")
ensureDefaultConfigResource("menus.yml")
```

After:

```kotlin
ensureDefaultConfigResources("config.yml", "lang.yml", "menus.yml")
```
