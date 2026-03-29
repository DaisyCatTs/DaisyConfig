package cat.daisy.config.example

import cat.daisy.config.daisycore.DaisyTextConfig

data class ExampleTextConfig(
    private val values: Map<String, Any?>,
) : DaisyTextConfig {
    override fun text(key: String): String? = values[key] as? String

    override fun textList(key: String): List<String> = (values[key] as? List<*>)?.mapNotNull { it as? String }.orEmpty()
}

