package dev.banking.asyncapi.generator.core.generator.configuration

import dev.banking.asyncapi.generator.core.generator.model.GeneratorName
import java.io.File

/**
 * Frontend-neutral generator configuration request.
 *
 * CLI, Maven, and Gradle map their public configuration surfaces into this
 * request before core generator configuration is assembled.
 *
 * Expected behavior is covered by:
 * - `GeneratorConfigurationFactoryTest`
 * - `GeneratorConfigurationRequestTest`
 */
data class GeneratorConfigurationRequest(
    val language: GeneratorName,
    val sourceOutputDirectory: File,
    val resourceOutputDirectory: File,
    val javaSourceOutputDirectory: File = sourceOutputDirectory,
    val models: Models? = null,
    val schemas: Schemas = Schemas(),
    val clients: Clients = Clients(),
) {
    data class Models(
        val packageName: String? = null,
        val annotation: String? = null,
        val javaModelType: JavaModelType = JavaModelType.CLASS,
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

    data class NativeProtobuf(
        val generateJavaMessageTypes: Boolean = true,
    )

    data class Clients(
        val kafka: Kafka? = null,
        val quarkusKafka: QuarkusKafka? = null,
    )

    data class Kafka(
        val packageName: String? = null,
        val modelPackageName: String? = null,
        val headers: KafkaHeaders = KafkaHeaders(),
        val springKafka: KafkaSpringKafka? = null,
    )

    data class KafkaHeaders(
        val enabled: Boolean = true,
    )

    data class KafkaSpringKafka(
        val producer: KafkaProducer = KafkaProducer(),
        val consumer: KafkaConsumer = KafkaConsumer(),
    )

    data class KafkaProducer(
        val enabled: Boolean = true,
    )

    data class KafkaConsumer(
        val enabled: Boolean = true,
    )

    data class QuarkusKafka(
        val packageName: String? = null,
        val modelPackageName: String? = null,
    )

    companion object {
        fun models(
            enabled: Boolean? = null,
            packageName: String? = null,
            annotation: String? = null,
            javaModelType: String? = null,
        ): Models? =
            when {
                enabled == false -> null
                enabled == true || packageName != null || annotation != null || javaModelType != null ->
                    Models(
                        packageName = packageName,
                        annotation = annotation,
                        javaModelType =
                            JavaModelType.fromConfigurationValue(
                                value = javaModelType,
                                path = "models.javaModelType",
                            ),
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
            generateJavaMessageTypes: Boolean? = null,
        ): NativeProtobuf? =
            when {
                enabled == false -> null
                enabled == true || generateJavaMessageTypes != null ->
                    NativeProtobuf(generateJavaMessageTypes = generateJavaMessageTypes ?: true)
                else -> null
            }

        fun kafka(
            enabled: Boolean? = null,
            packageName: String? = null,
            modelPackageName: String? = null,
            headers: KafkaHeaders? = null,
            springKafka: KafkaSpringKafka? = null,
        ): Kafka? =
            when {
                enabled == false -> null
                enabled == true ||
                    packageName != null ||
                    modelPackageName != null ||
                    headers != null ||
                    springKafka != null ->
                    Kafka(
                        packageName = packageName,
                        modelPackageName = modelPackageName,
                        headers = headers ?: KafkaHeaders(),
                        springKafka = springKafka,
                    )
                else -> null
            }

        fun kafkaHeaders(enabled: Boolean? = null): KafkaHeaders? =
            when (enabled) {
                null -> null
                else -> KafkaHeaders(enabled = enabled)
            }

        fun kafkaSpringKafka(
            enabled: Boolean? = null,
            producer: KafkaProducer? = null,
            consumer: KafkaConsumer? = null,
        ): KafkaSpringKafka? =
            when {
                enabled == false -> null
                enabled == true || producer != null || consumer != null ->
                    KafkaSpringKafka(
                        producer = producer ?: KafkaProducer(),
                        consumer = consumer ?: KafkaConsumer(),
                    )
                else -> null
            }

        fun kafkaProducer(enabled: Boolean? = null): KafkaProducer? =
            when (enabled) {
                null -> null
                else -> KafkaProducer(enabled = enabled)
            }

        fun kafkaConsumer(enabled: Boolean? = null): KafkaConsumer? =
            when (enabled) {
                null -> null
                else -> KafkaConsumer(enabled = enabled)
            }

        fun quarkusKafka(
            enabled: Boolean? = null,
            packageName: String? = null,
            modelPackageName: String? = null,
        ): QuarkusKafka? =
            when {
                enabled == false -> null
                enabled == true || packageName != null || modelPackageName != null ->
                    QuarkusKafka(
                        packageName = packageName,
                        modelPackageName = modelPackageName,
                    )
                else -> null
            }
    }
}
