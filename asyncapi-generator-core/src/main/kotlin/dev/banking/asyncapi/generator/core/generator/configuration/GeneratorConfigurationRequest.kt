package dev.banking.asyncapi.generator.core.generator.configuration

import dev.banking.asyncapi.generator.core.generator.model.GeneratorName
import dev.banking.asyncapi.generator.core.generator.plan.SpringKafkaClientType
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
        val springKafka: SpringKafka? = null,
        val quarkusKafka: QuarkusKafka? = null,
    )

    data class SpringKafka(
        val packageName: String? = null,
        val modelPackageName: String? = null,
        val clientType: SpringKafkaClientType = SpringKafkaClientType.SIMPLE,
        val topicPropertyPrefix: String = DEFAULT_KAFKA_TOPICS_PROPERTY_PREFIX,
    )

    data class QuarkusKafka(
        val packageName: String? = null,
        val modelPackageName: String? = null,
    )

    companion object {
        const val DEFAULT_KAFKA_TOPICS_PROPERTY_PREFIX = "kafka.topics"

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

        fun springKafka(
            enabled: Boolean? = null,
            packageName: String? = null,
            modelPackageName: String? = null,
            mode: String? = null,
            topicPropertyPrefix: String? = null,
        ): SpringKafka? =
            when {
                enabled == false -> null
                enabled == true ||
                    packageName != null ||
                    modelPackageName != null ||
                    mode != null ||
                    topicPropertyPrefix != null ->
                    SpringKafka(
                        packageName = packageName,
                        modelPackageName = modelPackageName,
                        clientType =
                            SpringKafkaClientType.fromConfigurationValue(
                                value = mode,
                                path = "clients.springKafka.mode",
                            ),
                        topicPropertyPrefix = topicPropertyPrefix ?: DEFAULT_KAFKA_TOPICS_PROPERTY_PREFIX,
                    )
                else -> null
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
