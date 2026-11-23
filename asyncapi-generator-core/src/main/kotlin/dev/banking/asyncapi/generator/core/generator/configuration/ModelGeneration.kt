package dev.banking.asyncapi.generator.core.generator.configuration

/**
 * Typed source model generation configuration.
 */
sealed interface ModelGeneration {
    /** No source models; native Avro and Protobuf models are [SchemaGeneration]s instead. */
    data object Disabled : ModelGeneration

    /**
     * Source models written by the source generator.
     *
     * @param packageName the package of the generated models.
     * @param annotation the annotation placed on every generated model.
     * @param modelType the model implementation, one written by the source generator's language.
     */
    data class Enabled(
        val packageName: String,
        val annotation: QualifiedTypeName? = null,
        val modelType: ModelType,
    ) : ModelGeneration
}
