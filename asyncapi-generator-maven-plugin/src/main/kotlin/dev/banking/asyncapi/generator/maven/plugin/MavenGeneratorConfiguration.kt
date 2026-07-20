package dev.banking.asyncapi.generator.maven.plugin

import dev.banking.asyncapi.generator.core.generator.configuration.GeneratorConfigurationRequest

/**
 * Maven model generation configuration.
 *
 * Expected behavior is covered by:
 * - `AsyncApiGeneratorMojoTest`
 */
class MavenModelConfiguration {
    var modelAnnotation: String? = null
    var javaModelType: String? = null
    var protobufModelType: String? = null

    fun toRequest(modelPackage: String?): GeneratorConfigurationRequest.Models? =
        GeneratorConfigurationRequest.models(
            enabled = true,
            packageName = modelPackage,
            annotation = modelAnnotation,
            javaModelType = javaModelType,
            protobufModelType = protobufModelType,
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
 * Maven client generation configuration.
 *
 * Expected behavior is covered by:
 * - `AsyncApiGeneratorMojoTest`
 */
class MavenClientConfiguration {
    var kafka: MavenKafkaConfiguration? = null
    var quarkusKafka: MavenQuarkusKafkaConfiguration? = null

    fun toRequest(
        clientPackage: String?,
        modelPackage: String?,
    ): GeneratorConfigurationRequest.Clients =
        GeneratorConfigurationRequest.Clients(
            kafka = kafka?.toRequest(clientPackage, modelPackage),
            quarkusKafka = quarkusKafka?.toRequest(clientPackage, modelPackage),
        )
}

/**
 * Maven Kafka client configuration.
 *
 * Expected behavior is covered by:
 * - `AsyncApiGeneratorMojoTest`
 */
class MavenKafkaConfiguration {
    var enabled: Boolean? = null
    var headers: MavenKafkaHeadersConfiguration? = null
    var springKafka: MavenKafkaSpringKafkaConfiguration? = null

    fun toRequest(
        clientPackage: String?,
        modelPackage: String?,
    ): GeneratorConfigurationRequest.Kafka? =
        GeneratorConfigurationRequest.kafka(
            enabled = enabled,
            packageName = clientPackage,
            modelPackageName = modelPackage,
            headers = headers?.toRequest(),
            springKafka = springKafka?.toRequest(),
        )
}

/**
 * Maven Kafka header generation configuration.
 *
 * Expected behavior is covered by:
 * - `AsyncApiGeneratorMojoTest`
 */
class MavenKafkaHeadersConfiguration {
    var enabled: Boolean? = null

    fun toRequest(): GeneratorConfigurationRequest.KafkaHeaders? =
        GeneratorConfigurationRequest.kafkaHeaders(enabled = enabled)
}

/**
 * Maven Spring Kafka client generation configuration.
 *
 * Expected behavior is covered by:
 * - `AsyncApiGeneratorMojoTest`
 */
class MavenKafkaSpringKafkaConfiguration {
    var enabled: Boolean? = null
    var producer: MavenKafkaProducerConfiguration? = null
    var consumer: MavenKafkaConsumerConfiguration? = null

    fun toRequest(): GeneratorConfigurationRequest.KafkaSpringKafka? =
        GeneratorConfigurationRequest.kafkaSpringKafka(
            enabled = enabled ?: true,
            producer = producer?.toRequest(),
            consumer = consumer?.toRequest(),
        )
}

/**
 * Maven Spring Kafka producer generation configuration.
 *
 * Expected behavior is covered by:
 * - `AsyncApiGeneratorMojoTest`
 */
class MavenKafkaProducerConfiguration {
    var enabled: Boolean? = null

    fun toRequest(): GeneratorConfigurationRequest.KafkaProducer? =
        GeneratorConfigurationRequest.kafkaProducer(enabled = enabled)
}

/**
 * Maven Spring Kafka consumer generation configuration.
 *
 * Expected behavior is covered by:
 * - `AsyncApiGeneratorMojoTest`
 */
class MavenKafkaConsumerConfiguration {
    var enabled: Boolean? = null

    fun toRequest(): GeneratorConfigurationRequest.KafkaConsumer? =
        GeneratorConfigurationRequest.kafkaConsumer(enabled = enabled)
}

/**
 * Maven Quarkus Kafka client configuration.
 *
 * Expected behavior is covered by:
 * - `AsyncApiGeneratorMojoTest`
 */
class MavenQuarkusKafkaConfiguration {
    var enabled: Boolean? = null

    fun toRequest(
        clientPackage: String?,
        modelPackage: String?,
    ): GeneratorConfigurationRequest.QuarkusKafka? =
        GeneratorConfigurationRequest.quarkusKafka(
            enabled = enabled,
            packageName = clientPackage,
            modelPackageName = modelPackage,
        )
}
