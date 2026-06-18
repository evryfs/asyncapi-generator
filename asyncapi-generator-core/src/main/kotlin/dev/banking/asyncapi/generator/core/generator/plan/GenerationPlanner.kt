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
                when (val models = configuration.models) {
                    ModelGeneration.Disabled -> Unit
                    is ModelGeneration.Enabled ->
                        add(
                            GenerationTask.ModelArtifacts(
                                language = configuration.language,
                                packageName = models.packageName,
                                annotation = models.annotation,
                                javaModelType = models.javaModelType,
                            ),
                        )
                }

                configuration.clients.forEach { client ->
                    when (client) {
                        is ClientGeneration.Kafka -> {
                            if (client.headers.enabled) {
                                add(
                                    GenerationTask.HeaderModelArtifacts(
                                        language = configuration.language,
                                        packageName = "${client.packageName}.header",
                                    ),
                                )
                            }
                            client.springKafka?.let { springKafka ->
                                add(
                                    GenerationTask.SpringKafkaClient(
                                        language = configuration.language,
                                        clientPackage = client.packageName,
                                        modelPackage = client.modelPackageName,
                                        generateHeaders = client.headers.enabled,
                                        generateProducers = springKafka.producer.enabled,
                                        generateConsumers = springKafka.consumer.enabled,
                                    ),
                                )
                            }
                        }
                        is ClientGeneration.QuarkusKafka ->
                            add(GenerationTask.QuarkusKafkaClient(configuration.language))
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
                                ),
                            )
                        is SchemaGeneration.NativeProtobuf ->
                            add(
                                GenerationTask.NativeProtobufArtifacts(
                                    generateJavaMessageTypes = schema.generateJavaMessageTypes,
                                ),
                            )
                    }
                }
            },
        )

}
