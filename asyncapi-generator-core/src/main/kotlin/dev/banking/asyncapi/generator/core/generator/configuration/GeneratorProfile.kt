package dev.banking.asyncapi.generator.core.generator.configuration

import dev.banking.asyncapi.generator.core.generator.model.SourceLanguage

/**
 * Typed generation purpose resolved from the user-facing generator name.
 *
 * A profile has exactly one responsibility: generate source code, generate
 * schema artifacts, or serialize an AsyncAPI document.
 */
sealed interface GeneratorProfile {
    data class Source(
        val language: SourceLanguage,
    ) : GeneratorProfile

    data class Schema(
        val type: SchemaType,
    ) : GeneratorProfile

    data class Document(
        val format: DocumentFormat,
    ) : GeneratorProfile
}
