package cat.daisy.config.daisycore

import cat.daisy.config.DaisyConfigNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DaisyTextConfigTest {
    @Test
    fun `text config codec flattens nested keys`() {
        val node =
            TestNode(
                mapOf(
                    "messages" to mapOf("ready" to "<green>Ready"),
                    "menus" to mapOf("profile" to mapOf("lore" to listOf("<gray>A", "<white>B"))),
                ),
            )

        val result = daisyTextConfigCodec().decode(node, "")
        assertTrue(result is cat.daisy.config.DaisyDecodeResult.Success)
        assertEquals("<green>Ready", result.value.text("messages.ready"))
        assertEquals(listOf("<gray>A", "<white>B"), result.value.textList("menus.profile.lore"))
    }
}

private class TestNode(
    private val value: Any?,
) : DaisyConfigNode {
    override fun isNull(): Boolean = value == null
    override fun asString(): String? = value as? String
    override fun asInt(): Int? = null
    override fun asLong(): Long? = null
    override fun asDouble(): Double? = null
    override fun asBoolean(): Boolean? = null
    override fun get(key: String): DaisyConfigNode = TestNode((value as? Map<*, *>)?.get(key))
    override fun entries(): Map<String, DaisyConfigNode> = (value as? Map<*, *>)?.entries?.associate { it.key.toString() to TestNode(it.value) }.orEmpty()
    override fun elements(): List<DaisyConfigNode> = (value as? List<*>)?.map(::TestNode).orEmpty()
}
