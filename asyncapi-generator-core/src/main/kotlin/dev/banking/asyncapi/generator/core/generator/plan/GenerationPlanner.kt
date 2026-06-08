package dev.banking.asyncapi.generator.core.generator.plan

import dev.banking.asyncapi.generator.core.generator.model.GeneratorOptions

/**
 * Creates an ordered generation plan from generator options.
 *
 * Expected behavior is covered by:
 * - `GenerationPlannerTest`
 */
class GenerationPlanner {
    fun plan(generatorOptions: GeneratorOptions): GenerationPlan =
        GenerationPlan(
            buildList {
                if (generatorOptions.generateModels) {
                    add(
                        GenerationTask.ModelArtifacts(
                            language = generatorOptions.generatorName,
                            packageName = generatorOptions.modelPackage,
                            annotation = generatorOptions.configOptions["model.annotation"],
                        ),
                    )
                }

                if (generatorOptions.generateSpringKafkaClient) {
                    require(generatorOptions.kafkaTopicsPropertyPrefix.isNotBlank()) {
                        "kafka.topics.property.prefix cannot be empty"
                    }

                    val clientType = generatorOptions.springKafkaClientType()
                    if (clientType != SpringKafkaClientType.SIMPLE) {
                        add(
                            GenerationTask.HeaderModelArtifacts(
                                language = generatorOptions.generatorName,
                                packageName = "${generatorOptions.clientPackage}.header",
                            ),
                        )
                    }
                    add(
                        GenerationTask.SpringKafkaClient(
                            language = generatorOptions.generatorName,
                            clientType = clientType,
                            clientPackage = generatorOptions.clientPackage,
                            modelPackage = generatorOptions.modelPackage,
                            topicPropertyPrefix = generatorOptions.kafkaTopicsPropertyPrefix,
                        ),
                    )
                }

                if (generatorOptions.generateQuarkusKafkaClient) {
                    add(GenerationTask.QuarkusKafkaClient(generatorOptions.generatorName))
                }

                if (generatorOptions.generateAvroSchema) {
                    add(GenerationTask.AvroSchemaArtifacts(generatorOptions.schemaPackage))
                }
            },
        )

    private fun GeneratorOptions.springKafkaClientType(): SpringKafkaClientType =
        SpringKafkaClientType.fromConfigValue(configOptions["client.type"])
}
