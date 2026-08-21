package dev.banking.asyncapi.generator.core.generator.configuration

import dev.banking.asyncapi.generator.core.generator.model.GeneratorName
import dev.banking.asyncapi.generator.core.generator.model.SourceLanguage

/**
 * Resolves the primary source language produced by a configured generator.
 */
object GeneratorSourceLanguageResolver {
    fun resolve(generatorName: GeneratorName): SourceLanguage =
        resolveOrNull(generatorName)
            ?: throw IllegalArgumentException(
                "generatorName '${generatorName.configurationValue}' does not generate source code",
            )

    fun resolveOrNull(generatorName: GeneratorName): SourceLanguage? =
        (generatorName.profile as? GeneratorProfile.Source)?.language
}
