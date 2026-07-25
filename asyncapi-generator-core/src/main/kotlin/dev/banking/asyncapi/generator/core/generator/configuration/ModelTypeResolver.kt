package dev.banking.asyncapi.generator.core.generator.configuration

import dev.banking.asyncapi.generator.core.generator.model.GeneratorName

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
        val supportedModelTypes = supportedModelTypes(generatorName)
        val modelType = configuredModelType ?: defaultModelType(generatorName)

        if (modelType !in supportedModelTypes) {
            throw IllegalArgumentException(
                "modelConfig.modelType '${modelType.configurationValue}' is not supported when " +
                    "generatorName is ${generatorName.configurationValue}. Supported values: " +
                    supportedModelTypes.joinToString(", ") { it.configurationValue },
            )
        }

        return modelType
    }

    private fun defaultModelType(generatorName: GeneratorName): ModelType =
        when (generatorName) {
            GeneratorName.KOTLIN -> ModelType.KOTLIN_DATA_CLASS
            GeneratorName.JAVA -> ModelType.JAVA_CLASS
        }

    private fun supportedModelTypes(generatorName: GeneratorName): List<ModelType> =
        when (generatorName) {
            GeneratorName.KOTLIN ->
                listOf(
                    ModelType.KOTLIN_DATA_CLASS,
                    ModelType.AVRO_SPECIFIC_RECORD,
                    ModelType.PROTOBUF_MESSAGE,
                )
            GeneratorName.JAVA ->
                listOf(
                    ModelType.JAVA_CLASS,
                    ModelType.JAVA_RECORD,
                    ModelType.AVRO_SPECIFIC_RECORD,
                    ModelType.PROTOBUF_MESSAGE,
                )
        }
}
