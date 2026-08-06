package dev.banking.asyncapi.generator.core.generator.kafka.spring

import dev.banking.asyncapi.generator.core.fixtures.GeneratorApprovalFormat
import dev.banking.asyncapi.generator.core.fixtures.GeneratorApprovals
import dev.banking.asyncapi.generator.core.fixtures.SpringKafkaClientOutputFixtures
import dev.banking.asyncapi.generator.core.fixtures.SpringKafkaClientOutputFixtures.Companion.SINGLE_MESSAGE_CONTRACT
import dev.banking.asyncapi.generator.core.fixtures.SpringKafkaClientOutputFixtures.Companion.THREE_MESSAGE_CONTRACT
import dev.banking.asyncapi.generator.core.generator.configuration.JavaModelType
import dev.banking.asyncapi.generator.core.generator.configuration.AdditionalProducerPayloadType
import dev.banking.asyncapi.generator.core.generator.model.SourceLanguage
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class SpringKafkaClientApprovalTest {
    private val generatedClients = SpringKafkaClientOutputFixtures()

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun approves_kotlin_single_message_producer_and_consumer_contracts() {
        val contracts =
            generatedClients.generate(
                contractPath = SINGLE_MESSAGE_CONTRACT,
                language = SourceLanguage.KOTLIN,
                outputDirectory = tempDir.resolve("kotlin-single-message"),
            )

        assertAll(
            {
                GeneratorApprovals.verify(
                    generated = contracts.singleProducer(),
                    format = GeneratorApprovalFormat.SPRING_KAFKA_KOTLIN,
                    scenario = "single-message-producer",
                )
            },
            {
                GeneratorApprovals.verify(
                    generated = contracts.singleConsumer(),
                    format = GeneratorApprovalFormat.SPRING_KAFKA_KOTLIN,
                    scenario = "single-message-consumer",
                )
            },
        )
    }

    @Test
    fun approves_kotlin_three_message_producer_and_consumer_contracts() {
        val contracts =
            generatedClients.generate(
                contractPath = THREE_MESSAGE_CONTRACT,
                language = SourceLanguage.KOTLIN,
                outputDirectory = tempDir.resolve("kotlin-three-message"),
            )

        assertAll(
            {
                GeneratorApprovals.verify(
                    generated = contracts.singleProducer(),
                    format = GeneratorApprovalFormat.SPRING_KAFKA_KOTLIN,
                    scenario = "three-message-producer",
                )
            },
            {
                GeneratorApprovals.verify(
                    generated = contracts.singleConsumer(),
                    format = GeneratorApprovalFormat.SPRING_KAFKA_KOTLIN,
                    scenario = "three-message-consumer",
                )
            },
        )
    }

    @Test
    fun approves_java_single_message_producer_and_consumer_contracts() {
        val contracts =
            generatedClients.generate(
                contractPath = SINGLE_MESSAGE_CONTRACT,
                language = SourceLanguage.JAVA,
                outputDirectory = tempDir.resolve("java-single-message"),
            )

        assertAll(
            {
                GeneratorApprovals.verify(
                    generated = contracts.singleProducer(),
                    format = GeneratorApprovalFormat.SPRING_KAFKA_JAVA,
                    scenario = "single-message-producer",
                )
            },
            {
                GeneratorApprovals.verify(
                    generated = contracts.singleConsumer(),
                    format = GeneratorApprovalFormat.SPRING_KAFKA_JAVA,
                    scenario = "single-message-consumer",
                )
            },
        )
    }

    @Test
    fun approves_java_three_message_producer_and_consumer_contracts() {
        val contracts =
            generatedClients.generate(
                contractPath = THREE_MESSAGE_CONTRACT,
                language = SourceLanguage.JAVA,
                outputDirectory = tempDir.resolve("java-three-message"),
            )

        assertAll(
            {
                GeneratorApprovals.verify(
                    generated = contracts.singleProducer(),
                    format = GeneratorApprovalFormat.SPRING_KAFKA_JAVA,
                    scenario = "three-message-producer",
                )
            },
            {
                GeneratorApprovals.verify(
                    generated = contracts.singleConsumer(),
                    format = GeneratorApprovalFormat.SPRING_KAFKA_JAVA,
                    scenario = "three-message-consumer",
                )
            },
        )
    }

    @Test
    fun approves_kotlin_producer_with_all_additional_payload_types() {
        val contracts =
            generatedClients.generate(
                contractPath = SINGLE_MESSAGE_CONTRACT,
                language = SourceLanguage.KOTLIN,
                outputDirectory = tempDir.resolve("kotlin-producer-payload-types"),
                additionalPayloadTypes = AdditionalProducerPayloadType.entries.toSet(),
            )

        GeneratorApprovals.verify(
            generated = contracts.singleProducer(),
            format = GeneratorApprovalFormat.SPRING_KAFKA_KOTLIN,
            scenario = "additional-payload-types-producer",
        )
    }

    @Test
    fun approves_java_producer_with_all_additional_payload_types() {
        val contracts =
            generatedClients.generate(
                contractPath = SINGLE_MESSAGE_CONTRACT,
                language = SourceLanguage.JAVA,
                outputDirectory = tempDir.resolve("java-producer-payload-types"),
                additionalPayloadTypes = AdditionalProducerPayloadType.entries.toSet(),
            )

        GeneratorApprovals.verify(
            generated = contracts.singleProducer(),
            format = GeneratorApprovalFormat.SPRING_KAFKA_JAVA,
            scenario = "additional-payload-types-producer",
        )
    }

    @Test
    fun approves_kotlin_byte_array_producer_implementation_example() {
        val contracts =
            generatedClients.generate(
                contractPath = SINGLE_MESSAGE_CONTRACT,
                language = SourceLanguage.KOTLIN,
                outputDirectory = tempDir.resolve("kotlin-byte-array-producer"),
                additionalPayloadTypes = setOf(AdditionalProducerPayloadType.BYTE_ARRAY),
            )

        GeneratorApprovals.verify(
            generated = contracts.singleProducer(),
            format = GeneratorApprovalFormat.SPRING_KAFKA_KOTLIN,
            scenario = "byte-array-additional-producer",
        )
    }

    @Test
    fun approves_java_string_producer_implementation_example() {
        val contracts =
            generatedClients.generate(
                contractPath = SINGLE_MESSAGE_CONTRACT,
                language = SourceLanguage.JAVA,
                outputDirectory = tempDir.resolve("java-string-producer"),
                additionalPayloadTypes = setOf(AdditionalProducerPayloadType.STRING),
            )

        GeneratorApprovals.verify(
            generated = contracts.singleProducer(),
            format = GeneratorApprovalFormat.SPRING_KAFKA_JAVA,
            scenario = "string-additional-producer",
        )
    }

    @Test
    fun approves_generated_kotlin_object_key_model() {
        val contracts =
            generatedClients.generate(
                contractPath = SINGLE_MESSAGE_CONTRACT,
                language = SourceLanguage.KOTLIN,
                outputDirectory = tempDir.resolve("kotlin-object-key-model"),
            )

        GeneratorApprovals.verify(
            generated = contracts.model("MyAccountKey"),
            format = GeneratorApprovalFormat.KOTLIN,
            scenario = "spring-kafka-object-key-model",
        )
    }

    @Test
    fun approves_generated_java_object_key_class() {
        val contracts =
            generatedClients.generate(
                contractPath = SINGLE_MESSAGE_CONTRACT,
                language = SourceLanguage.JAVA,
                outputDirectory = tempDir.resolve("java-object-key-class"),
            )

        GeneratorApprovals.verify(
            generated = contracts.model("MyAccountKey"),
            format = GeneratorApprovalFormat.JAVA,
            scenario = "spring-kafka-object-key-class",
        )
    }

    @Test
    fun approves_generated_java_object_key_record() {
        val contracts =
            generatedClients.generate(
                contractPath = SINGLE_MESSAGE_CONTRACT,
                language = SourceLanguage.JAVA,
                outputDirectory = tempDir.resolve("java-object-key-record"),
                javaModelType = JavaModelType.RECORD,
            )

        GeneratorApprovals.verify(
            generated = contracts.model("MyAccountKey"),
            format = GeneratorApprovalFormat.JAVA,
            scenario = "spring-kafka-object-key-record",
        )
    }
}
