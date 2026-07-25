package dev.banking.asyncapi.generator.core.generator.configuration

import dev.banking.asyncapi.generator.core.generator.model.GeneratorName

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

        val modelType =
            request.models?.let { models ->
                ModelTypeResolver.resolve(
                    generatorName = request.generatorName,
                    configuredModelType = models.modelType,
                )
            }
        validateModelAnnotation(request, modelType)
        val protobufModels = request.protobufModels(modelType)

        return GeneratorConfiguration(
            language = GeneratorSourceLanguageResolver.resolve(request.generatorName),
            output =
                GeneratorOutputConfiguration(
                    sourceOutputDirectory = request.sourceOutputDirectory,
                    javaSourceOutputDirectory = request.javaSourceOutputDirectory,
                    resourceOutputDirectory = request.resourceOutputDirectory,
                ),
            models = request.jvmModels(modelType),
            schemas =
                buildList {
                    request.schemas.avroProjection?.packageName?.let { packageName ->
                        add(SchemaGeneration.AvroProjection(packageName))
                    }
                    val nativeAvro = request.schemas.nativeAvro
                    if (nativeAvro != null || modelType == ModelType.AVRO_SPECIFIC_RECORD) {
                        add(
                            SchemaGeneration.NativeAvro(
                                generateSpecificRecords =
                                    modelType == ModelType.AVRO_SPECIFIC_RECORD ||
                                        nativeAvro?.generateSpecificRecords == true,
                            ),
                        )
                    }
                    if (request.schemas.nativeProtobuf != null || protobufModels != null) {
                        add(
                            SchemaGeneration.NativeProtobuf(
                                models = protobufModels,
                            ),
                        )
                    }
                },
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
                                headers =
                                    ClientGeneration.Headers(
                                        enabled = kafka.headers.enabled,
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
    }

    private fun validate(request: GeneratorConfigurationRequest) {
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

    private fun validateModelAnnotation(
        request: GeneratorConfigurationRequest,
        modelType: ModelType?,
    ) {
        if (
            request.models?.annotation != null &&
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
    }

    private fun GeneratorConfigurationRequest.jvmModels(modelType: ModelType?): ModelGeneration =
        models?.packageName?.let { packageName ->
            when (modelType) {
                ModelType.KOTLIN_DATA_CLASS,
                ModelType.JAVA_CLASS,
                ->
                    ModelGeneration.Enabled(
                        packageName = packageName,
                        annotation = models.annotation,
                        javaModelType = JavaModelType.CLASS,
                    )
                ModelType.JAVA_RECORD ->
                    ModelGeneration.Enabled(
                        packageName = packageName,
                        annotation = models.annotation,
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
                        when (generatorName) {
                            GeneratorName.KOTLIN -> ProtobufModelType.KOTLIN
                            GeneratorName.JAVA -> ProtobufModelType.JAVA
                        },
                )
            }
}
