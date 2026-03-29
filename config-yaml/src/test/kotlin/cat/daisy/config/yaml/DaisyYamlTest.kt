package cat.daisy.config.yaml

import cat.daisy.config.DaisyReloadResult
import cat.daisy.config.defaulted
import cat.daisy.config.intCodec
import cat.daisy.config.objectCodec
import cat.daisy.config.stringCodec
import java.io.File
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
}
