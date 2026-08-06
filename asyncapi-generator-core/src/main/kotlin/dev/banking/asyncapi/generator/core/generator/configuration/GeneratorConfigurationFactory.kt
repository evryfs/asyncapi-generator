package dev.banking.asyncapi.generator.core.generator.configuration

import dev.banking.asyncapi.generator.core.generator.model.SourceLanguage

/**
 * Assembles core generator configuration from frontend-neutral requests.
 *
 * Expected behavior is covered by:
 * - `GeneratorConfigurationFactoryTest`
 */
object GeneratorConfigurationFactory {
    private val packageNamePattern = Regex("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)*")

    fun create(request: GeneratorConfigurationRequest): GeneratorConfiguration {
        validate(request)

        val profile = request.generatorName.profile
        val modelType =
            request.models?.let { models ->
                ModelTypeResolver.resolve(
                    generatorName = request.generatorName,
                    configuredModelType = models.modelType,
                )
            }
        val modelAnnotation = resolveModelAnnotation(request, modelType)
        val protobufModels = request.protobufModels(modelType)

        val configuration = GeneratorConfiguration(
            profile = profile,
            output =
                GeneratorOutputConfiguration(
                    sourceOutputDirectory = request.sourceOutputDirectory,
                    javaSourceOutputDirectory = request.javaSourceOutputDirectory,
                    resourceOutputDirectory = request.resourceOutputDirectory,
                    document =
                        request.outputFile?.let { outputFile ->
                            DocumentOutput(
                                file = outputFile,
                                format =
                                    when (profile) {
                                        is GeneratorProfile.Document -> profile.format
                                        is GeneratorProfile.Schema,
                                        is GeneratorProfile.Source,
                                        -> DocumentFormat.YAML
                                    },
                            )
                        },
                ),
            models = request.jvmModels(modelType, modelAnnotation),
            schemas = request.schemaGeneration(modelType, protobufModels),
            clients =
                buildList {
                    request.clients.kafka?.let { kafka ->
                        add(
                            ClientGeneration.Kafka(
                                packageName = requiredPackageName(
                                    path = "clients.kafka.packageName",
                                    value = kafka.packageName,
                                ),
                                modelPackageName = requiredClientModelPackageName(
                                    path = "clients.kafka.modelPackageName",
                                    configuredModelPackageName = kafka.modelPackageName,
                                    modelsPackageName = request.models?.packageName,
                                ),
                                springKafka =
                                    kafka.springKafka?.let { springKafka ->
                                        ClientGeneration.SpringKafka(
                                            clientContract = springKafka.clientContract,
                                            topicParameterProperties =
                                                TopicParameterProperties.fromConfigurationValues(
                                                    values = springKafka.topicParameterProperties,
                                                    path = "clients.kafka.springKafka.topicParameterProperties",
                                                ),
                                            validationAnnotations = springKafka.validationAnnotations,
                                            producer =
                                                ClientGeneration.Producer(
                                                    enabled = springKafka.producer.enabled,
                                                    payloadTypes =
                                                        ProducerPayloadType.fromConfigurationValues(
                                                            values = springKafka.producer.payloadTypes,
                                                            path =
                                                                "clients.kafka.springKafka.producer.payloadTypes",
                                                        ),
                                                ),
                                            consumer =
                                                ClientGeneration.Consumer(
                                                    enabled = springKafka.consumer.enabled,
                                                ),
                                        )
                                    },
                            ),
                        )
                    }

                    request.clients.quarkusKafka?.let { quarkusKafka ->
                        add(
                            ClientGeneration.QuarkusKafka(
                                packageName = requiredPackageName(
                                    path = "clients.quarkusKafka.packageName",
                                    value = quarkusKafka.packageName,
                                ),
                                modelPackageName = requiredClientModelPackageName(
                                    path = "clients.quarkusKafka.modelPackageName",
                                    configuredModelPackageName = quarkusKafka.modelPackageName,
                                    modelsPackageName = request.models?.packageName,
                                ),
                            ),
                        )
                    }
                },
        )

        validateActivatedOutputs(request, configuration)
        return configuration
    }

    private fun validate(request: GeneratorConfigurationRequest) {
        validateProfileConfiguration(request)

        if (request.models?.annotation != null && request.models.packageName == null) {
            throw IllegalArgumentException("models.packageName is required when models.annotation is configured")
        }

        if (request.models != null && request.models.packageName == null) {
            throw IllegalArgumentException("models.packageName is required when models are configured")
        }

        if (request.schemas.avroProjection != null && request.schemas.avroProjection.packageName == null) {
            throw IllegalArgumentException(
                "schemas.avroProjection.packageName is required when schemas.avroProjection is configured",
            )
        }

        if (request.clients.kafka != null && request.clients.kafka.packageName == null) {
            throw IllegalArgumentException(
                "clients.kafka.packageName is required when clients.kafka is configured",
            )
        }

        if (request.clients.quarkusKafka != null && request.clients.quarkusKafka.packageName == null) {
            throw IllegalArgumentException(
                "clients.quarkusKafka.packageName is required when clients.quarkusKafka is configured",
            )
        }

        validatePackageName(
            path = "models.packageName",
            value = request.models?.packageName,
        )
        validatePackageName(
            path = "schemaPackage",
            value = request.schemaPackageName,
        )
        validatePackageName(
            path = "schemas.avroProjection.packageName",
            value = request.schemas.avroProjection?.packageName,
        )
        validatePackageName(
            path = "clients.kafka.packageName",
            value = request.clients.kafka?.packageName,
        )
        validatePackageName(
            path = "clients.kafka.modelPackageName",
            value = request.clients.kafka?.modelPackageName,
        )
        validatePackageName(
            path = "clients.quarkusKafka.packageName",
            value = request.clients.quarkusKafka?.packageName,
        )
        validatePackageName(
            path = "clients.quarkusKafka.modelPackageName",
            value = request.clients.quarkusKafka?.modelPackageName,
        )
    }

    private fun validateActivatedOutputs(
        request: GeneratorConfigurationRequest,
        configuration: GeneratorConfiguration,
    ) {
        if (request.schemaPackageName != null && configuration.schemas.isEmpty()) {
            throw IllegalArgumentException(
                "schemaPackage is only supported by schema generator profiles and native Avro or Protobuf models",
            )
        }
        if (!configuration.hasConfiguredOutputs()) {
            throw IllegalArgumentException(
                "No generator output is configured. Configure modelPackage, clientPackage with clientConfig, " +
                    "schemaPackage with a schema generator, or outputFile.",
            )
        }
    }

    private fun validateProfileConfiguration(request: GeneratorConfigurationRequest) {
        when (request.generatorName.profile) {
            is GeneratorProfile.Source -> Unit
            is GeneratorProfile.Schema -> {
                if (request.models != null) {
                    throw IllegalArgumentException(
                        "models cannot be configured when generatorName is ${request.generatorName.configurationValue}",
                    )
                }
                if (request.clients != GeneratorConfigurationRequest.Clients()) {
                    throw IllegalArgumentException(
                        "clients cannot be configured when generatorName is ${request.generatorName.configurationValue}",
                    )
                }
                if (request.schemas != GeneratorConfigurationRequest.Schemas()) {
                    throw IllegalArgumentException(
                        "schema generation options cannot be configured when generatorName is " +
                            request.generatorName.configurationValue +
                            "; the generator name already selects the schema type",
                    )
                }
                if (request.schemaPackageName == null) {
                    throw IllegalArgumentException(
                        "schemaPackage is required when generatorName is ${request.generatorName.configurationValue}",
                    )
                }
            }
            is GeneratorProfile.Document -> {
                if (request.outputFile == null) {
                    throw IllegalArgumentException(
                        "outputFile is required when generatorName is ${request.generatorName.configurationValue}",
                    )
                }
                if (request.models != null) {
                    throw IllegalArgumentException(
                        "models cannot be configured when generatorName is ${request.generatorName.configurationValue}",
                    )
                }
                if (
                    request.schemaPackageName != null ||
                    request.schemas != GeneratorConfigurationRequest.Schemas()
                ) {
                    throw IllegalArgumentException(
                        "schemas cannot be configured when generatorName is ${request.generatorName.configurationValue}",
                    )
                }
                if (request.clients != GeneratorConfigurationRequest.Clients()) {
                    throw IllegalArgumentException(
                        "clients cannot be configured when generatorName is ${request.generatorName.configurationValue}",
                    )
                }
            }
        }
    }

    private fun validatePackageName(
        path: String,
        value: String?,
    ) {
        if (value == null) {
            return
        }

        if (value.isBlank()) {
            throw IllegalArgumentException("$path cannot be empty")
        }

        if (!packageNamePattern.matches(value)) {
            throw IllegalArgumentException(
                "$path must be a dot-separated package name, for example com.example.model",
            )
        }
    }

    private fun requiredPackageName(
        path: String,
        value: String?,
    ): String =
        value ?: throw IllegalArgumentException("$path is required")

    private fun requiredClientModelPackageName(
        path: String,
        configuredModelPackageName: String?,
        modelsPackageName: String?,
    ): String =
        configuredModelPackageName ?: modelsPackageName
            ?: throw IllegalArgumentException(
                "$path is required when models.packageName is not configured",
            )

    private fun resolveModelAnnotation(
        request: GeneratorConfigurationRequest,
        modelType: ModelType?,
    ): QualifiedTypeName? {
        val annotation = request.models?.annotation ?: return null
        if (
            modelType !in
            setOf(
                ModelType.KOTLIN_DATA_CLASS,
                ModelType.JAVA_CLASS,
                ModelType.JAVA_RECORD,
            )
        ) {
            throw IllegalArgumentException(
                "modelConfig.modelAnnotation is only supported for kotlin-data-class, java-class, " +
                "and java-record model types",
            )
        }

        return QualifiedTypeName.fromConfigurationValue(
            value = annotation,
            path = "modelConfig.modelAnnotation",
        )
    }

    private fun GeneratorConfigurationRequest.jvmModels(
        modelType: ModelType?,
        modelAnnotation: QualifiedTypeName?,
    ): ModelGeneration =
        models?.packageName?.let { packageName ->
            when (modelType) {
                ModelType.KOTLIN_DATA_CLASS,
                ModelType.JAVA_CLASS,
                ->
                    ModelGeneration.Enabled(
                        packageName = packageName,
                        annotation = modelAnnotation,
                        javaModelType = JavaModelType.CLASS,
                    )
                ModelType.JAVA_RECORD ->
                    ModelGeneration.Enabled(
                        packageName = packageName,
                        annotation = modelAnnotation,
                        javaModelType = JavaModelType.RECORD,
                    )
                ModelType.AVRO_SPECIFIC_RECORD,
                ModelType.PROTOBUF_MESSAGE,
                null,
                -> ModelGeneration.Disabled
            }
        } ?: ModelGeneration.Disabled

    private fun GeneratorConfigurationRequest.protobufModels(
        modelType: ModelType?,
    ): ProtobufModelGeneration? =
        models
            ?.takeIf { modelType == ModelType.PROTOBUF_MESSAGE }
            ?.packageName
            ?.let { packageName ->
                ProtobufModelGeneration(
                    packageName = packageName,
                    modelType =
                        when (GeneratorSourceLanguageResolver.resolve(generatorName)) {
                            SourceLanguage.KOTLIN -> ProtobufModelType.KOTLIN
                            SourceLanguage.JAVA -> ProtobufModelType.JAVA
                        },
                )
            }

    private fun GeneratorConfigurationRequest.schemaGeneration(
        modelType: ModelType?,
        protobufModels: ProtobufModelGeneration?,
    ): List<SchemaGeneration> =
        when (val profile = generatorName.profile) {
            is GeneratorProfile.Source ->
                buildList {
                    schemas.avroProjection?.packageName?.let { packageName ->
                        add(SchemaGeneration.AvroProjection(packageName))
                    }
                    val nativeAvro = schemas.nativeAvro
                    if (nativeAvro != null || modelType == ModelType.AVRO_SPECIFIC_RECORD) {
                        val generateSpecificRecords =
                            modelType == ModelType.AVRO_SPECIFIC_RECORD ||
                                nativeAvro?.generateSpecificRecords == true
                        add(
                            SchemaGeneration.NativeAvro(
                                generateSpecificRecords = generateSpecificRecords,
                                modelPackageName =
                                    models
                                        ?.packageName
                                        ?.takeIf { generateSpecificRecords },
                                schemaPackageName = schemaPackageName,
                            ),
                        )
                    }
                    if (schemas.nativeProtobuf != null || protobufModels != null) {
                        add(
                            SchemaGeneration.NativeProtobuf(
                                models = protobufModels,
                                schemaPackageName = schemaPackageName,
                            ),
                        )
                    }
                }
            is GeneratorProfile.Schema ->
                when (profile.type) {
                    SchemaType.AVRO ->
                        listOf(
                            SchemaGeneration.AvroProjection(
                                packageName = requireNotNull(schemaPackageName),
                            ),
                            SchemaGeneration.NativeAvro(
                                generateSpecificRecords = false,
                                schemaPackageName = schemaPackageName,
                            ),
                        )
                    SchemaType.PROTOBUF ->
                        listOf(
                            SchemaGeneration.NativeProtobuf(
                                schemaPackageName = schemaPackageName,
                            ),
                        )
                    SchemaType.JSON_SCHEMA ->
                        listOf(
                            SchemaGeneration.JsonSchema(
                                packageName = requireNotNull(schemaPackageName),
                            ),
                        )
                }
            is GeneratorProfile.Document -> emptyList()
        }
}
