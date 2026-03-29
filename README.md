# DaisyConfig

Typed YAML-first configuration for modern Paper plugins.

DaisyConfig gives you:

- explicit codecs instead of ad hoc `getString(...)` trees
- reload-safe handles that keep the last good runtime value live
- DaisySeries-backed config parsing for materials, sounds, item flags, enchantments, and potions
- DaisyCore-ready text config adapters through `DaisyTextSource`
- managed YAML files with versioning, missing-key merge, and ordered migrations
- first-class module bundles for `settings.yml` + `lang.yml`

## Modules

Phase 3 ships these modules:

- `config-base`
- `config-yaml`
- `config-managed`
- `config-modules`
- `config-daisycore`
- `config-all`
- `example-plugin`

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

## Unmanaged YAML still exists

The low-level YAML API remains available for simple files:

```kotlin
val handle = plugin.yamlConfigHandle("settings.yml", settingsCodec)
val result = DaisyYaml.load(File(dataFolder, "settings.yml"), settingsCodec)
```

Bundle handles also still exist:

```kotlin
val bundle =
    plugin.yamlConfigBundleHandle {
        FeatureBundle(
            settings = file("settings.yml", settingsCodec),
            lang = file("lang.yml", daisyTextConfigCodec()),
        )
    }
```

## Managed YAML

Managed YAML adds file lifecycle, version-aware migration, and missing-key merge on top of the unmanaged layer.

```kotlin
val settingsFile =
    DaisyManagedYamlFile(
        id = "settings",
        path = "settings.yml",
        codec = settingsCodec,
        currentVersion = 2,
        migrations = listOf(
            DaisyYamlMigrations.move(1, 2, "spawn-delay", "spawn.delay"),
        ),
    )

val handle = plugin.managedYamlConfigHandle(settingsFile)
val config = handle.current
```

Managed lifecycle:

1. create missing files from bundled defaults
2. merge missing default keys without overwriting user values
3. treat missing `config_version` as version `1`
4. run ordered migrations up to `currentVersion`
5. write changes to disk only when something actually changed
6. decode into a typed runtime value

If reload fails, the previous typed value stays live.

## Managed reload results

Managed handles return a report that explains what changed:

```kotlin
when (val result = handle.migrate()) {
    is DaisyManagedReloadResult.Success -> {
        logger.info("Merged keys: ${result.report.mergedMissingKeys}")
        logger.info("Renamed keys: ${result.report.renamedKeys}")
    }
    is DaisyManagedReloadResult.Failure -> {
        result.errors.forEach { error ->
            logger.warning("${error.path}: ${error.message}")
        }
    }
}
```

## Migration helpers

Phase 3 includes helper migrations for common YAML changes:

- `DaisyYamlMigrations.rename(...)`
- `DaisyYamlMigrations.move(...)`
- `DaisyYamlMigrations.remove(...)`
- `DaisyYamlMigrations.setDefault(...)`

Migration chains must be ordered one version at a time. Skipped or duplicate chains are rejected.

## Module bundles

`config-modules` gives you first-class module conventions for:

- `modules/<category>/<module>/settings.yml`
- `modules/<category>/<module>/lang.yml`

```kotlin
val registry =
    DaisyModules.load(plugin) {
        module(
            DaisyModuleDefinition(
                category = "commands",
                module = "spawn",
                settings =
                    DaisyManagedYamlFile(
                        id = "commands/spawn/settings",
                        path = "modules/commands/spawn/settings.yml",
                        codec = spawnSettingsCodec,
                        currentVersion = 2,
                        migrations = listOf(
                            DaisyYamlMigrations.move(1, 2, "spawn-delay", "spawn.delay"),
                        ),
                    ),
                lang =
                    DaisyManagedYamlFile(
                        id = "commands/spawn/lang",
                        path = "modules/commands/spawn/lang.yml",
                        codec = daisyTextConfigCodec(),
                    ),
            ),
        )
    }

val spawn = registry.require<SpawnSettings>("commands", "spawn")
val delay = spawn.current.settings.delaySeconds
val textSource = spawn.textSource
```

Module handles preserve bundle safety:

- typed settings and lang reload together
- the last good module value stays live if reload fails
- migration reports are aggregated across both files

## DaisyCore text bridge

`config-daisycore` stays focused on the text bridge:

- `DaisyTextConfig`
- `daisyTextConfigCodec()`
- `asDaisyTextSource()`

DaisyConfig stores and loads data. DaisyCore still owns runtime rendering.

DaisyConfig does **not** execute placeholders directly.

## Lifesteal-style module layout

Phase 3 is designed to replace plugin-local config services that manually manage:

- `settings.yml`
- `lang.yml`
- `modules/<category>/<module>/...`
- `saveResource(...)`
- version bumps
- missing-key merge logic

That workflow now lives in DaisyConfig itself.

## Example plugin

See [`example-plugin`](./example-plugin) for a managed module example with:

- `commands/spawn`
- `commands/warp`
- `guis/store`
- startup creation from bundled defaults
- managed reload and migration reporting
- DaisySeries-backed typed settings
- DaisyCore-facing module text sources

## Tooling

Recommended JDK: Java 21

Useful commands:

```bash
./gradlew.bat --no-daemon test
./gradlew.bat --no-daemon :example-plugin:compileKotlin
```

## Changelog and Migration

- [CHANGELOG.md](./CHANGELOG.md)
- [MIGRATION.md](./MIGRATION.md)

## License

MIT
