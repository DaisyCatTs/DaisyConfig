## [Unreleased]
### Added
- `config-managed` for managed YAML files with lifecycle-aware creation, merge, and migration
- `config-modules` for first-class `settings.yml` + `lang.yml` module bundles
- `DaisyManagedYamlFile`
- `DaisyManagedConfigHandle`
- `DaisyManagedReloadResult`
- `DaisyManagedMigrationReport`
- `DaisyManagedBundleReloadResult`
- `DaisyYamlMigration` and `DaisyYamlMigrations`
- managed `config_version` support with missing-version fallback to version `1`
- automatic missing-key merge that preserves user overrides and unknown keys
- ordered migration chain validation and reporting
- module registry loading for `modules/<category>/<module>/settings.yml`
- module lang support through `DaisyTextSource`
- refreshed example plugin for managed module bundles

### Changed
- Phase 2 low-level YAML APIs remain intact, but managed YAML is now the recommended path for versioned plugin configs
- README and migration docs now document the Phase 3 managed and module workflow

## [0.1.0-SNAPSHOT]
### Added
- DaisyConfig repo skeleton
- config-base
- config-yaml
- config-daisycore
- config-all
- example plugin
- README and migration docs
