package dev.banking.asyncapi.generator.core.generator.configuration

/**
 * Enabled native Protobuf model generation.
 *
 * Expected behavior is covered by:
 * - `GenerationPlannerTest`
 */
data class ProtobufModelGeneration(
    val packageName: String,
    val modelType: ProtobufModelType = ProtobufModelType.JAVA,
)
