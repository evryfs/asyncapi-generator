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
                        is ClientGeneration.SpringKafka -> {
                            require(client.topicPropertyPrefix.isNotBlank()) {
                                "topicPropertyPrefix cannot be empty"
                            }

                            if (client.clientType != SpringKafkaClientType.SIMPLE) {
                                add(
                                    GenerationTask.HeaderModelArtifacts(
                                        language = configuration.language,
                                        packageName = "${client.packageName}.header",
                                    ),
                                )
                            }
                            add(
                                GenerationTask.SpringKafkaClient(
                                    language = configuration.language,
                                    clientType = client.clientType,
                                    clientPackage = client.packageName,
                                    modelPackage = client.modelPackageName,
                                    topicPropertyPrefix = client.topicPropertyPrefix,
                                ),
                            )
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
                        SchemaGeneration.NativeProtobuf ->
                            add(GenerationTask.NativeProtobufArtifacts)
                    }
                }
            },
        )

}
