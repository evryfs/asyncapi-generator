package dev.banking.asyncapi.generator.core.generator.plan

import dev.banking.asyncapi.generator.core.generator.configuration.GeneratorConfiguration
import dev.banking.asyncapi.generator.core.generator.configuration.ModelGeneration
import dev.banking.asyncapi.generator.core.generator.configuration.SchemaGeneration

/**
 * Creates an ordered generation plan from generator options.
 *
 * Tasks refer to the configuration values they plan. The planner decides only
 * the order (document, models, Kafka key models, client, schemas), the source
 * language of client tasks, and whether Kafka key models are needed: they are
 * when a client uses native Avro or Protobuf payload models, which do not
 * cover the message keys.
 */
class GenerationPlanner {
    /** The tasks [configuration] asks for, in render order. */
    fun plan(configuration: GeneratorConfiguration): GenerationPlan =
        GenerationPlan(
            buildList {
                val hasNativePayloadModels = configuration.hasNativePayloadModels()

                configuration.output.document?.let { add(GenerationTask.DocumentArtifact(it)) }
                (configuration.models as? ModelGeneration.Enabled)?.let { add(GenerationTask.ModelArtifacts(it)) }
                configuration.client?.let { client ->
                    val language = configuration.requireSourceLanguage()
                    if (hasNativePayloadModels) {
                        add(GenerationTask.KafkaKeyModelArtifacts(language, client.modelPackageName))
                    }
                    add(GenerationTask.SpringKafkaClient(language, client))
                }
                configuration.schemas.forEach { schema ->
                    add(
                        when (schema) {
                            is SchemaGeneration.AvroProjection -> GenerationTask.AvroSchemaArtifacts(schema)
                            is SchemaGeneration.NativeAvro -> GenerationTask.NativeAvroArtifacts(schema)
                            is SchemaGeneration.NativeProtobuf -> GenerationTask.NativeProtobufArtifacts(schema)
                            is SchemaGeneration.JsonSchema -> GenerationTask.JsonSchemaArtifacts(schema)
                        },
                    )
                }
            },
        )

    private fun GeneratorConfiguration.hasNativePayloadModels(): Boolean =
        schemas.any { schema ->
            when (schema) {
                is SchemaGeneration.NativeAvro -> schema.generateSpecificRecords
                is SchemaGeneration.NativeProtobuf -> schema.models != null
                is SchemaGeneration.AvroProjection,
                is SchemaGeneration.JsonSchema,
                -> false
            }
        }

    private fun GeneratorConfiguration.requireSourceLanguage() =
        requireNotNull(sourceLanguage) {
            "Source language is required for model and client generation"
        }
}
