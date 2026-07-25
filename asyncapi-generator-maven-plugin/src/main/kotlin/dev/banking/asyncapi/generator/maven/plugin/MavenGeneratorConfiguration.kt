package dev.banking.asyncapi.generator.maven.plugin

import dev.banking.asyncapi.generator.core.generator.configuration.ClientContract
import dev.banking.asyncapi.generator.core.generator.configuration.ClientType
import dev.banking.asyncapi.generator.core.generator.configuration.ClientValidationAnnotations
import dev.banking.asyncapi.generator.core.generator.configuration.GeneratorConfigurationRequest
import dev.banking.asyncapi.generator.core.generator.configuration.PackageName
import dev.banking.asyncapi.generator.core.generator.configuration.QualifiedTypeName

/**
 * Maven model generation configuration.
 *
 * Expected behavior is covered by:
 * - `AsyncApiGeneratorMojoTest`
 */
class MavenModelConfiguration {
    var modelAnnotation: String? = null
    var modelType: String? = null

    fun toRequest(modelPackage: String?): GeneratorConfigurationRequest.Models? =
        GeneratorConfigurationRequest.models(
            enabled = true,
            packageName = modelPackage,
            annotation = modelAnnotation,
            modelType = modelType,
        )
}

/**
 * Maven schema generation configuration.
 *
 * Expected behavior is covered by:
 * - `AsyncApiGeneratorMojoTest`
 */
class MavenSchemaConfiguration {
    var avroProjection: MavenAvroProjectionConfiguration? = null
    var nativeAvro: MavenNativeAvroConfiguration? = null
    var nativeProtobuf: MavenNativeProtobufConfiguration? = null

    fun toRequest(schemaPackage: String?): GeneratorConfigurationRequest.Schemas =
        GeneratorConfigurationRequest.Schemas(
            avroProjection = avroProjection?.toRequest(schemaPackage),
            nativeAvro = nativeAvro?.toRequest(),
            nativeProtobuf = nativeProtobuf?.toRequest(),
        )
}

/**
 * Maven Avro projection configuration.
 *
 * Expected behavior is covered by:
 * - `AsyncApiGeneratorMojoTest`
 */
class MavenAvroProjectionConfiguration {
    var enabled: Boolean? = null

    fun toRequest(schemaPackage: String?): GeneratorConfigurationRequest.AvroProjection? =
        GeneratorConfigurationRequest.avroProjection(
            enabled = enabled,
            packageName = schemaPackage,
        )
}

/**
 * Maven native Avro schema generation configuration.
 *
 * Expected behavior is covered by:
 * - `AsyncApiGeneratorMojoTest`
 */
class MavenNativeAvroConfiguration {
    var enabled: Boolean? = null
    var generateSpecificRecords: Boolean? = null

    fun toRequest(): GeneratorConfigurationRequest.NativeAvro? =
        GeneratorConfigurationRequest.nativeAvro(
            enabled = enabled,
            generateSpecificRecords = generateSpecificRecords,
        )
}

/**
 * Maven native Protobuf schema generation configuration.
 *
 * Expected behavior is covered by:
 * - `AsyncApiGeneratorMojoTest`
 */
class MavenNativeProtobufConfiguration {
    var enabled: Boolean? = null

    fun toRequest(): GeneratorConfigurationRequest.NativeProtobuf? =
        GeneratorConfigurationRequest.nativeProtobuf(
            enabled = enabled,
        )
}

/**
 * Maven producer generation configuration.
 *
 * Expected behavior is covered by:
 * - `AsyncApiGeneratorMojoTest`
 */
class MavenProducerConfiguration {
    var enabled: Boolean? = null

    fun toRequest(): GeneratorConfigurationRequest.KafkaProducer =
        GeneratorConfigurationRequest.KafkaProducer(
            enabled = enabled ?: true,
        )
}

/**
 * Maven consumer generation configuration.
 *
 * Expected behavior is covered by:
 * - `AsyncApiGeneratorMojoTest`
 */
class MavenConsumerConfiguration {
    var enabled: Boolean? = null

    fun toRequest(): GeneratorConfigurationRequest.KafkaConsumer =
        GeneratorConfigurationRequest.KafkaConsumer(
            enabled = enabled ?: true,
        )
}

/**
 * Maven client generation configuration.
 *
 * Expected behavior is covered by:
 * - `AsyncApiGeneratorMojoTest`
 */
class MavenClientConfiguration {
    var clientType: String? = null
    var clientContract: String? = null
    var producer: MavenProducerConfiguration? = null
    var consumer: MavenConsumerConfiguration? = null
    var topicParameterProperties: Map<String, String>? = null
    var validationAnnotations: MavenValidationAnnotationsConfiguration? = null

    fun toRequest(
        clientPackage: String?,
        modelPackage: String?,
    ): GeneratorConfigurationRequest.Clients {
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
        val resolvedClientPackage = requiredPackageName(clientPackage, "clientPackage")
        val resolvedModelPackage = requiredPackageName(modelPackage, "modelPackage")

        return when (resolvedClientType) {
            ClientType.SPRING_KAFKA ->
                GeneratorConfigurationRequest.Clients(
                    kafka =
                        GeneratorConfigurationRequest.Kafka(
                            packageName = resolvedClientPackage,
                            modelPackageName = resolvedModelPackage,
                            springKafka =
                                GeneratorConfigurationRequest.KafkaSpringKafka(
                                    clientContract = resolvedClientContract,
                                    topicParameterProperties = topicParameterProperties.orEmpty(),
                                    validationAnnotations =
                                        validationAnnotations?.toRequest()
                                            ?: ClientValidationAnnotations(),
                                    producer =
                                        producer?.toRequest()
                                            ?: GeneratorConfigurationRequest.KafkaProducer(),
                                    consumer =
                                        consumer?.toRequest()
                                            ?: GeneratorConfigurationRequest.KafkaConsumer(),
                                ),
                        ),
                )
        }
    }

    private fun requiredPackageName(
        value: String?,
        path: String,
    ): String =
        PackageName.fromConfigurationValue(
            value = value ?: throw IllegalArgumentException("$path is required when clientConfig is configured"),
            path = path,
        ).value
}

/**
 * Maven annotation configuration for generated client contracts.
 *
 * Annotation values must be fully qualified so generated imports do not depend on
 * framework or validation-library aliases maintained by the generator.
 *
 * Expected behavior is covered by:
 * - `AsyncApiGeneratorMojoTest`
 */
class MavenValidationAnnotationsConfiguration {
    var clientContract: String? = null
    var payloadParameter: String? = null

    fun toRequest(): ClientValidationAnnotations =
        ClientValidationAnnotations(
            clientContract = clientContract.toQualifiedTypeName("clientContract"),
            payloadParameter = payloadParameter.toQualifiedTypeName("payloadParameter"),
        )

    private fun String?.toQualifiedTypeName(fieldName: String): QualifiedTypeName? =
        this?.let { value ->
            QualifiedTypeName.fromConfigurationValue(
                value = value,
                path = "clientConfig.validationAnnotations.$fieldName",
            )
        }
}
