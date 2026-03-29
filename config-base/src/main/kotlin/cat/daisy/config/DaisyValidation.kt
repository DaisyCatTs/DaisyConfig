package cat.daisy.config

public object DaisyValidation {
    public fun require(
        condition: Boolean,
        path: String,
        message: String,
    ): List<DaisyConfigError> = if (condition) emptyList() else listOf(DaisyConfigError(path, message))

    public fun notBlank(
        path: String,
        value: String,
        message: String = "Value cannot be blank.",
    ): List<DaisyConfigError> = require(value.isNotBlank(), path, message)

    public fun intRange(
        path: String,
        value: Int,
        min: Int,
        max: Int,
    ): List<DaisyConfigError> = require(value in min..max, path, "Value must be between $min and $max.")
}
