package dev.banking.asyncapi.generator.core.generator.kafka.spring

import dev.banking.asyncapi.generator.core.fixtures.SpringKafkaClientCompilationFixtures
import dev.banking.asyncapi.generator.core.fixtures.SpringKafkaClientOutputFixtures
import dev.banking.asyncapi.generator.core.fixtures.SpringKafkaClientOutputFixtures.Companion.SINGLE_MESSAGE_CONTRACT
import dev.banking.asyncapi.generator.core.generator.configuration.ClientValidationAnnotations
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
            workspace = tempDir.resolve("java-compilation"),
        )
    }
}
