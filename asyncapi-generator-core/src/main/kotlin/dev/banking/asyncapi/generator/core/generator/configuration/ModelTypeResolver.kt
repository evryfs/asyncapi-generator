package dev.banking.asyncapi.generator.core.generator.configuration

import dev.banking.asyncapi.generator.core.generator.model.GeneratorName
import dev.banking.asyncapi.generator.core.generator.model.SourceLanguage

/**
 * Resolves model defaults and validates model compatibility for a generator.
 *
 * Expected behavior is covered by:
 * - `ModelTypeResolverTest`
 */
object ModelTypeResolver {
    fun resolve(
        generatorName: GeneratorName,
        configuredModelType: ModelType?,
    ): ModelType {
        val sourceLanguage = GeneratorSourceLanguageResolver.resolve(generatorName)
        val supportedModelTypes = supportedModelTypes(sourceLanguage)
        val modelType = configuredModelType ?: defaultModelType(sourceLanguage)

        if (modelType !in supportedModelTypes) {
            throw IllegalArgumentException(
                "modelConfig.modelType '${modelType.configurationValue}' is not supported when " +
                    "generatorName is ${generatorName.configurationValue}. Supported values: " +
                    supportedModelTypes.joinToString(", ") { it.configurationValue },
            )
        }

        return modelType
    }

    private fun defaultModelType(sourceLanguage: SourceLanguage): ModelType =
        when (sourceLanguage) {
            SourceLanguage.KOTLIN -> ModelType.KOTLIN_DATA_CLASS
            SourceLanguage.JAVA -> ModelType.JAVA_CLASS
        }

    private fun supportedModelTypes(sourceLanguage: SourceLanguage): List<ModelType> =
        when (sourceLanguage) {
            SourceLanguage.KOTLIN ->
                listOf(
                    ModelType.KOTLIN_DATA_CLASS,
                    ModelType.AVRO_SPECIFIC_RECORD,
                    ModelType.PROTOBUF_MESSAGE,
                )
            SourceLanguage.JAVA ->
                listOf(
                    ModelType.JAVA_CLASS,
                    ModelType.JAVA_RECORD,
                    ModelType.AVRO_SPECIFIC_RECORD,
                    ModelType.PROTOBUF_MESSAGE,
                )
        }
}
