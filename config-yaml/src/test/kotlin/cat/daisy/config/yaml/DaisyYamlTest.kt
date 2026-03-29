package cat.daisy.config.yaml

import cat.daisy.config.DaisyReloadResult
import cat.daisy.config.defaulted
import cat.daisy.config.intCodec
import cat.daisy.config.objectCodec
import cat.daisy.config.stringCodec
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DaisyYamlTest {
    @Test
    fun `yaml load decodes object`() {
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

        val file = File.createTempFile("daisy-config", ".yml")
        file.writeText("title: Profile\n")

        val result = DaisyYaml.load(file, codec)
        assertTrue(result is DaisyReloadResult.Success)
        assertEquals("Profile", result.value.title)
        assertEquals(3, result.value.rows)
    }

    @Test
    fun `yaml bundle handle preserves previous current on reload failure`() {
        data class UiConfig(
            val icon: String,
        )

        data class MenuConfig(
            val rows: Int,
        )

        data class BundleConfig(
            val ui: UiConfig,
            val menu: MenuConfig,
        )

        val uiCodec =
            objectCodec {
                UiConfig(
                    icon = required("icon", stringCodec()),
                )
            }

        val menuCodec =
            objectCodec {
                MenuConfig(
                    rows = required("rows", intCodec()),
                )
            }

        val directory = createTempDirectory(prefix = "daisy-bundle").toFile()
        File(directory, "profile-ui.yml").writeText("icon: diamond_sword\n")
        File(directory, "profile-menu.yml").writeText("rows: 3\n")

        val handle =
            DaisyYamlBundle.handle(directory) {
                BundleConfig(
                    ui = file("profile-ui.yml", uiCodec),
                    menu = file("profile-menu.yml", menuCodec),
                )
            }

        assertEquals("diamond_sword", handle.current.ui.icon)
        assertEquals(3, handle.current.menu.rows)

        File(directory, "profile-menu.yml").writeText("rows: nope\n")

        val result = handle.reload()
        assertTrue(result is DaisyReloadResult.Failure)
        assertEquals("diamond_sword", handle.current.ui.icon)
        assertEquals(3, handle.current.menu.rows)
        assertEquals("profile-menu.yml.rows", result.errors.first().path)
    }
}
