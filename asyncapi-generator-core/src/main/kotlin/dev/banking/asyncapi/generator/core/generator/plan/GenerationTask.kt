package dev.banking.asyncapi.generator.core.generator.plan

import dev.banking.asyncapi.generator.core.generator.configuration.JavaModelType
import dev.banking.asyncapi.generator.core.generator.configuration.ProtobufModelGeneration
import dev.banking.asyncapi.generator.core.generator.model.SourceLanguage

/**
 * Planned generator work item.
 *
 * Expected behavior is covered by:
 * - `GenerationPlannerTest`
 */
sealed interface GenerationTask {
    data class ModelArtifacts(
        val language: SourceLanguage,
        val packageName: String,
        val annotation: String? = null,
        val javaModelType: JavaModelType = JavaModelType.CLASS,
    ) : GenerationTask

    data class HeaderModelArtifacts(
        val language: SourceLanguage,
        val packageName: String,
    ) : GenerationTask

    data class SpringKafkaClient(
        val language: SourceLanguage,
        val clientPackage: String,
        val modelPackage: String,
        val generateHeaders: Boolean = true,
        val generateProducers: Boolean = true,
        val generateConsumers: Boolean = true,
    ) : GenerationTask

    data class QuarkusKafkaClient(
        val language: SourceLanguage,
    ) : GenerationTask

    data class AvroSchemaArtifacts(
        val packageName: String,
    ) : GenerationTask

    data class NativeAvroArtifacts(
        val generateSpecificRecords: Boolean = true,
    ) : GenerationTask

    data class NativeProtobufArtifacts(
        val models: ProtobufModelGeneration? = null,
    ) : GenerationTask
}
