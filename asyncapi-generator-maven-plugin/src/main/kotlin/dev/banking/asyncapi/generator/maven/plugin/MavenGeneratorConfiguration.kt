package dev.banking.asyncapi.generator.maven.plugin

import dev.banking.asyncapi.generator.core.generator.configuration.GeneratorConfigurationRequest

/**
 * Maven model generation configuration.
 *
 * Expected behavior is covered by:
 * - `AsyncApiGeneratorMojoTest`
 */
class MavenModelGenerationConfiguration {
    var enabled: Boolean? = null
    var packageName: String? = null
    var annotation: String? = null
    var javaModelType: String? = null

    fun toRequest(): GeneratorConfigurationRequest.Models? =
        GeneratorConfigurationRequest.models(
            enabled = enabled,
            packageName = packageName,
            annotation = annotation,
            javaModelType = javaModelType,
        )
}

/**
 * Maven schema generation configuration.
 *
 * Expected behavior is covered by:
 * - `AsyncApiGeneratorMojoTest`
 */
class MavenSchemaGenerationConfiguration {
    var avroProjection: MavenAvroProjectionConfiguration? = null
    var nativeAvro: MavenNativeAvroConfiguration? = null
    var nativeProtobuf: MavenNativeProtobufConfiguration? = null

    fun toRequest(): GeneratorConfigurationRequest.Schemas =
        GeneratorConfigurationRequest.Schemas(
            avroProjection = avroProjection?.toRequest(),
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
    var packageName: String? = null

    fun toRequest(): GeneratorConfigurationRequest.AvroProjection? =
        GeneratorConfigurationRequest.avroProjection(
            enabled = enabled,
            packageName = packageName,
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
    var generateJavaMessageTypes: Boolean? = null

    fun toRequest(): GeneratorConfigurationRequest.NativeProtobuf? =
        GeneratorConfigurationRequest.nativeProtobuf(
            enabled = enabled,
            generateJavaMessageTypes = generateJavaMessageTypes,
        )
}

/**
 * Maven client generation configuration.
 *
 * Expected behavior is covered by:
 * - `AsyncApiGeneratorMojoTest`
 */
class MavenClientGenerationConfiguration {
    var kafka: MavenKafkaConfiguration? = null
    var quarkusKafka: MavenQuarkusKafkaConfiguration? = null

    fun toRequest(): GeneratorConfigurationRequest.Clients =
        GeneratorConfigurationRequest.Clients(
            kafka = kafka?.toRequest(),
            quarkusKafka = quarkusKafka?.toRequest(),
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
    var packageName: String? = null
    var modelPackageName: String? = null
    var headers: MavenKafkaHeadersConfiguration? = null
    var springKafka: MavenKafkaSpringKafkaConfiguration? = null

    fun toRequest(): GeneratorConfigurationRequest.Kafka? =
        GeneratorConfigurationRequest.kafka(
            enabled = enabled,
            packageName = packageName,
            modelPackageName = modelPackageName,
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
    var packageName: String? = null
    var modelPackageName: String? = null

    fun toRequest(): GeneratorConfigurationRequest.QuarkusKafka? =
        GeneratorConfigurationRequest.quarkusKafka(
            enabled = enabled,
            packageName = packageName,
            modelPackageName = modelPackageName,
        )
}
