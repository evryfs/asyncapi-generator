package dev.banking.asyncapi.generator.core.generator.configuration

/**
 * Internal Java source shape used by Java model generation.
 *
 * Expected behavior is covered by:
 * - `JavaGeneratorTest`
 */
enum class JavaModelType(
    val configurationValue: String,
) {
    CLASS("class"),
    RECORD("record"),
    ;
}
