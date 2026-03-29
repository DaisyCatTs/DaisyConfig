package cat.daisy.config

public interface DaisyConfigHandle<T> {
    public val current: T

    public fun reload(): DaisyReloadResult<T>
}

public interface DaisyConfigBundleHandle<T> {
    public val current: T

    public fun reload(): DaisyReloadResult<T>
}

public sealed interface DaisyReloadResult<T> {
    public data class Success<T>(
        val value: T,
        val warnings: List<DaisyConfigWarning> = emptyList(),
    ) : DaisyReloadResult<T>

    public data class Failure<T>(
        val errors: List<DaisyConfigError>,
        val previousValue: T?,
    ) : DaisyReloadResult<T>
}

public data class DaisyConfigError(
    val path: String,
    val message: String,
)

public fun DaisyConfigError.at(path: String): DaisyConfigError = copy(path = path)

public data class DaisyConfigWarning(
    val path: String,
    val message: String,
)

public sealed interface DaisyDecodeResult<T> {
    public data class Success<T>(
        val value: T,
        val warnings: List<DaisyConfigWarning> = emptyList(),
    ) : DaisyDecodeResult<T>

    public data class Failure<T>(
        val errors: List<DaisyConfigError>,
    ) : DaisyDecodeResult<T>
}

public fun interface DaisyConfigCodec<T> {
    public fun decode(
        node: DaisyConfigNode,
        path: String,
    ): DaisyDecodeResult<T>
}

public fun <T> DaisyConfigCodec<T>.decode(node: DaisyConfigNode): DaisyDecodeResult<T> = decode(node, "")

public interface DaisyConfigNode {
    public fun isNull(): Boolean

    public fun asString(): String?

    public fun asInt(): Int?

    public fun asLong(): Long?

    public fun asDouble(): Double?

    public fun asBoolean(): Boolean?

    public fun get(key: String): DaisyConfigNode

    public fun entries(): Map<String, DaisyConfigNode>

    public fun elements(): List<DaisyConfigNode>
}

public interface DaisyFieldReader {
    public fun <T> required(
        name: String,
        codec: DaisyConfigCodec<T>,
    ): T

    public fun <T> optional(
        name: String,
        codec: DaisyConfigCodec<T>,
    ): T?

    public fun <T> defaulted(
        name: String,
        codec: DaisyConfigCodec<T>,
        default: T,
    ): T

    public fun <T> section(
        name: String,
        codec: DaisyConfigCodec<T>,
    ): T

    public fun <T> optionalSection(
        name: String,
        codec: DaisyConfigCodec<T>,
    ): T?

    public fun <T> defaultedSection(
        name: String,
        codec: DaisyConfigCodec<T>,
        default: T,
    ): T
}

public fun <T> objectCodec(factory: DaisyFieldReader.() -> T): DaisyConfigCodec<T> = DaisyObjectCodec(factory)

private class DaisyObjectCodec<T>(
    private val factory: DaisyFieldReader.() -> T,
) : DaisyConfigCodec<T> {
    override fun decode(
        node: DaisyConfigNode,
        path: String,
    ): DaisyDecodeResult<T> {
        val reader = DaisyFieldReaderImpl(node = node, path = path)
        return try {
            val value = reader.factory()
            if (reader.errors.isEmpty()) {
                DaisyDecodeResult.Success(value = value, warnings = reader.warnings)
            } else {
                DaisyDecodeResult.Failure(reader.errors.toList())
            }
        } catch (_: DaisyConfigAbort) {
            DaisyDecodeResult.Failure(reader.errors.toList())
        }
    }
}

private class DaisyFieldReaderImpl(
    private val node: DaisyConfigNode,
    private val path: String,
) : DaisyFieldReader {
    val errors: MutableList<DaisyConfigError> = mutableListOf()
    val warnings: MutableList<DaisyConfigWarning> = mutableListOf()

    override fun <T> required(
        name: String,
        codec: DaisyConfigCodec<T>,
    ): T {
        val child = node.get(name)
        val childPath = path.child(name)
        return when (val result = codec.decode(child, childPath)) {
            is DaisyDecodeResult.Success -> {
                warnings += result.warnings
                result.value
            }
            is DaisyDecodeResult.Failure -> {
                errors += result.errors
                throw DaisyConfigAbort
            }
        }
    }

    override fun <T> optional(
        name: String,
        codec: DaisyConfigCodec<T>,
    ): T? {
        val child = node.get(name)
        if (child.isNull()) {
            return null
        }
        return when (val result = codec.decode(child, path.child(name))) {
            is DaisyDecodeResult.Success -> {
                warnings += result.warnings
                result.value
            }
            is DaisyDecodeResult.Failure -> {
                errors += result.errors
                null
            }
        }
    }

    override fun <T> defaulted(
        name: String,
        codec: DaisyConfigCodec<T>,
        default: T,
    ): T {
        val child = node.get(name)
        if (child.isNull()) {
            return default
        }
        return when (val result = codec.decode(child, path.child(name))) {
            is DaisyDecodeResult.Success -> {
                warnings += result.warnings
                result.value
            }
            is DaisyDecodeResult.Failure -> {
                errors += result.errors
                default
            }
        }
    }

    override fun <T> section(
        name: String,
        codec: DaisyConfigCodec<T>,
    ): T = required(name, codec)

    override fun <T> optionalSection(
        name: String,
        codec: DaisyConfigCodec<T>,
    ): T? = optional(name, codec)

    override fun <T> defaultedSection(
        name: String,
        codec: DaisyConfigCodec<T>,
        default: T,
    ): T = defaulted(name, codec, default)
}

private data object DaisyConfigAbort : RuntimeException()

private fun String.child(name: String): String = if (isBlank()) name else "$this.$name"

public fun stringCodec(): DaisyConfigCodec<String> = primitiveCodec("string") { it.asString() }

public fun intCodec(): DaisyConfigCodec<Int> = primitiveCodec("integer") { it.asInt() }

public fun longCodec(): DaisyConfigCodec<Long> = primitiveCodec("long") { it.asLong() }

public fun doubleCodec(): DaisyConfigCodec<Double> = primitiveCodec("double") { it.asDouble() }

public fun booleanCodec(): DaisyConfigCodec<Boolean> = primitiveCodec("boolean") { it.asBoolean() }

public fun <T> optional(codec: DaisyConfigCodec<T>): DaisyConfigCodec<T?> =
    DaisyConfigCodec<T?> { node, path ->
        if (node.isNull()) {
            DaisyDecodeResult.Success(null)
        } else {
            when (val result = codec.decode(node, path)) {
                is DaisyDecodeResult.Success -> DaisyDecodeResult.Success(result.value, result.warnings)
                is DaisyDecodeResult.Failure -> DaisyDecodeResult.Failure(result.errors)
            }
        }
    }

public fun <T> defaulted(
    codec: DaisyConfigCodec<T>,
    defaultValue: T,
): DaisyConfigCodec<T> =
    DaisyConfigCodec<T> { node, path ->
        if (node.isNull()) {
            DaisyDecodeResult.Success(defaultValue)
        } else {
            when (val result = codec.decode(node, path)) {
                is DaisyDecodeResult.Success -> DaisyDecodeResult.Success(result.value, result.warnings)
                is DaisyDecodeResult.Failure -> DaisyDecodeResult.Success(defaultValue)
            }
        }
    }

public fun <T> listCodec(itemCodec: DaisyConfigCodec<T>): DaisyConfigCodec<List<T>> =
    DaisyConfigCodec<List<T>> { node, path ->
        val values = mutableListOf<T>()
        val warnings = mutableListOf<DaisyConfigWarning>()
        val errors = mutableListOf<DaisyConfigError>()
        node.elements().forEachIndexed { index, element ->
            when (val result = itemCodec.decode(element, path.child(index.toString()))) {
                is DaisyDecodeResult.Success -> {
                    values += result.value
                    warnings += result.warnings
                }
                is DaisyDecodeResult.Failure -> errors += result.errors
            }
        }
        if (errors.isEmpty()) {
            DaisyDecodeResult.Success(values, warnings)
        } else {
            DaisyDecodeResult.Failure(errors)
        }
    }

public fun <T> setCodec(itemCodec: DaisyConfigCodec<T>): DaisyConfigCodec<Set<T>> =
    DaisyConfigCodec<Set<T>> { node, path ->
        when (val result = listCodec(itemCodec).decode(node, path)) {
            is DaisyDecodeResult.Success -> DaisyDecodeResult.Success(result.value.toCollection(linkedSetOf()), result.warnings)
            is DaisyDecodeResult.Failure -> DaisyDecodeResult.Failure(result.errors)
        }
    }

public fun <T> mapCodec(valueCodec: DaisyConfigCodec<T>): DaisyConfigCodec<Map<String, T>> =
    DaisyConfigCodec<Map<String, T>> { node, path ->
        val values = linkedMapOf<String, T>()
        val warnings = mutableListOf<DaisyConfigWarning>()
        val errors = mutableListOf<DaisyConfigError>()
        node.entries().forEach { (key, child) ->
            when (val result = valueCodec.decode(child, path.child(key))) {
                is DaisyDecodeResult.Success -> {
                    values[key] = result.value
                    warnings += result.warnings
                }
                is DaisyDecodeResult.Failure -> errors += result.errors
            }
        }
        if (errors.isEmpty()) DaisyDecodeResult.Success(values, warnings) else DaisyDecodeResult.Failure(errors)
    }

public fun <T> DaisyConfigCodec<T>.validate(validator: (T) -> List<DaisyConfigError>): DaisyConfigCodec<T> =
    DaisyConfigCodec<T> { node, path ->
        when (val result = decode(node, path)) {
            is DaisyDecodeResult.Success -> {
                val errors = validator(result.value)
                if (errors.isEmpty()) {
                    result
                } else {
                    DaisyDecodeResult.Failure(errors)
                }
            }
            is DaisyDecodeResult.Failure -> result
        }
    }

private fun <T> primitiveCodec(
    type: String,
    reader: (DaisyConfigNode) -> T?,
): DaisyConfigCodec<T> =
    DaisyConfigCodec<T> { node, path ->
        if (node.isNull()) {
            DaisyDecodeResult.Failure(listOf(DaisyConfigError(path, "Expected $type, found null.")))
        } else {
            val value = reader(node)
            if (value != null) {
                DaisyDecodeResult.Success(value)
            } else {
                DaisyDecodeResult.Failure(listOf(DaisyConfigError(path, "Expected $type.")))
            }
        }
    }
