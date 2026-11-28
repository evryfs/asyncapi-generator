package dev.banking.asyncapi.generator.core.generator.configuration

/**
 * Typed generation purpose resolved from the user-facing generator name.
 *
 * A profile has exactly one responsibility: generate source code, generate
 * schema artifacts, or serialize an AsyncAPI document.
 */
internal sealed interface GeneratorProfile {
    /**
     * Generates source code.
     *
     * @param language the language of the generated sources.
     */
    data class Source(
        val language: SourceLanguage,
    ) : GeneratorProfile

    /**
     * Generates schema artifacts.
     *
     * @param type the format of the generated schemas.
     */
    data class Schema(
        val type: SchemaType,
    ) : GeneratorProfile

    /**
     * Serializes the bundled AsyncAPI document.
     *
     * @param format the serialization of the written document.
     */
    data class Document(
        val format: DocumentFormat,
    ) : GeneratorProfile
}
