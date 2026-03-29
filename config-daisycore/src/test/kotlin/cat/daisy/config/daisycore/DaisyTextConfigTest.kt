package cat.daisy.config.daisycore

import cat.daisy.config.DaisyConfigBundleHandle
import cat.daisy.config.DaisyConfigHandle
import cat.daisy.config.DaisyReloadResult
import cat.daisy.config.DaisyConfigNode
import cat.daisy.config.managed.DaisyManagedConfigHandle
import cat.daisy.config.managed.DaisyManagedConfigMetadata
import cat.daisy.config.managed.DaisyManagedReloadResult
import cat.daisy.config.managed.DaisyManagedYamlFile
import cat.daisy.config.stringCodec
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

    @Test
    fun `merge text configs uses later configs as override`() {
        val first =
            object : DaisyTextConfig {
                override fun text(key: String): String? = if (key == "messages.ready") "<gray>First</gray>" else null

                override fun textList(key: String): List<String> = if (key == "menus.profile.lore") listOf("<gray>A") else emptyList()
            }

        val second =
            object : DaisyTextConfig {
                override fun text(key: String): String? = if (key == "messages.ready") "<green>Second</green>" else null

                override fun textList(key: String): List<String> = if (key == "menus.profile.lore") listOf("<white>B") else emptyList()
            }

        val merged = mergeTextConfigs(first, second)
        assertEquals("<green>Second</green>", merged.text("messages.ready"))
        assertEquals(listOf("<white>B"), merged.textList("menus.profile.lore"))
    }

    @Test
    fun `handle backed text source stays live across current changes`() {
        val handle = MutableTextHandle(SimpleTextConfig(mapOf("messages.ready" to "<green>Ready</green>")))
        val textSource = handle.asDaisyTextSource()

        assertEquals("<green>Ready</green>", textSource.text("messages.ready"))

        handle.current = SimpleTextConfig(mapOf("messages.ready" to "<blue>Reloaded</blue>"))

        assertEquals("<blue>Reloaded</blue>", textSource.text("messages.ready"))
    }

    @Test
    fun `bundle handle backed text source stays live across current changes`() {
        val handle = MutableTextBundleHandle(SimpleTextConfig(mapOf("messages.ready" to "<green>Ready</green>")))
        val textSource = handle.asDaisyTextSource()

        assertEquals("<green>Ready</green>", textSource.text("messages.ready"))

        handle.current = SimpleTextConfig(mapOf("messages.ready" to "<gold>Bundle Reloaded</gold>"))

        assertEquals("<gold>Bundle Reloaded</gold>", textSource.text("messages.ready"))
    }

    @Test
    fun `managed text handle stays live and does not expand placeholders`() {
        val file =
            DaisyManagedYamlFile(
                id = "lang",
                path = "lang.yml",
                codec = daisyTextConfigCodec(),
            )
        val handle =
            MutableManagedTextHandle(
                file = file,
                current = SimpleTextConfig(mapOf("messages.ready" to "<green>Hello %player_name%</green>")),
            )

        val textSource = handle.asDaisyTextSource()

        assertEquals("<green>Hello %player_name%</green>", textSource.text("messages.ready"))

        handle.current = SimpleTextConfig(mapOf("messages.ready" to "<blue>Reloaded %player_name%</blue>"))

        assertEquals("<blue>Reloaded %player_name%</blue>", textSource.text("messages.ready"))
    }
}

private data class SimpleTextConfig(
    private val values: Map<String, String>,
) : DaisyTextConfig {
    override fun text(key: String): String? = values[key]

    override fun textList(key: String): List<String> = emptyList()
}

private class MutableTextHandle(
    override var current: DaisyTextConfig,
) : DaisyConfigHandle<DaisyTextConfig> {
    override fun reload(): DaisyReloadResult<DaisyTextConfig> = DaisyReloadResult.Success(current)
}

private class MutableTextBundleHandle(
    override var current: DaisyTextConfig,
) : DaisyConfigBundleHandle<DaisyTextConfig> {
    override fun reload(): DaisyReloadResult<DaisyTextConfig> = DaisyReloadResult.Success(current)
}

private class MutableManagedTextHandle(
    override val file: DaisyManagedYamlFile<DaisyTextConfig>,
    override var current: DaisyTextConfig,
) : DaisyManagedConfigHandle<DaisyTextConfig> {
    override val metadata: DaisyManagedConfigMetadata =
        DaisyManagedConfigMetadata(
            path = file.path,
            versionKey = file.versionKey,
            currentVersion = file.currentVersion,
            diskVersion = file.currentVersion,
        )

    override fun reload(): DaisyReloadResult<DaisyTextConfig> = DaisyReloadResult.Success(current)

    override fun migrate(): DaisyManagedReloadResult<DaisyTextConfig> =
        DaisyManagedReloadResult.Success(
            value = current,
            report = cat.daisy.config.managed.DaisyManagedMigrationReport(),
        )
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
