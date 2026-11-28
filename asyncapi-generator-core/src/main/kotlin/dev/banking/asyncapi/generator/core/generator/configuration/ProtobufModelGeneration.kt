package dev.banking.asyncapi.generator.core.generator.configuration

/**
 * Enabled native Protobuf model generation.
 *
 * @param packageName the package of the generated models.
 * @param modelType the Protobuf compiler output.
 */
internal data class ProtobufModelGeneration(
    val packageName: String,
    val modelType: ProtobufModelType = ProtobufModelType.JAVA,
)
