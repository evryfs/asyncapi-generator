package dev.banking.asyncapi.generator.core.generator.plan

import dev.banking.asyncapi.generator.core.generator.configuration.ClientGeneration
import dev.banking.asyncapi.generator.core.generator.configuration.GeneratorConfiguration
import dev.banking.asyncapi.generator.core.generator.configuration.ModelGeneration
import dev.banking.asyncapi.generator.core.generator.configuration.SchemaGeneration

/**
 * Creates an ordered generation plan from generator options.
 *
 * Expected behavior is covered by:
 * - `GenerationPlannerTest`
 */
class GenerationPlanner {
    fun plan(configuration: GeneratorConfiguration): GenerationPlan =
        GenerationPlan(
            buildList {
                val keyModelPackages = mutableSetOf<String>()
                val hasNativePayloadModels = configuration.hasNativePayloadModels()

                configuration.output.document?.let { document ->
                    add(
                        GenerationTask.DocumentArtifact(
                            file = document.file,
                            format = document.format,
                        ),
                    )
                }

                when (val models = configuration.models) {
                    ModelGeneration.Disabled -> Unit
                    is ModelGeneration.Enabled ->
                        add(
                            GenerationTask.ModelArtifacts(
                                language = configuration.requireSourceLanguage(),
                                packageName = models.packageName,
                                annotation = models.annotation,
                                javaModelType = models.javaModelType,
                            ),
                        )
                }

                configuration.clients.forEach { client ->
                    when (client) {
                        is ClientGeneration.Kafka -> {
                            val springKafka = client.springKafka?.takeIf { it.hasEnabledOutput() }
                            if (
                                hasNativePayloadModels &&
                                springKafka != null &&
                                keyModelPackages.add(client.modelPackageName)
                            ) {
                                add(
                                    GenerationTask.KafkaKeyModelArtifacts(
                                        language = configuration.requireSourceLanguage(),
                                        packageName = client.modelPackageName,
                                    ),
                                )
                            }
                            if (client.headers.enabled) {
                                add(
                                    GenerationTask.HeaderModelArtifacts(
                                        language = configuration.requireSourceLanguage(),
                                        packageName = "${client.packageName}.header",
                                    ),
                                )
                            }
                            springKafka?.let {
                                add(
                                    GenerationTask.SpringKafkaClient(
                                        language = configuration.requireSourceLanguage(),
                                        clientPackage = client.packageName,
                                        modelPackage = client.modelPackageName,
                                        generateHeaders = client.headers.enabled,
                                        generateProducers = springKafka.producer.enabled,
                                        generateConsumers = springKafka.consumer.enabled,
                                        clientContract = springKafka.clientContract,
                                        topicParameterProperties = springKafka.topicParameterProperties,
                                        validationAnnotations = springKafka.validationAnnotations,
                                    ),
                                )
                            }
                        }
                        is ClientGeneration.QuarkusKafka ->
                            add(GenerationTask.QuarkusKafkaClient(configuration.requireSourceLanguage()))
                    }
                }

                configuration.schemas.forEach { schema ->
                    when (schema) {
                        is SchemaGeneration.AvroProjection ->
                            add(GenerationTask.AvroSchemaArtifacts(schema.packageName))
                        is SchemaGeneration.NativeAvro ->
                            add(
                                GenerationTask.NativeAvroArtifacts(
                                    generateSpecificRecords = schema.generateSpecificRecords,
                                    schemaPackageName = schema.schemaPackageName,
                                ),
                            )
                        is SchemaGeneration.NativeProtobuf ->
                            add(
                                GenerationTask.NativeProtobufArtifacts(
                                    models = schema.models,
                                    schemaPackageName = schema.schemaPackageName,
                                ),
                            )
                        is SchemaGeneration.JsonSchema ->
                            add(GenerationTask.JsonSchemaArtifacts(schema.packageName))
                    }
                }
            },
        )

    private fun ClientGeneration.SpringKafka.hasEnabledOutput(): Boolean =
        producer.enabled || consumer.enabled

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
