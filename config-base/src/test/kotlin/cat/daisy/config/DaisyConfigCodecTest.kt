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

    @Test
    fun `object codec decodes nested section`() {
        data class Feedback(
            val sound: String,
            val message: String,
        )

        data class Example(
            val title: String,
            val feedback: Feedback,
        )

        val feedbackCodec =
            objectCodec {
                Feedback(
                    sound = required("sound", stringCodec()),
                    message = defaulted("message", stringCodec(), "<green>Saved.</green>"),
                )
            }

        val codec =
            objectCodec {
                Example(
                    title = required("title", stringCodec()),
                    feedback = section("feedback", feedbackCodec),
                )
            }

        val node =
            TestNode(
                mapOf(
                    "title" to "Profile",
                    "feedback" to
                        mapOf(
                            "sound" to "entity_player_levelup",
                        ),
                ),
            )

        val result = codec.decode(node)
        assertTrue(result is DaisyDecodeResult.Success)
        assertEquals("Profile", result.value.title)
        assertEquals("entity_player_levelup", result.value.feedback.sound)
        assertEquals("<green>Saved.</green>", result.value.feedback.message)
    }

    @Test
    fun `optional and defaulted sections work`() {
        data class Section(
            val title: String,
        )

        data class Example(
            val optional: Section?,
            val defaulted: Section,
        )

        val sectionCodec =
            objectCodec {
                Section(
                    title = defaulted("title", stringCodec(), "Fallback"),
                )
            }

        val codec =
            objectCodec {
                Example(
                    optional = optionalSection("optional", sectionCodec),
                    defaulted = defaultedSection("defaulted", sectionCodec, Section("Default Section")),
                )
            }

        val result = codec.decode(TestNode(emptyMap<String, Any?>()))
        assertTrue(result is DaisyDecodeResult.Success)
        assertEquals(null, result.value.optional)
        assertEquals("Default Section", result.value.defaulted.title)
    }

    @Test
    fun `validation helpers return useful path errors`() {
        data class Example(
            val title: String,
            val rows: Int,
        )

        val codec =
            objectCodec {
                Example(
                    title = required("title", stringCodec()),
                    rows = required("rows", intCodec()),
                )
            }.validate { config ->
                DaisyValidation.notBlank("title", config.title) +
                    DaisyValidation.intRange("rows", config.rows, 1, 6)
            }

        val result =
            codec.decode(
                TestNode(
                    mapOf(
                        "title" to "",
                        "rows" to 9,
                    ),
                ),
            )

        assertTrue(result is DaisyDecodeResult.Failure)
        assertEquals(listOf("title", "rows"), result.errors.map { it.path })
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
