package dev.banking.asyncapi.generator.core.generator.kafka.spring

import dev.banking.asyncapi.generator.core.fixtures.SpringKafkaClientCompilationFixtures
import dev.banking.asyncapi.generator.core.fixtures.SpringKafkaClientOutputFixtures
import dev.banking.asyncapi.generator.core.fixtures.SpringKafkaClientOutputFixtures.Companion.SINGLE_MESSAGE_CONTRACT
import dev.banking.asyncapi.generator.core.fixtures.SpringKafkaClientOutputFixtures.Companion.THREE_MESSAGE_CONTRACT
import dev.banking.asyncapi.generator.core.generator.configuration.ClientValidationAnnotations
import dev.banking.asyncapi.generator.core.generator.configuration.AdditionalProducerPayloadType
import dev.banking.asyncapi.generator.core.generator.model.SourceLanguage
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class SpringKafkaTopicAddressCompilationTest {
    private val generatedClients = SpringKafkaClientOutputFixtures()
    private val compilationFixtures = SpringKafkaClientCompilationFixtures()

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `generated Kotlin topic address is valid in a listener annotation`() {
        val contracts =
            generatedClients.generate(
                contractPath = SINGLE_MESSAGE_CONTRACT,
                language = SourceLanguage.KOTLIN,
                outputDirectory = tempDir.resolve("generated-contracts"),
                validationAnnotations = ClientValidationAnnotations(),
            )

        compilationFixtures.compileKotlinConsumerTopicConstant(
            consumerSource = contracts.singleConsumer(),
            contractName = "MyAccountUpdatedConsumer",
            topicAddressConstantName = "MY_ACCOUNT_UPDATED_TOPIC_ADDRESS",
            payloadName = "MyAccountUpdatedPayload",
            keyModelName = "MyAccountKey",
            workspace = tempDir.resolve("compilation"),
        )
    }

    @Test
    fun `generated Java topic address is valid in a listener annotation`() {
        val contracts =
            generatedClients.generate(
                contractPath = SINGLE_MESSAGE_CONTRACT,
                language = SourceLanguage.JAVA,
                outputDirectory = tempDir.resolve("generated-java-contracts"),
                validationAnnotations = ClientValidationAnnotations(),
            )

        compilationFixtures.compileJavaConsumerTopicConstant(
            consumerSource = contracts.singleConsumer(),
            contractName = "MyAccountUpdatedConsumer",
            topicAddressConstantName = "MY_ACCOUNT_UPDATED_TOPIC_ADDRESS",
            payloadName = "MyAccountUpdatedPayload",
            keyModelName = "MyAccountKey",
            workspace = tempDir.resolve("java-compilation"),
        )
    }

    @Test
    fun `generated Kotlin contracts support partial implementations with message-specific Kafka keys`() {
        val contracts =
            generatedClients.generate(
                contractPath = THREE_MESSAGE_CONTRACT,
                language = SourceLanguage.KOTLIN,
                outputDirectory = tempDir.resolve("generated-mixed-kotlin-contracts"),
                validationAnnotations = ClientValidationAnnotations(),
            )

        compilationFixtures.compileKotlinContracts(
            producerSource = contracts.singleProducer(),
            consumerSource = contracts.singleConsumer(),
            producerName = "MyAccountLifecycleProducer",
            consumerName = "MyAccountLifecycleConsumer",
            payloadNames =
                listOf(
                    "MyAccountCreatedPayload",
                    "MyAccountUpdatedPayload",
                    "MyAccountClosedPayload",
                ),
            keyModelNames = listOf("MyAccountClosureKey"),
            workspace = tempDir.resolve("mixed-kotlin-compilation"),
        )
    }

    @Test
    fun `generated Java contracts support partial implementations with message-specific Kafka keys`() {
        val contracts =
            generatedClients.generate(
                contractPath = THREE_MESSAGE_CONTRACT,
                language = SourceLanguage.JAVA,
                outputDirectory = tempDir.resolve("generated-mixed-java-contracts"),
                validationAnnotations = ClientValidationAnnotations(),
            )

        compilationFixtures.compileJavaContracts(
            producerSource = contracts.singleProducer(),
            consumerSource = contracts.singleConsumer(),
            producerName = "MyAccountLifecycleProducer",
            consumerName = "MyAccountLifecycleConsumer",
            payloadNames =
                listOf(
                    "MyAccountCreatedPayload",
                    "MyAccountUpdatedPayload",
                    "MyAccountClosedPayload",
                ),
            keyModelNames = listOf("MyAccountClosureKey"),
            workspace = tempDir.resolve("mixed-java-compilation"),
        )
    }

    @Test
    fun `generated Kotlin producer payload representations compile`() {
        val contracts =
            generatedClients.generate(
                contractPath = SINGLE_MESSAGE_CONTRACT,
                language = SourceLanguage.KOTLIN,
                outputDirectory = tempDir.resolve("generated-kotlin-payload-contracts"),
                validationAnnotations = ClientValidationAnnotations(),
                additionalPayloadTypes = AdditionalProducerPayloadType.entries.toSet(),
            )

        compilationFixtures.compileKotlinContracts(
            producerSource = contracts.singleProducer(),
            consumerSource = contracts.singleConsumer(),
            producerName = "MyAccountUpdatedProducer",
            consumerName = "MyAccountUpdatedConsumer",
            payloadNames = listOf("MyAccountUpdatedPayload"),
            keyModelNames = listOf("MyAccountKey"),
            workspace = tempDir.resolve("kotlin-payload-compilation"),
        )
    }

    @Test
    fun `generated Java producer payload representations compile`() {
        val contracts =
            generatedClients.generate(
                contractPath = SINGLE_MESSAGE_CONTRACT,
                language = SourceLanguage.JAVA,
                outputDirectory = tempDir.resolve("generated-java-payload-contracts"),
                validationAnnotations = ClientValidationAnnotations(),
                additionalPayloadTypes = AdditionalProducerPayloadType.entries.toSet(),
            )

        compilationFixtures.compileJavaContracts(
            producerSource = contracts.singleProducer(),
            consumerSource = contracts.singleConsumer(),
            producerName = "MyAccountUpdatedProducer",
            consumerName = "MyAccountUpdatedConsumer",
            payloadNames = listOf("MyAccountUpdatedPayload"),
            keyModelNames = listOf("MyAccountKey"),
            workspace = tempDir.resolve("java-payload-compilation"),
        )
    }
}
