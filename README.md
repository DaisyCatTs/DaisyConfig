# DaisyConfig

Typed YAML-first config loading for modern Paper plugins, with explicit codecs, reload-safe handles, nested sections, and DaisyCore-ready text packs.

## Why DaisyConfig

DaisyConfig removes the plugin-local config boilerplate that usually grows around:

- `getString(...)` and `getInt(...)` sprawl
- nested section traversal
- reload handling
- validation
- config-backed DaisyCore text wiring
- DaisySeries-backed typed parsing from config

## What it solves

- typed config mapping
- reload-safe config handles
- clear validation failures
- config-backed text sources for DaisyCore
- DaisySeries-aware config codecs

## Installation

```kotlin
repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
}

dependencies {
    implementation("cat.daisy:DaisyConfig:0.1.0-SNAPSHOT")
}
```

## Quick example

```kotlin
data class ProfileUiConfig(
    val icon: Material,
    val feedback: FeedbackConfig,
)

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

val profileUiCodec =
    objectCodec {
        ProfileUiConfig(
            icon = required("icon", materialCodec()),
            feedback = section("feedback", feedbackCodec),
        )
    }

val handle = DaisyYaml.handle(File(dataFolder, "profile-ui.yml"), profileUiCodec)
val config = handle.current
config.feedback.message
```

## Reload-safe handles

`DaisyConfigHandle<T>` always exposes the last good config through `current`.

If reload fails:
- the previous valid value stays live
- errors are returned
- runtime state does not switch to a half-decoded config

## Nested sections and validation

`DaisyFieldReader` now supports explicit section mapping:

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

Available section helpers:

- `section(...)`
- `optionalSection(...)`
- `defaultedSection(...)`

Validation helpers:

- `DaisyValidation.require(...)`
- `DaisyValidation.notBlank(...)`
- `DaisyValidation.intRange(...)`

## Multi-file config bundles

For real plugins, you usually own more than one file. DaisyConfig Phase 2 adds bundle orchestration without inventing a second config architecture.

```kotlin
val featureBundle =
    plugin.yamlConfigBundleHandle {
        ProfileFeatureConfig(
            ui = file("profile-ui.yml", profileUiCodec),
            layout = file("profile-layout.yml", profileLayoutCodec),
        )
    }
```

`DaisyConfigBundleHandle<T>` keeps one logical current value and preserves the last good state if a bundle reload fails.

## DaisySeries integration

DaisyConfig includes first-class codecs for:

- materials
- sounds
- item flags
- enchantments
- potion effects

These codecs use DaisySeries rather than rebuilding parsing logic locally.

## DaisyCore text bridge

`config-daisycore` provides:

- `DaisyTextConfig`
- `asDaisyTextSource()`
- YAML-backed text config handles

This is the clean path for config-backed DaisyCore messages, menus, sidebars, and tablists.

DaisyConfig stores and loads text. DaisyCore renders it.

You can also merge text packs and keep them live through bundle handles:

```kotlin
val textBundle =
    plugin.yamlConfigBundleHandle {
        mergeTextConfigs(
            file("lang.yml", daisyTextConfigCodec()),
            file("profile-text.yml", daisyTextConfigCodec()),
        )
    }

messages(textBundle.asDaisyTextSource())
```

## Placeholder safety model

DaisyConfig does **not** expand PlaceholderAPI directly.

It only provides typed values and text-source data.
If placeholder-aware rendering happens, it happens through DaisyCore's existing viewer-aware rendering model.

## Default resources

Phase 2 also adds a bulk helper for first-run plugin setup:

```kotlin
ensureDefaultConfigResources(
    "profile-ui.yml",
    "profile-layout.yml",
    "lang.yml",
    "profile-text.yml",
)
```

## IntelliJ Setup

- Open the repo as a Gradle project in IntelliJ IDEA.
- Use the checked-in Gradle wrapper.
- Recommended JDK: Java 21.
- Useful runs:

```bash
./gradlew.bat --no-daemon test
./gradlew.bat --no-daemon :example-plugin:compileKotlin
```

## Example plugin

See [`example-plugin`](./example-plugin) for a Paper example using:

- nested config sections
- bundle reloads across multiple files
- merged text packs
- DaisySeries codecs
- DaisyCore menu/sidebar/tablist rendering

## Changelog and Migration

- [CHANGELOG.md](./CHANGELOG.md)
- [MIGRATION.md](./MIGRATION.md)

## License

MIT
