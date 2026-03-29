package cat.daisy.config.managed

public object DaisyYamlMigrations {
    public fun rename(
        fromVersion: Int,
        toVersion: Int,
        from: String,
        to: String,
    ): DaisyYamlMigration = move(fromVersion, toVersion, from, to)

    public fun remove(
        fromVersion: Int,
        toVersion: Int,
        path: String,
    ): DaisyYamlMigration =
        simpleMigration(fromVersion, toVersion) { root ->
            DaisyYamlMigrationResult(
                removedKeys = if (root.remove(path)) listOf(path) else emptyList(),
            )
        }

    public fun setDefault(
        fromVersion: Int,
        toVersion: Int,
        path: String,
        value: Any?,
    ): DaisyYamlMigration =
        simpleMigration(fromVersion, toVersion) { root ->
            if (root.contains(path)) {
                DaisyYamlMigrationResult()
            } else {
                root.set(path, value)
                DaisyYamlMigrationResult()
            }
        }

    public fun move(
        fromVersion: Int,
        toVersion: Int,
        from: String,
        to: String,
    ): DaisyYamlMigration =
        simpleMigration(fromVersion, toVersion) { root ->
            if (!root.contains(from)) {
                DaisyYamlMigrationResult()
            } else {
                val value = root.get(from).materialize()
                root.set(to, value)
                root.remove(from)
                DaisyYamlMigrationResult(renamedKeys = listOf(from to to))
            }
        }

    private fun simpleMigration(
        fromVersion: Int,
        toVersion: Int,
        action: (DaisyMutableYamlNode) -> DaisyYamlMigrationResult,
    ): DaisyYamlMigration =
        object : DaisyYamlMigration {
            override val fromVersion: Int = fromVersion
            override val toVersion: Int = toVersion

            override fun apply(root: DaisyMutableYamlNode): DaisyYamlMigrationResult = action(root)
        }
}

private fun DaisyMutableYamlNode.materialize(): Any? =
    when {
        isNull() -> null
        asString() != null -> asString()
        asInt() != null -> asInt()
        asBoolean() != null -> asBoolean()
        else -> asMap()
    }
