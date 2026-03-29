# DaisyConfig

Typed YAML-first config loading for modern Paper plugins.

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
    val sidebarTitle: String,
)

val profileUiCodec =
    objectCodec {
        ProfileUiConfig(
            icon = required("icon", materialCodec()),
            sidebarTitle = defaulted("sidebar_title", stringCodec(), "<gradient:#7dd3fc:#c4b5fd>Profile</gradient>"),
        )
    }

val handle = DaisyYaml.handle(File(dataFolder, "profile-ui.yml"), profileUiCodec)
val config = handle.current
```

## Reload-safe handles

`DaisyConfigHandle<T>` always exposes the last good config through `current`.

If reload fails:
- the previous valid value stays live
- errors are returned
- runtime state does not switch to a half-decoded config

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

## Placeholder safety model

DaisyConfig does **not** expand PlaceholderAPI directly.

It only provides typed values and text-source data.
If placeholder-aware rendering happens, it happens through DaisyCore's existing viewer-aware rendering model.

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

See [`example-plugin`](./example-plugin) for a Paper example using DaisyConfig, DaisySeries, and DaisyCore together.

## Changelog and Migration

- [CHANGELOG.md](./CHANGELOG.md)
- [MIGRATION.md](./MIGRATION.md)

## License

MIT

