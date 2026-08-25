package dev.banking.asyncapi.generator.core.generator.configuration

import dev.banking.asyncapi.generator.core.generator.model.GeneratorName
import java.io.File

/**
 * Frontend-neutral generator configuration request.
 *
 * CLI, Maven, and Gradle map their public configuration surfaces into this
 * request before core generator configuration is assembled.
 */
data class GeneratorConfigurationRequest(
    val generatorName: GeneratorName,
    val sourceOutputDirectory: File,
    val resourceOutputDirectory: File,
    val javaSourceOutputDirectory: File = sourceOutputDirectory,
    val outputFile: File? = null,
    val schemaPackageName: String? = null,
    val models: Models? = null,
    val schemas: Schemas = Schemas(),
    val clients: Clients = Clients(),
) {
    data class Models(
        val packageName: String? = null,
        val annotation: String? = null,
        val modelType: ModelType? = null,
    )

    data class Schemas(
        val avroProjection: AvroProjection? = null,
        val nativeAvro: NativeAvro? = null,
        val nativeProtobuf: NativeProtobuf? = null,
    )

    data class AvroProjection(
        val packageName: String? = null,
    )

    data class NativeAvro(
        val generateSpecificRecords: Boolean = true,
    )

    data object NativeProtobuf

    data class Clients(
        val kafka: Kafka? = null,
    )

    data class Kafka(
        val packageName: String? = null,
        val modelPackageName: String? = null,
        val springKafka: KafkaSpringKafka? = null,
    )

    data class KafkaSpringKafka(
        val clientContract: ClientContract = ClientContract.INTERFACE,
        val topicParameterProperties: Map<String, String> = emptyMap(),
        val validationAnnotations: ClientValidationAnnotations = ClientValidationAnnotations(),
        val producer: KafkaProducer = KafkaProducer(),
        val consumer: KafkaConsumer = KafkaConsumer(),
    )

    data class KafkaProducer(
        val enabled: Boolean = true,
        val additionalPayloadTypes: List<String>? = null,
    )

    data class KafkaConsumer(
        val enabled: Boolean = true,
    )

    companion object {
        fun models(
            enabled: Boolean? = null,
            packageName: String? = null,
            annotation: String? = null,
            modelType: String? = null,
        ): Models? =
            when {
                enabled == false -> null
                enabled == true ||
                    packageName != null ||
                    annotation != null ||
                    modelType != null ->
                    Models(
                        packageName = packageName,
                        annotation = annotation,
                        modelType =
                            modelType?.let {
                                ModelType.fromConfigurationValue(
                                    value = it,
                                    path = "modelConfig.modelType",
                                )
                            },
                    )
                else -> null
            }

        fun avroProjection(
            enabled: Boolean? = null,
            packageName: String? = null,
        ): AvroProjection? =
            when {
                enabled == false -> null
                enabled == true || packageName != null ->
                    AvroProjection(packageName = packageName)
                else -> null
            }

        fun nativeAvro(
            enabled: Boolean? = null,
            generateSpecificRecords: Boolean? = null,
        ): NativeAvro? =
            when {
                enabled == false -> null
                enabled == true || generateSpecificRecords != null ->
                    NativeAvro(generateSpecificRecords = generateSpecificRecords ?: true)
                else -> null
            }

        fun nativeProtobuf(
            enabled: Boolean? = null,
        ): NativeProtobuf? =
            when {
                enabled == false -> null
                enabled == true -> NativeProtobuf
                else -> null
            }

        fun kafka(
            enabled: Boolean? = null,
            packageName: String? = null,
            modelPackageName: String? = null,
            springKafka: KafkaSpringKafka? = null,
        ): Kafka? =
            when {
                enabled == false -> null
                enabled == true ||
                    packageName != null ||
                    modelPackageName != null ||
                    springKafka != null ->
                    Kafka(
                        packageName = packageName,
                        modelPackageName = modelPackageName,
                        springKafka = springKafka,
                    )
                else -> null
            }

        fun kafkaSpringKafka(
            enabled: Boolean? = null,
            clientContract: ClientContract = ClientContract.INTERFACE,
            topicParameterProperties: Map<String, String> = emptyMap(),
            validationAnnotations: ClientValidationAnnotations = ClientValidationAnnotations(),
            producer: KafkaProducer? = null,
            consumer: KafkaConsumer? = null,
        ): KafkaSpringKafka? =
            when {
                enabled == false -> null
                enabled == true ||
                    topicParameterProperties.isNotEmpty() ||
                    validationAnnotations != ClientValidationAnnotations() ||
                    producer != null ||
                    consumer != null ->
                    KafkaSpringKafka(
                        clientContract = clientContract,
                        topicParameterProperties = topicParameterProperties,
                        validationAnnotations = validationAnnotations,
                        producer = producer ?: KafkaProducer(),
                        consumer = consumer ?: KafkaConsumer(),
                    )
                else -> null
            }

        fun kafkaProducer(
            enabled: Boolean? = null,
            additionalPayloadTypes: List<String>? = null,
        ): KafkaProducer? =
            when {
                enabled == null && additionalPayloadTypes == null -> null
                else ->
                    KafkaProducer(
                        enabled = enabled ?: true,
                        additionalPayloadTypes = additionalPayloadTypes,
                    )
            }

        fun kafkaConsumer(enabled: Boolean? = null): KafkaConsumer? =
            when (enabled) {
                null -> null
                else -> KafkaConsumer(enabled = enabled)
            }

        fun clients(
            clientType: String?,
            clientContract: String?,
            clientPackage: String?,
            modelPackage: String?,
            producerEnabled: Boolean? = null,
            producerAdditionalPayloadTypes: List<String>? = null,
            consumerEnabled: Boolean? = null,
            topicParameterProperties: Map<String, String> = emptyMap(),
            validationClientContract: String? = null,
            validationPayloadParameter: String? = null,
        ): Clients {
            val resolvedClientType =
                ClientType.fromConfigurationValue(
                    value = clientType,
                    path = "clientConfig.clientType",
                )
            val resolvedClientContract =
                ClientContract.fromConfigurationValue(
                    value = clientContract,
                    path = "clientConfig.clientContract",
                )
            val resolvedClientPackage = requiredClientPackage(clientPackage, "clientPackage")
            val resolvedModelPackage = requiredClientPackage(modelPackage, "modelPackage")
            val validationAnnotations =
                ClientValidationAnnotations(
                    clientContract =
                        validationClientContract.toQualifiedTypeName(
                            "clientConfig.validationAnnotations.clientContract",
                        ),
                    payloadParameter =
                        validationPayloadParameter.toQualifiedTypeName(
                            "clientConfig.validationAnnotations.payloadParameter",
                        ),
                )

            return when (resolvedClientType) {
                ClientType.SPRING_KAFKA ->
                    Clients(
                        kafka =
                            Kafka(
                                packageName = resolvedClientPackage,
                                modelPackageName = resolvedModelPackage,
                                springKafka =
                                    KafkaSpringKafka(
                                        clientContract = resolvedClientContract,
                                        topicParameterProperties = topicParameterProperties,
                                        validationAnnotations = validationAnnotations,
                                        producer =
                                            KafkaProducer(
                                                enabled = producerEnabled ?: true,
                                                additionalPayloadTypes = producerAdditionalPayloadTypes,
                                            ),
                                        consumer = KafkaConsumer(enabled = consumerEnabled ?: true),
                                    ),
                            ),
                    )
            }
        }

        private fun requiredClientPackage(
            value: String?,
            path: String,
        ): String =
            PackageName.fromConfigurationValue(
                value = value ?: throw IllegalArgumentException("$path is required when clientConfig is configured"),
                path = path,
            ).value

        private fun String?.toQualifiedTypeName(path: String): QualifiedTypeName? =
            this?.let { value ->
                QualifiedTypeName.fromConfigurationValue(
                    value = value,
                    path = path,
                )
            }
    }
}
