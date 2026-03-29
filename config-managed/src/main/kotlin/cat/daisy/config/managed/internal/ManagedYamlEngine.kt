package cat.daisy.config.managed.internal

import cat.daisy.config.DaisyConfigError
import cat.daisy.config.DaisyConfigWarning
import cat.daisy.config.managed.DaisyManagedConfigMetadata
import cat.daisy.config.managed.DaisyManagedMigrationReport
import cat.daisy.config.managed.DaisyManagedReloadResult
import cat.daisy.config.managed.DaisyManagedYamlFile
import cat.daisy.config.managed.DaisyMutableYamlNode
import cat.daisy.config.managed.DaisyYamlMergePolicy
import cat.daisy.config.managed.DaisyYamlMigration
import cat.daisy.config.yaml.DaisyYaml
import cat.daisy.config.yaml.internal.DaisyMutableYamlSupport
import java.io.File

internal data class ManagedLoadOutcome<T>(
    val result: DaisyManagedReloadResult<T>,
    val metadata: DaisyManagedConfigMetadata,
)

internal class ManagedYamlEngine(
    private val rootDirectory: File,
    private val resourceLoader: (String) -> String?,
) {
    fun <T> load(
        file: DaisyManagedYamlFile<T>,
        previousValue: T? = null,
    ): ManagedLoadOutcome<T> {
        val target = File(rootDirectory, file.path)
        target.parentFile?.mkdirs()

        val rawResource = resourceLoader(file.resourcePath)
        var createdFromDefault = false
        var changed = false

        if (!target.exists()) {
            if (rawResource == null) {
                return failure(
                    file = file,
                    previousValue = previousValue,
                    report = DaisyManagedMigrationReport(versionAfter = null),
                    errors = listOf(
                        DaisyConfigError(
                            path = file.path,
                            message = "Bundled resource '${file.resourcePath}' was not found.",
                        ),
                    ),
                    diskVersion = null,
                )
            }
            target.writeText(rawResource)
            createdFromDefault = true
            changed = true
        }

        val diskTree = DaisyMutableYamlSupport.load(target)
        val rootNode = MutableYamlNode(diskTree)
        val defaultTree =
            when {
                rawResource.isNullOrBlank() -> linkedMapOf()
                else -> DaisyYaml.mutableTree(rawResource)
            }

        val rawDiskVersion = rootNode.get(file.versionKey).asInt()
        val diskVersion = rawDiskVersion ?: 1

        val mergedKeys =
            if (file.mergePolicy == DaisyYamlMergePolicy.AddMissingKeys && defaultTree.isNotEmpty()) {
                val merged = mergeMissing(rootNode, defaultTree)
                if (merged.isNotEmpty()) {
                    changed = true
                }
                merged
            } else {
                emptyList()
            }

        val migrationWarnings = mutableListOf<DaisyConfigWarning>()
        val renamedKeys = mutableListOf<Pair<String, String>>()
        val removedKeys = mutableListOf<String>()

        val migrations = validateChain(file, diskVersion)
        if (migrations is ChainValidation.Failure) {
            return failure(file, previousValue, DaisyManagedMigrationReport(versionBefore = rawDiskVersion), migrations.errors, rawDiskVersion)
        }

        var versionAfter = rawDiskVersion
        (migrations as ChainValidation.Success).chain.forEach { migration ->
            val result = migration.apply(rootNode)
            migrationWarnings += result.warnings
            renamedKeys += result.renamedKeys
            removedKeys += result.removedKeys
            changed = true
            versionAfter = migration.toVersion
        }

        val desiredVersion = file.currentVersion
        if (rootNode.get(file.versionKey).asInt() != desiredVersion) {
            rootNode.set(file.versionKey, desiredVersion)
            changed = true
            versionAfter = desiredVersion
        } else if (versionAfter == null) {
            versionAfter = desiredVersion
        }

        if (changed) {
            DaisyYaml.writeTree(target, rootNode.asMap())
        }

        val report =
            DaisyManagedMigrationReport(
                createdFromDefault = createdFromDefault,
                mergedMissingKeys = mergedKeys,
                removedKeys = removedKeys,
                renamedKeys = renamedKeys,
                versionBefore = rawDiskVersion,
                versionAfter = versionAfter,
                wroteToDisk = changed,
            )

        return when (val decoded = DaisyYaml.decode(rootNode.asMap(), file.codec)) {
            is cat.daisy.config.DaisyReloadResult.Success ->
                ManagedLoadOutcome(
                    result = DaisyManagedReloadResult.Success(decoded.value, decoded.warnings + migrationWarnings, report),
                    metadata =
                        DaisyManagedConfigMetadata(
                            path = file.path,
                            versionKey = file.versionKey,
                            currentVersion = file.currentVersion,
                            diskVersion = rawDiskVersion,
                        ),
                )

            is cat.daisy.config.DaisyReloadResult.Failure ->
                ManagedLoadOutcome(
                    result = DaisyManagedReloadResult.Failure(decoded.errors, previousValue, report),
                    metadata =
                        DaisyManagedConfigMetadata(
                            path = file.path,
                            versionKey = file.versionKey,
                            currentVersion = file.currentVersion,
                            diskVersion = rawDiskVersion,
                        ),
                )
        }
    }

    private fun <T> failure(
        file: DaisyManagedYamlFile<T>,
        previousValue: T?,
        report: DaisyManagedMigrationReport,
        errors: List<DaisyConfigError>,
        diskVersion: Int?,
    ): ManagedLoadOutcome<T> =
        ManagedLoadOutcome(
            result = DaisyManagedReloadResult.Failure(errors, previousValue, report),
            metadata =
                DaisyManagedConfigMetadata(
                    path = file.path,
                    versionKey = file.versionKey,
                    currentVersion = file.currentVersion,
                    diskVersion = diskVersion,
                ),
        )

    private fun mergeMissing(
        rootNode: MutableYamlNode,
        defaults: Map<String, Any?>,
    ): List<String> {
        val merged = mutableListOf<String>()
        DaisyMutableYamlSupport.flattenLeaves(defaults).forEach { (path, value) ->
            if (!rootNode.contains(path)) {
                rootNode.set(path, DaisyMutableYamlSupport.deepCopy(value))
                merged += path
            }
        }
        return merged
    }

    private fun <T> validateChain(
        file: DaisyManagedYamlFile<T>,
        startVersion: Int,
    ): ChainValidation {
        if (startVersion > file.currentVersion) {
            return ChainValidation.Failure(
                listOf(
                    DaisyConfigError(
                        path = file.path,
                        message = "Disk version $startVersion is newer than supported version ${file.currentVersion}.",
                    ),
                ),
            )
        }

        val duplicates =
            file.migrations
                .groupBy { it.fromVersion }
                .filterValues { it.size > 1 }
                .keys

        if (duplicates.isNotEmpty()) {
            return ChainValidation.Failure(
                duplicates.map { version ->
                    DaisyConfigError(file.path, "Duplicate migration starting at version $version.")
                },
            )
        }

        val invalidRanges =
            file.migrations
                .filter { it.toVersion <= it.fromVersion || it.toVersion != it.fromVersion + 1 }
                .map { migration ->
                    DaisyConfigError(
                        file.path,
                        "Invalid migration ${migration.fromVersion} -> ${migration.toVersion}; migrations must advance by exactly one version.",
                    )
                }

        if (invalidRanges.isNotEmpty()) {
            return ChainValidation.Failure(invalidRanges)
        }

        val migrationsByFrom = file.migrations.associateBy { it.fromVersion }
        val chain = mutableListOf<DaisyYamlMigration>()
        var expected = startVersion
        while (expected < file.currentVersion) {
            val migration = migrationsByFrom[expected]
                ?: return ChainValidation.Failure(
                    listOf(
                        DaisyConfigError(
                            file.path,
                            "No migration path from version $expected to ${file.currentVersion}.",
                        ),
                    ),
                )
            chain += migration
            expected = migration.toVersion
        }
        return ChainValidation.Success(chain)
    }
}

private sealed interface ChainValidation {
    data class Success(
        val chain: List<DaisyYamlMigration>,
    ) : ChainValidation

    data class Failure(
        val errors: List<DaisyConfigError>,
    ) : ChainValidation
}

private class MutableYamlNode(
    private val root: MutableMap<String, Any?>,
    private val path: List<String> = emptyList(),
) : DaisyMutableYamlNode {
    override fun isNull(): Boolean = resolve() == null

    override fun get(path: String): DaisyMutableYamlNode =
        MutableYamlNode(root, this.path + path.segments())

    override fun set(
        path: String,
        value: Any?,
    ) {
        setSegments(this.path + path.segments(), DaisyMutableYamlSupport.deepCopy(value))
    }

    override fun remove(path: String): Boolean = removeSegments(this.path + path.segments())

    override fun contains(path: String): Boolean = resolve(this.path + path.segments()).resolved

    override fun asString(): String? = resolve() as? String

    override fun asInt(): Int? =
        when (val value = resolve()) {
            is Int -> value
            is Number -> value.toInt()
            else -> null
        }

    override fun asBoolean(): Boolean? = resolve() as? Boolean

    override fun keys(deep: Boolean): Set<String> {
        val value = resolve()
        val map =
            when (value) {
                is Map<*, *> -> value.entries.associate { it.key.toString() to it.value }
                else -> return emptySet()
            }
        if (!deep) {
            return map.keys
        }
        return DaisyMutableYamlSupport.flattenLeaves(map).keys
    }

    override fun asMap(): Map<String, Any?> =
        when (val value = resolve()) {
            is Map<*, *> ->
                value.entries.associateTo(linkedMapOf()) { entry ->
                    entry.key.toString() to DaisyMutableYamlSupport.deepCopy(entry.value)
                }
            else -> emptyMap()
        }

    private fun resolve(): Any? = resolve(path).value

    private fun resolve(segments: List<String>): Resolution {
        var current: Any? = root
        segments.forEach { segment ->
            current =
                when (current) {
                    is Map<*, *> ->
                        if (current.containsKey(segment)) {
                            current[segment]
                        } else {
                            return Resolution(false, null)
                        }
                    else -> return Resolution(false, null)
                }
        }
        return Resolution(true, current)
    }

    private fun setSegments(
        segments: List<String>,
        value: Any?,
    ) {
        require(segments.isNotEmpty()) { "Path cannot be blank." }
        var current: MutableMap<String, Any?> = root
        segments.dropLast(1).forEach { segment ->
            val next =
                when (val existing = current[segment]) {
                    is MutableMap<*, *> -> existing as MutableMap<String, Any?>
                    is Map<*, *> -> existing.entries.associateTo(linkedMapOf()) { entry -> entry.key.toString() to DaisyMutableYamlSupport.deepCopy(entry.value) }
                    else -> linkedMapOf()
                }
            current[segment] = next
            current = next
        }
        current[segments.last()] = value
    }

    private fun removeSegments(segments: List<String>): Boolean {
        require(segments.isNotEmpty()) { "Path cannot be blank." }
        var current: MutableMap<String, Any?> = root
        segments.dropLast(1).forEach { segment ->
            val next = current[segment] as? MutableMap<String, Any?> ?: (current[segment] as? Map<*, *>)?.entries?.associateTo(linkedMapOf()) { entry ->
                entry.key.toString() to DaisyMutableYamlSupport.deepCopy(entry.value)
            } ?: return false
            current[segment] = next
            current = next
        }
        return current.remove(segments.last()) != null
    }
}

private data class Resolution(
    val resolved: Boolean,
    val value: Any?,
)

private fun String.segments(): List<String> = split('.').filter { it.isNotBlank() }
