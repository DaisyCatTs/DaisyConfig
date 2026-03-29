package cat.daisy.config.example

import cat.daisy.command.DaisyCommandGroup
import cat.daisy.command.DaisyCommandSet
import cat.daisy.config.managed.DaisyManagedBundleReloadResult
import cat.daisy.config.managed.DaisyManagedMigrationReport

@DaisyCommandSet
class ProfileConfigCommand(
    private val plugin: DaisyConfigExamplePlugin,
) : DaisyCommandGroup({
    command("moduleconfig") {
        description("Inspect and reload the DaisyConfig Phase 3 module example")

        subcommand("show") {
            player {
                val spawn = plugin.modules.spawn.current.settings
                val warp = plugin.modules.warp.current.settings
                val store = plugin.modules.store.current.settings
                reply("<gradient:#7dd3fc:#c4b5fd>Spawn delay</gradient>: ${spawn.delaySeconds}s")
                reply("<gradient:#7dd3fc:#c4b5fd>Spawn bypass</gradient>: ${spawn.bypassPermission}")
                reply("<gradient:#7dd3fc:#c4b5fd>Warp warmup</gradient>: ${warp.warmupSeconds}s")
                reply("<gradient:#7dd3fc:#c4b5fd>Warp max</gradient>: ${warp.maxWarps}")
                reply("<gradient:#7dd3fc:#c4b5fd>Store icon</gradient>: ${store.icon}")
                reply("<gradient:#7dd3fc:#c4b5fd>Store sound</gradient>: ${store.openSound}")
            }
        }

        subcommand("spawn") {
            player {
                val settings = plugin.modules.spawn.current.settings
                val message =
                    plugin.modules.spawn.textSource.text("messages.spawn.ready")
                        ?.replace("%delay%", settings.delaySeconds.toString())
                        ?: "<green>Spawn ready in ${settings.delaySeconds}s.</green>"
                reply(message)
            }
        }

        subcommand("reload") {
            player {
                when (val result = plugin.modules.reloadAll()) {
                    is DaisyManagedBundleReloadResult.Success -> {
                        reply("<green>Reloaded ${result.value.size} managed module bundles.</green>")
                        result.reports.forEachIndexed { index, report ->
                            reply("<gray>#${index + 1} ${formatReport(report)}</gray>")
                        }
                    }
                    is DaisyManagedBundleReloadResult.Failure -> {
                        reply("<red>Reload failed.</red>")
                        result.errors.forEach { error ->
                            reply("<gray>${error.path}: ${error.message}</gray>")
                        }
                    }
                }
            }
        }

        subcommand("migrate") {
            player {
                when (val result = plugin.modules.migrateAll()) {
                    is DaisyManagedBundleReloadResult.Success -> {
                        reply("<green>Managed migration completed.</green>")
                        result.reports.forEachIndexed { index, report ->
                            reply("<gray>#${index + 1} ${formatReport(report)}</gray>")
                        }
                    }
                    is DaisyManagedBundleReloadResult.Failure -> {
                        reply("<red>Migration failed.</red>")
                        result.errors.forEach { error ->
                            reply("<gray>${error.path}: ${error.message}</gray>")
                        }
                    }
                }
            }
        }
    }
})

private fun formatReport(report: DaisyManagedMigrationReport): String =
    buildString {
        append("version ")
        append(report.versionBefore ?: 1)
        append(" -> ")
        append(report.versionAfter ?: report.versionBefore ?: 1)
        if (report.createdFromDefault) {
            append(", created")
        }
        if (report.mergedMissingKeys.isNotEmpty()) {
            append(", merged ")
            append(report.mergedMissingKeys.size)
            append(" keys")
        }
        if (report.renamedKeys.isNotEmpty()) {
            append(", renamed ")
            append(report.renamedKeys.size)
            append(" keys")
        }
        if (report.removedKeys.isNotEmpty()) {
            append(", removed ")
            append(report.removedKeys.size)
            append(" keys")
        }
    }
