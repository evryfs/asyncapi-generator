package dev.banking.asyncapi.generator.core.generator.configuration

import dev.banking.asyncapi.generator.core.generator.model.GeneratorName
import dev.banking.asyncapi.generator.core.generator.model.SourceLanguage

/**
 * Resolves the primary source language produced by a configured generator.
 *
 * Expected behavior is covered by:
 * - `GeneratorConfigurationFactoryTest`
 */
object GeneratorSourceLanguageResolver {
    fun resolve(generatorName: GeneratorName): SourceLanguage =
        (generatorName.profile as GeneratorProfile.Source).language
}
