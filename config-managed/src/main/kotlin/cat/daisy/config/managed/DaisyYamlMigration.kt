package cat.daisy.config.managed

import cat.daisy.config.DaisyConfigWarning

public interface DaisyYamlMigration {
    public val fromVersion: Int
    public val toVersion: Int

    public fun apply(root: DaisyMutableYamlNode): DaisyYamlMigrationResult
}

public data class DaisyYamlMigrationResult(
    val warnings: List<DaisyConfigWarning> = emptyList(),
    val renamedKeys: List<Pair<String, String>> = emptyList(),
    val removedKeys: List<String> = emptyList(),
)

public interface DaisyMutableYamlNode {
    public fun isNull(): Boolean

    public fun get(path: String): DaisyMutableYamlNode

    public fun set(path: String, value: Any?)

    public fun remove(path: String): Boolean

    public fun contains(path: String): Boolean

    public fun asString(): String?

    public fun asInt(): Int?

    public fun asBoolean(): Boolean?

    public fun keys(deep: Boolean = false): Set<String>

    public fun asMap(): Map<String, Any?>
}

public enum class DaisyYamlMergePolicy {
    AddMissingKeys,
    None,
}
