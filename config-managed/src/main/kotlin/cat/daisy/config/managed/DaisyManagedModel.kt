package cat.daisy.config.managed

import cat.daisy.config.DaisyConfigCodec
import cat.daisy.config.DaisyConfigBundleHandle
import cat.daisy.config.DaisyConfigError
import cat.daisy.config.DaisyConfigHandle
import cat.daisy.config.DaisyConfigWarning
import cat.daisy.config.DaisyReloadResult

public data class DaisyManagedYamlFile<T>(
    val id: String,
    val path: String,
    val resourcePath: String = path,
    val codec: DaisyConfigCodec<T>,
    val versionKey: String = "config_version",
    val currentVersion: Int = 1,
    val migrations: List<DaisyYamlMigration> = emptyList(),
    val mergePolicy: DaisyYamlMergePolicy = DaisyYamlMergePolicy.AddMissingKeys,
)

public interface DaisyManagedConfigHandle<T> : DaisyConfigHandle<T> {
    public val file: DaisyManagedYamlFile<T>
    public val metadata: DaisyManagedConfigMetadata

    public fun migrate(): DaisyManagedReloadResult<T>
}

public data class DaisyManagedConfigMetadata(
    val path: String,
    val versionKey: String,
    val currentVersion: Int,
    val diskVersion: Int?,
)

public sealed interface DaisyManagedReloadResult<T> {
    public data class Success<T>(
        val value: T,
        val warnings: List<DaisyConfigWarning> = emptyList(),
        val report: DaisyManagedMigrationReport,
    ) : DaisyManagedReloadResult<T>

    public data class Failure<T>(
        val errors: List<DaisyConfigError>,
        val previousValue: T?,
        val report: DaisyManagedMigrationReport,
    ) : DaisyManagedReloadResult<T>
}

public data class DaisyManagedMigrationReport(
    val createdFromDefault: Boolean = false,
    val mergedMissingKeys: List<String> = emptyList(),
    val removedKeys: List<String> = emptyList(),
    val renamedKeys: List<Pair<String, String>> = emptyList(),
    val versionBefore: Int? = null,
    val versionAfter: Int? = null,
    val wroteToDisk: Boolean = false,
)

public sealed interface DaisyManagedBundleReloadResult<T> {
    public data class Success<T>(
        val value: T,
        val warnings: List<DaisyConfigWarning> = emptyList(),
        val reports: List<DaisyManagedMigrationReport> = emptyList(),
    ) : DaisyManagedBundleReloadResult<T>

    public data class Failure<T>(
        val errors: List<DaisyConfigError>,
        val previousValue: T?,
        val reports: List<DaisyManagedMigrationReport> = emptyList(),
    ) : DaisyManagedBundleReloadResult<T>
}

public fun <T> DaisyManagedReloadResult<T>.asReloadResult(): DaisyReloadResult<T> =
    when (this) {
        is DaisyManagedReloadResult.Success -> DaisyReloadResult.Success(value, warnings)
        is DaisyManagedReloadResult.Failure -> DaisyReloadResult.Failure(errors, previousValue)
    }

public fun <T> DaisyManagedBundleReloadResult<T>.asReloadResult(): DaisyReloadResult<T> =
    when (this) {
        is DaisyManagedBundleReloadResult.Success -> DaisyReloadResult.Success(value, warnings)
        is DaisyManagedBundleReloadResult.Failure -> DaisyReloadResult.Failure(errors, previousValue)
    }
