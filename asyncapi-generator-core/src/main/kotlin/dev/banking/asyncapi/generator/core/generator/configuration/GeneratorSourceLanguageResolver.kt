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
    fun resolve(
        generatorName: GeneratorName,
        protobufModelType: ProtobufModelType? = null,
    ): SourceLanguage =
        when (generatorName) {
            GeneratorName.JAVA -> SourceLanguage.JAVA
            GeneratorName.KOTLIN -> SourceLanguage.KOTLIN
            GeneratorName.AVRO -> SourceLanguage.JAVA
            GeneratorName.PROTOBUF ->
                when (protobufModelType ?: ProtobufModelType.JAVA) {
                    ProtobufModelType.JAVA -> SourceLanguage.JAVA
                    ProtobufModelType.KOTLIN -> SourceLanguage.KOTLIN
                }
        }
}
