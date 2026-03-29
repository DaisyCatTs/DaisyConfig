# DaisyConfig Migration

## Why Phase 3 exists

Phase 2 removed most plugin-local decode boilerplate.

Phase 3 removes the next layer of plugin-local config plumbing:

- manual `saveResource(...)`
- manual missing-key merge
- manual `config_version` handling
- hardcoded module file maps
- startup/reload migration code
- plugin-local `settings.yml` + `lang.yml` registries

## Replace plugin-local config services

Typical local config services usually own:

- file creation
- reload orchestration
- one-off migration logic
- `modules/<category>/<module>/...` discovery
- lang/settings pairing

Those responsibilities now belong in DaisyConfig.

## Replace manual managed-file lifecycle

Before:

```kotlin
saveResource("settings.yml", false)
val file = File(dataFolder, "settings.yml")
val yaml = YamlConfiguration.loadConfiguration(file)
val version = yaml.getInt("config_version", 1)
// merge defaults
// rename keys
// bump version
// save again
```

After:

```kotlin
val settings =
    DaisyManagedYamlFile(
        id = "settings",
        path = "settings.yml",
        codec = settingsCodec,
        currentVersion = 2,
        migrations = listOf(
            DaisyYamlMigrations.move(1, 2, "spawn-delay", "spawn.delay"),
        ),
    )

val handle = plugin.managedYamlConfigHandle(settings)
```

## Replace hardcoded module maps

Before:

- keep a `Map<String, File>` for module settings
- keep another map for lang files
- reload them manually
- hope one failure does not leave the runtime in a mixed state

After:

```kotlin
val registry =
    DaisyModules.load(plugin) {
        module(
            DaisyModuleDefinition(
                category = "commands",
                module = "spawn",
                settings = spawnSettingsFile,
                lang = spawnLangFile,
            ),
        )
    }
```

Then:

```kotlin
val spawn = registry.require<SpawnSettings>("commands", "spawn")
val settings = spawn.current.settings
val textSource = spawn.textSource
```

## Replace local lang accessors

Before:

```kotlin
fun text(key: String): String? = langYaml.getString(key)
```

After:

```kotlin
val textSource = moduleHandle.textSource
val value = textSource.text("messages.spawn.ready")
```

`DaisyConfig` still does not execute placeholders directly.

## Replace local reload orchestration

Before:

- reload settings
- reload lang
- merge reports yourself
- decide whether runtime state should roll forward

After:

```kotlin
when (val result = registry.reloadAll()) {
    is DaisyManagedBundleReloadResult.Success -> {
        result.reports.forEach { report ->
            logger.info("Merged keys: ${report.mergedMissingKeys}")
        }
    }
    is DaisyManagedBundleReloadResult.Failure -> {
        result.errors.forEach { error ->
            logger.warning("${error.path}: ${error.message}")
        }
    }
}
```

## Replace local version bump logic

Before:

- read `config_version`
- treat missing versions specially
- rename/remove keys by hand
- write the updated version back

After:

```kotlin
val file =
    DaisyManagedYamlFile(
        id = "spawn",
        path = "modules/commands/spawn/settings.yml",
        codec = spawnCodec,
        currentVersion = 2,
        migrations = listOf(
            DaisyYamlMigrations.move(1, 2, "spawn-delay", "spawn.delay"),
        ),
    )
```

Managed YAML now handles:

- missing version treated as `1`
- ordered migration validation
- write-back of the final version key
- migration reporting

## Replace manual default merge logic

Before:

- compare live file against resource defaults manually
- add keys carefully without overwriting user edits

After:

Use the default managed merge policy:

```kotlin
mergePolicy = DaisyYamlMergePolicy.AddMissingKeys
```

This adds missing deep keys from bundled defaults while preserving:

- user overrides
- unknown user keys

## Keep low-level APIs when you need them

Phase 3 does not remove the simple APIs:

- `DaisyYaml.load(...)`
- `DaisyYaml.handle(...)`
- `yamlConfigHandle(...)`
- `yamlConfigBundleHandle(...)`

Use managed YAML when:

- files need migration
- files need missing-key merge
- files follow a module convention
- you want config lifecycle owned by DaisyConfig itself
