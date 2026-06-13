package dev.banking.asyncapi.generator.core.generator.plan

import dev.banking.asyncapi.generator.core.generator.model.GeneratorName
import dev.banking.asyncapi.generator.core.generator.configuration.JavaModelType

/**
 * Planned generator work item.
 *
 * Expected behavior is covered by:
 * - `GenerationPlannerTest`
 */
sealed interface GenerationTask {
    data class ModelArtifacts(
        val language: GeneratorName,
        val packageName: String,
        val annotation: String? = null,
        val javaModelType: JavaModelType = JavaModelType.CLASS,
    ) : GenerationTask

    data class HeaderModelArtifacts(
        val language: GeneratorName,
        val packageName: String,
    ) : GenerationTask

    data class SpringKafkaClient(
        val language: GeneratorName,
        val clientType: SpringKafkaClientType,
        val clientPackage: String,
        val modelPackage: String,
        val topicPropertyPrefix: String,
    ) : GenerationTask

    data class QuarkusKafkaClient(
        val language: GeneratorName,
    ) : GenerationTask

    data class AvroSchemaArtifacts(
        val packageName: String,
    ) : GenerationTask

    data class NativeAvroArtifacts(
        val generateSpecificRecords: Boolean = true,
    ) : GenerationTask

    data class NativeProtobufArtifacts(
        val generateJavaMessageTypes: Boolean = true,
    ) : GenerationTask
}
