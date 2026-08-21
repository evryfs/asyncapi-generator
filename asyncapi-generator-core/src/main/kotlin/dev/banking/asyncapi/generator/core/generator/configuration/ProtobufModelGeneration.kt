package dev.banking.asyncapi.generator.core.generator.configuration

/**
 * Enabled native Protobuf model generation.
 */
data class ProtobufModelGeneration(
    val packageName: String,
    val modelType: ProtobufModelType = ProtobufModelType.JAVA,
)
