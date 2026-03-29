package cat.daisy.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DaisyConfigCodecTest {
    @Test
    fun `object codec decodes required and defaulted fields`() {
        data class Example(
            val title: String,
            val rows: Int,
        )

        val codec =
            objectCodec {
                Example(
                    title = required("title", stringCodec()),
                    rows = defaulted("rows", intCodec(), 3),
                )
            }

        val node =
            TestNode(
                mapOf(
                    "title" to "Profile",
                ),
            )

        val result = codec.decode(node)
        assertTrue(result is DaisyDecodeResult.Success)
        assertEquals("Profile", result.value.title)
        assertEquals(3, result.value.rows)
    }

    @Test
    fun `validation returns failure`() {
        val codec =
            objectCodec {
                required("rows", intCodec())
            }.validate { rows ->
                if (rows in 1..6) emptyList() else listOf(DaisyConfigError("rows", "Rows must be between 1 and 6."))
            }

        val result = codec.decode(TestNode(mapOf("rows" to 9)))
        assertTrue(result is DaisyDecodeResult.Failure)
        assertEquals("rows", result.errors.first().path)
    }
}

private class TestNode(
    private val value: Any?,
) : DaisyConfigNode {
    override fun isNull(): Boolean = value == null

    override fun asString(): String? = value as? String

    override fun asInt(): Int? = when (value) {
        is Int -> value
        is Number -> value.toInt()
        else -> null
    }

    override fun asLong(): Long? = when (value) {
        is Long -> value
        is Number -> value.toLong()
        else -> null
    }

    override fun asDouble(): Double? = when (value) {
        is Double -> value
        is Number -> value.toDouble()
        else -> null
    }

    override fun asBoolean(): Boolean? = value as? Boolean

    override fun get(key: String): DaisyConfigNode = TestNode((value as? Map<*, *>)?.get(key))

    override fun entries(): Map<String, DaisyConfigNode> =
        (value as? Map<*, *>)
            ?.entries
            ?.associate { it.key.toString() to TestNode(it.value) }
            .orEmpty()

    override fun elements(): List<DaisyConfigNode> = (value as? List<*>)?.map(::TestNode).orEmpty()
}
