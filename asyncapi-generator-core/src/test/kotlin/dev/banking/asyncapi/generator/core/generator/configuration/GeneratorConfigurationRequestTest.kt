package dev.banking.asyncapi.generator.core.generator.configuration

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class GeneratorConfigurationRequestTest {
    @Test
    fun `models request is created only when model output is configured`() {
        assertNull(GeneratorConfigurationRequest.models())
        assertNull(
            GeneratorConfigurationRequest.models(
                enabled = false,
                packageName = "com.example.model",
                annotation = "com.example.NoArg",
                modelType = "java-record",
            ),
        )

        assertEquals(
            GeneratorConfigurationRequest.Models(
                packageName = "com.example.model",
                annotation = "com.example.NoArg",
                modelType = ModelType.JAVA_RECORD,
            ),
            GeneratorConfigurationRequest.models(
                packageName = "com.example.model",
                annotation = "com.example.NoArg",
                modelType = "java-record",
            ),
        )
    }

    @Test
    fun `avro projection request is created only when schema output is configured`() {
        assertNull(GeneratorConfigurationRequest.avroProjection())
        assertNull(
            GeneratorConfigurationRequest.avroProjection(
                enabled = false,
                packageName = "com.example.schema",
            ),
        )

        assertEquals(
            GeneratorConfigurationRequest.AvroProjection(packageName = "com.example.schema"),
            GeneratorConfigurationRequest.avroProjection(packageName = "com.example.schema"),
        )
    }

    @Test
    fun `native avro request is created only when schema output is configured`() {
        assertNull(GeneratorConfigurationRequest.nativeAvro())
        assertNull(
            GeneratorConfigurationRequest.nativeAvro(
                enabled = false,
                generateSpecificRecords = true,
            ),
        )

        assertEquals(
            GeneratorConfigurationRequest.NativeAvro(generateSpecificRecords = true),
            GeneratorConfigurationRequest.nativeAvro(enabled = true),
        )
        assertEquals(
            GeneratorConfigurationRequest.NativeAvro(generateSpecificRecords = false),
            GeneratorConfigurationRequest.nativeAvro(generateSpecificRecords = false),
        )
    }

    @Test
    fun `native protobuf request is created only when schema output is configured`() {
        assertNull(GeneratorConfigurationRequest.nativeProtobuf())
        assertNull(GeneratorConfigurationRequest.nativeProtobuf(enabled = false))

        assertEquals(
            GeneratorConfigurationRequest.NativeProtobuf,
            GeneratorConfigurationRequest.nativeProtobuf(enabled = true),
        )
    }

    @Test
    fun `kafka request is created only when client output is configured`() {
        assertNull(GeneratorConfigurationRequest.kafka())
        assertNull(
            GeneratorConfigurationRequest.kafka(
                enabled = false,
                packageName = "com.example.client",
                modelPackageName = "com.example.model",
                springKafka = GeneratorConfigurationRequest.KafkaSpringKafka(),
            ),
        )

        assertEquals(
            GeneratorConfigurationRequest.Kafka(
                packageName = "com.example.client",
                modelPackageName = "com.example.model",
                springKafka = GeneratorConfigurationRequest.KafkaSpringKafka(),
            ),
            GeneratorConfigurationRequest.kafka(
                packageName = "com.example.client",
                modelPackageName = "com.example.model",
                springKafka = GeneratorConfigurationRequest.KafkaSpringKafka(),
            ),
        )
    }

    @Test
    fun `kafka request can be created from package only`() {
        assertEquals(
            GeneratorConfigurationRequest.Kafka(
                packageName = "com.example.client",
            ),
            GeneratorConfigurationRequest.kafka(packageName = "com.example.client"),
        )
    }

    @Test
    fun `spring kafka request is created only when kafka spring kafka output is configured`() {
        assertNull(GeneratorConfigurationRequest.kafkaSpringKafka())
        assertNull(
            GeneratorConfigurationRequest.kafkaSpringKafka(
                enabled = false,
                producer = GeneratorConfigurationRequest.KafkaProducer(enabled = false),
                consumer = GeneratorConfigurationRequest.KafkaConsumer(enabled = false),
            ),
        )

        assertEquals(
            GeneratorConfigurationRequest.KafkaSpringKafka(),
            GeneratorConfigurationRequest.kafkaSpringKafka(enabled = true),
        )
        assertEquals(
            GeneratorConfigurationRequest.KafkaSpringKafka(
                topicParameterProperties = mapOf("environment" to "kafka.environment"),
            ),
            GeneratorConfigurationRequest.kafkaSpringKafka(
                topicParameterProperties = mapOf("environment" to "kafka.environment"),
            ),
        )

        assertEquals(
            GeneratorConfigurationRequest.KafkaSpringKafka(
                producer = GeneratorConfigurationRequest.KafkaProducer(enabled = false),
            ),
            GeneratorConfigurationRequest.kafkaSpringKafka(
                producer = GeneratorConfigurationRequest.KafkaProducer(enabled = false),
            ),
        )

        val validationAnnotations =
            ClientValidationAnnotations(
                payloadParameter =
                    QualifiedTypeName.fromConfigurationValue(
                        value = "jakarta.validation.Valid",
                        path = "clients.kafka.springKafka.validationAnnotations.payloadParameter",
                    ),
            )
        assertEquals(
            GeneratorConfigurationRequest.KafkaSpringKafka(
                validationAnnotations = validationAnnotations,
            ),
            GeneratorConfigurationRequest.kafkaSpringKafka(
                validationAnnotations = validationAnnotations,
            ),
        )
    }

    @Test
    fun `producer request preserves omitted and configured additional payload types`() {
        assertNull(GeneratorConfigurationRequest.kafkaProducer())
        assertEquals(
            GeneratorConfigurationRequest.KafkaProducer(
                additionalPayloadTypes = listOf("byte-array", "string"),
            ),
            GeneratorConfigurationRequest.kafkaProducer(
                additionalPayloadTypes = listOf("byte-array", "string"),
            ),
        )
    }

    @Test
    fun `clients request resolves Spring Kafka configuration values`() {
        assertEquals(
            GeneratorConfigurationRequest.Clients(
                kafka =
                    GeneratorConfigurationRequest.Kafka(
                        packageName = "com.example.client",
                        modelPackageName = "com.example.model",
                        springKafka =
                            GeneratorConfigurationRequest.KafkaSpringKafka(
                                clientContract = ClientContract.INTERFACE,
                                topicParameterProperties =
                                    mapOf(
                                        "environment" to "kafka.environment",
                                    ),
                                validationAnnotations =
                                    ClientValidationAnnotations(
                                        clientContract =
                                            QualifiedTypeName.fromConfigurationValue(
                                                value = "org.springframework.validation.annotation.Validated",
                                                path = "clientConfig.validationAnnotations.clientContract",
                                            ),
                                        payloadParameter =
                                            QualifiedTypeName.fromConfigurationValue(
                                                value = "jakarta.validation.Valid",
                                                path = "clientConfig.validationAnnotations.payloadParameter",
                                            ),
                                    ),
                                producer =
                                    GeneratorConfigurationRequest.KafkaProducer(
                                        enabled = false,
                                        additionalPayloadTypes = listOf("byte-array"),
                                    ),
                                consumer = GeneratorConfigurationRequest.KafkaConsumer(enabled = true),
                            ),
                    ),
            ),
            GeneratorConfigurationRequest.clients(
                clientType = "spring-kafka",
                clientContract = "interface",
                clientPackage = "com.example.client",
                modelPackage = "com.example.model",
                producerEnabled = false,
                producerAdditionalPayloadTypes = listOf("byte-array"),
                topicParameterProperties =
                    mapOf(
                        "environment" to "kafka.environment",
                    ),
                validationClientContract = "org.springframework.validation.annotation.Validated",
                validationPayloadParameter = "jakarta.validation.Valid",
            ),
        )
    }

    @Test
    fun `clients request requires client and model packages`() {
        val missingClientPackage =
            assertFailsWith<IllegalArgumentException> {
                GeneratorConfigurationRequest.clients(
                    clientType = "spring-kafka",
                    clientContract = "interface",
                    clientPackage = null,
                    modelPackage = "com.example.model",
                )
            }
        assertEquals(
            "clientPackage is required when clientConfig is configured",
            missingClientPackage.message,
        )

        val missingModelPackage =
            assertFailsWith<IllegalArgumentException> {
                GeneratorConfigurationRequest.clients(
                    clientType = "spring-kafka",
                    clientContract = "interface",
                    clientPackage = "com.example.client",
                    modelPackage = null,
                )
            }
        assertEquals(
            "modelPackage is required when clientConfig is configured",
            missingModelPackage.message,
        )
    }

    @Test
    fun `quarkus kafka request is created only when client output is configured`() {
        assertNull(GeneratorConfigurationRequest.quarkusKafka())
        assertNull(
            GeneratorConfigurationRequest.quarkusKafka(
                enabled = false,
                packageName = "com.example.client",
                modelPackageName = "com.example.model",
            ),
        )

        assertEquals(
            GeneratorConfigurationRequest.QuarkusKafka(
                packageName = "com.example.client",
                modelPackageName = "com.example.model",
            ),
            GeneratorConfigurationRequest.quarkusKafka(
                packageName = "com.example.client",
                modelPackageName = "com.example.model",
            ),
        )
    }
}
