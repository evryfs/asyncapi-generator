package dev.banking.asyncapi.generator.core.generator.kafka.spring

import dev.banking.asyncapi.generator.core.fixtures.GenerationInputFixtures
import dev.banking.asyncapi.generator.core.generator.configuration.ClientValidationAnnotations
import dev.banking.asyncapi.generator.core.generator.configuration.QualifiedTypeName
import dev.banking.asyncapi.generator.core.generator.model.SourceLanguage
import dev.banking.asyncapi.generator.core.generator.plan.GenerationTask
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.SpringKafkaClientMethodNameCollision
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SpringKafkaClientGenerationTest {
    private val generator = SpringKafkaClientGeneration()
    private val fixtures = GenerationInputFixtures()
    private val validationAnnotations =
        ClientValidationAnnotations(
            clientContract =
                QualifiedTypeName.fromConfigurationValue(
                    value = "com.example.validation.ValidatedClientContract",
                    path = "validationAnnotations.clientContract",
                ),
            payloadParameter =
                QualifiedTypeName.fromConfigurationValue(
                    value = "com.example.validation.ValidPayload",
                    path = "validationAnnotations.payloadParameter",
                ),
        )

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `generate delegates Kotlin client task to Kotlin generator`() {
        val sourceOutputDirectory = tempDir.resolve("kotlin-sources").toFile()
        val resourceOutputDirectory = tempDir.resolve("kotlin-resources").toFile()

        generator.generate(
            task =
                springKafkaClientTask(
                    language = SourceLanguage.KOTLIN,
                ),
            generationInput = fixtures.generationInputWithUserSignupChannel(),
            sourceOutputDirectory = sourceOutputDirectory,
            resourceOutputDirectory = resourceOutputDirectory,
        )

        assertTrue(
            sourceOutputDirectory.resolve("com/example/client/producer/UserEventsProducer.kt").exists(),
        )
        assertTrue(
            sourceOutputDirectory.resolve("com/example/client/consumer/UserEventsConsumer.kt").exists(),
        )
    }

    @Test
    fun `generate delegates Java client task to Java generator`() {
        val sourceOutputDirectory = tempDir.resolve("java-sources").toFile()
        val resourceOutputDirectory = tempDir.resolve("java-resources").toFile()

        generator.generate(
            task =
                springKafkaClientTask(
                    language = SourceLanguage.JAVA,
                ),
            generationInput = fixtures.generationInputWithUserSignupChannel(),
            sourceOutputDirectory = sourceOutputDirectory,
            resourceOutputDirectory = resourceOutputDirectory,
        )

        assertTrue(
            sourceOutputDirectory.resolve("com/example/client/producer/UserEventsProducer.java").exists(),
        )
        assertTrue(
            sourceOutputDirectory.resolve("com/example/client/consumer/UserEventsConsumer.java").exists(),
        )
    }

    @Test
    fun `generate respects producer and consumer task options`() {
        val sourceOutputDirectory = tempDir.resolve("configured-sources").toFile()
        val resourceOutputDirectory = tempDir.resolve("configured-resources").toFile()

        generator.generate(
            task =
                springKafkaClientTask(
                    language = SourceLanguage.KOTLIN,
                    generateProducers = false,
                    generateConsumers = true,
                ),
            generationInput = fixtures.generationInputWithUserSignupChannel(),
            sourceOutputDirectory = sourceOutputDirectory,
            resourceOutputDirectory = resourceOutputDirectory,
        )

        assertFalse(
            sourceOutputDirectory.resolve("com/example/client/producer/UserEventsProducer.kt").exists(),
        )
        assertTrue(
            sourceOutputDirectory.resolve("com/example/client/consumer/UserEventsConsumer.kt").exists(),
        )
    }

    @Test
    fun `generate applies configured validation annotations to Kotlin client contracts`() {
        val sourceOutputDirectory = tempDir.resolve("configured-kotlin-sources").toFile()

        generator.generate(
            task =
                springKafkaClientTask(
                    language = SourceLanguage.KOTLIN,
                    validationAnnotations = validationAnnotations,
                ),
            generationInput = fixtures.generationInputWithUserSignupChannel(),
            sourceOutputDirectory = sourceOutputDirectory,
            resourceOutputDirectory = tempDir.resolve("configured-kotlin-resources").toFile(),
        )

        val producerContent =
            sourceOutputDirectory
                .resolve("com/example/client/producer/UserEventsProducer.kt")
                .readText()
        val consumerContent =
            sourceOutputDirectory
                .resolve("com/example/client/consumer/UserEventsConsumer.kt")
                .readText()

        listOf(producerContent, consumerContent).forEach { content ->
            assertTrue(content.contains("import com.example.validation.ValidatedClientContract"))
            assertTrue(content.contains("import com.example.validation.ValidPayload"))
            assertTrue(content.contains("@ValidatedClientContract"))
            assertTrue(content.contains("@ValidPayload"))
        }
    }

    @Test
    fun `generate applies configured validation annotations to Java client contracts`() {
        val sourceOutputDirectory = tempDir.resolve("configured-java-sources").toFile()

        generator.generate(
            task =
                springKafkaClientTask(
                    language = SourceLanguage.JAVA,
                    validationAnnotations = validationAnnotations,
                ),
            generationInput = fixtures.generationInputWithUserSignupChannel(),
            sourceOutputDirectory = sourceOutputDirectory,
            resourceOutputDirectory = tempDir.resolve("configured-java-resources").toFile(),
        )

        val producerContent =
            sourceOutputDirectory
                .resolve("com/example/client/producer/UserEventsProducer.java")
                .readText()
        val consumerContent =
            sourceOutputDirectory
                .resolve("com/example/client/consumer/UserEventsConsumer.java")
                .readText()

        listOf(producerContent, consumerContent).forEach { content ->
            assertTrue(content.contains("import com.example.validation.ValidatedClientContract;"))
            assertTrue(content.contains("import com.example.validation.ValidPayload;"))
            assertTrue(content.contains("@ValidatedClientContract"))
            assertTrue(content.contains("@ValidPayload Object payload"))
            assertFalse(content.contains("@ValidPayload @NotNull Object payload"))
        }
        assertFalse(producerContent.contains("jakarta.validation.constraints.NotNull"))
        assertTrue(consumerContent.contains("import jakarta.validation.constraints.NotNull;"))
        assertTrue(
            consumerContent.contains(
                "@Header(name = KafkaHeaders.RECEIVED_TOPIC, required = true) @NotNull String receivedTopic",
            ),
        )
    }

    @Test
    fun `generate leaves the Kotlin producer record value type to the implementation`() {
        val sourceOutputDirectory = tempDir.resolve("flexible-kotlin-producer-sources").toFile()

        generator.generate(
            task =
                springKafkaClientTask(
                    language = SourceLanguage.KOTLIN,
                ),
            generationInput = fixtures.generationInputWithUserSignupChannel(),
            sourceOutputDirectory = sourceOutputDirectory,
            resourceOutputDirectory = tempDir.resolve("flexible-kotlin-producer-resources").toFile(),
        )

        val producerContent =
            sourceOutputDirectory
                .resolve("com/example/client/producer/UserEventsProducer.kt")
                .readText()
        assertTrue(producerContent.contains("interface UserEventsProducer {"))
        assertTrue(producerContent.contains("CompletableFuture<RecordMetadata>"))
    }

    @Test
    fun `generate leaves the Java producer record value type to the implementation`() {
        val sourceOutputDirectory = tempDir.resolve("flexible-java-producer-sources").toFile()

        generator.generate(
            task =
                springKafkaClientTask(
                    language = SourceLanguage.JAVA,
                ),
            generationInput = fixtures.generationInputWithUserSignupChannel(),
            sourceOutputDirectory = sourceOutputDirectory,
            resourceOutputDirectory = tempDir.resolve("flexible-java-producer-resources").toFile(),
        )

        val producerContent =
            sourceOutputDirectory
                .resolve("com/example/client/producer/UserEventsProducer.java")
                .readText()
        assertTrue(producerContent.contains("interface UserEventsProducer {"))
        assertTrue(producerContent.contains("CompletableFuture<RecordMetadata>"))
    }

    @Test
    fun `generate rejects a parameterized topic without a configured property mapping`() {
        val generationInput = fixtures.generationInputWithUserSignupChannel()
        val parameterizedInput =
            generationInput.copy(
                channels =
                    generationInput.channels.map { channel ->
                        channel.copy(topic = "user.{environment}.events")
                    },
            )

        val exception =
            assertFailsWith<IllegalArgumentException> {
                generator.generate(
                    task = springKafkaClientTask(language = SourceLanguage.KOTLIN),
                    generationInput = parameterizedInput,
                    sourceOutputDirectory = tempDir.resolve("missing-mapping-sources").toFile(),
                    resourceOutputDirectory = tempDir.resolve("missing-mapping-resources").toFile(),
                )
            }

        assertEquals(
            "Cannot generate Spring Kafka client for channel 'userEvents': " +
                "topic address 'user.{environment}.events' uses channel parameters [environment] without " +
                "matching topicParameterProperties entries. Configured entries: []",
            exception.message,
        )
    }

    @Test
    fun `generate leaves multi-message handler selection to the application`() {
        val generationInput = fixtures.generationInputWithUserSignupChannel()
        val channel = generationInput.channels.single()
        val message = channel.messages.single()
        val sharedPayloadInput =
            generationInput.copy(
                channels =
                    listOf(
                        channel.copy(
                            messages =
                                listOf(
                                    message.copy(messageName = "UserSignedUp"),
                                    message.copy(messageName = "UserProfileUpdated"),
                                ),
                        ),
                    ),
            )

        SourceLanguage.entries.forEach { language ->
            val sourceOutputDirectory = tempDir.resolve("shared-payload-$language-sources").toFile()
            generator.generate(
                task = springKafkaClientTask(language = language),
                generationInput = sharedPayloadInput,
                sourceOutputDirectory = sourceOutputDirectory,
                resourceOutputDirectory = tempDir.resolve("shared-payload-$language-resources").toFile(),
            )

            val extension = if (language == SourceLanguage.KOTLIN) "kt" else "java"
            val consumerContent =
                sourceOutputDirectory
                    .resolve("com/example/client/consumer/UserEventsConsumer.$extension")
                    .readText()

            assertTrue(consumerContent.contains("listenUserSignedUp"))
            assertTrue(consumerContent.contains("listenUserProfileUpdated"))
            assertFalse(consumerContent.contains("import org.springframework.kafka.annotation.KafkaHandler"))
            assertFalse(
                consumerContent.lineSequence().any { line ->
                    line.trim() == "@KafkaHandler"
                },
            )
        }
    }

    @Test
    fun `generate rejects method name collisions before writing client sources`() {
        val generationInput = fixtures.generationInputWithUserSignupChannel()
        val channel = generationInput.channels.single()
        val message = channel.messages.single()
        val sourceOutputDirectory = tempDir.resolve("colliding-method-sources").toFile()

        val error =
            assertFailsWith<SpringKafkaClientMethodNameCollision> {
                generator.generate(
                    task = springKafkaClientTask(language = SourceLanguage.JAVA),
                    generationInput =
                        generationInput.copy(
                            channels =
                                listOf(
                                    channel.copy(
                                        messages =
                                            listOf(
                                                message.copy(
                                                    messageId = "user-signed-up",
                                                    messageName = "UserSignedUp",
                                                ),
                                                message.copy(
                                                    messageId = "user_signed_up",
                                                    messageName = "UserSignedUp",
                                                ),
                                            ),
                                    ),
                                ),
                        ),
                    sourceOutputDirectory = sourceOutputDirectory,
                    resourceOutputDirectory = tempDir.resolve("colliding-method-resources").toFile(),
                )
            }

        assertTrue(error.message!!.contains("['user-signed-up', 'user_signed_up']"))
        assertFalse(sourceOutputDirectory.exists())
    }

    private fun springKafkaClientTask(
        language: SourceLanguage,
        generateProducers: Boolean = true,
        generateConsumers: Boolean = true,
        validationAnnotations: ClientValidationAnnotations = ClientValidationAnnotations(),
    ): GenerationTask.SpringKafkaClient =
        GenerationTask.SpringKafkaClient(
            language = language,
            clientPackage = "com.example.client",
            modelPackage = "com.example.model",
            generateHeaders = true,
            generateProducers = generateProducers,
            generateConsumers = generateConsumers,
            validationAnnotations = validationAnnotations,
        )
}
