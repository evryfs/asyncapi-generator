package dev.banking.asyncapi.generator.core.generator.plan

import dev.banking.asyncapi.generator.core.generator.configuration.GeneratorConfiguration
import dev.banking.asyncapi.generator.core.generator.configuration.JavaModelType
import dev.banking.asyncapi.generator.core.generator.configuration.ModelGeneration
import dev.banking.asyncapi.generator.core.generator.configuration.ModelType
import dev.banking.asyncapi.generator.core.generator.configuration.SchemaGeneration

/**
 * Creates an ordered generation plan from generator options.
 */
class GenerationPlanner {
    fun plan(configuration: GeneratorConfiguration): GenerationPlan =
        GenerationPlan(
            buildList {
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
                                javaModelType = models.modelType.javaModelType(),
                            ),
                        )
                }

                configuration.client?.let { client ->
                    if (hasNativePayloadModels) {
                        add(
                            GenerationTask.KafkaKeyModelArtifacts(
                                language = configuration.requireSourceLanguage(),
                                packageName = client.modelPackageName,
                            ),
                        )
                    }
                    add(
                        GenerationTask.SpringKafkaClient(
                            language = configuration.requireSourceLanguage(),
                            clientPackage = client.packageName,
                            modelPackage = client.modelPackageName,
                            generateProducers = client.producer.enabled,
                            additionalPayloadTypes = client.producer.additionalPayloadTypes,
                            generateConsumers = client.consumer.enabled,
                            clientContract = client.clientContract,
                            topicParameterProperties = client.topicParameterProperties,
                            validationAnnotations = client.validationAnnotations,
                        ),
                    )
                }

                configuration.schemas.forEach { schema ->
                    when (schema) {
                        is SchemaGeneration.AvroProjection ->
                            add(GenerationTask.AvroSchemaArtifacts(schema.packageName))
                        is SchemaGeneration.NativeAvro ->
                            add(
                                GenerationTask.NativeAvroArtifacts(
                                    generateSpecificRecords = schema.generateSpecificRecords,
                                    modelPackageName = schema.modelPackageName,
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

    private fun ModelType.javaModelType(): JavaModelType =
        if (this == ModelType.JAVA_RECORD) JavaModelType.RECORD else JavaModelType.CLASS

    private fun GeneratorConfiguration.requireSourceLanguage() =
        requireNotNull(sourceLanguage) {
            "Source language is required for model and client generation"
        }
}
