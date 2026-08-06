package dev.banking.asyncapi.generator.core.generator.kafka.spring

import dev.banking.asyncapi.generator.core.fixtures.GenerationInputFixtures
import dev.banking.asyncapi.generator.core.fixtures.SchemaFixtures
import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedMessageHeaders
import dev.banking.asyncapi.generator.core.generator.configuration.ClientValidationAnnotations
import dev.banking.asyncapi.generator.core.generator.configuration.QualifiedTypeName
import dev.banking.asyncapi.generator.core.generator.configuration.AdditionalProducerPayloadType
import dev.banking.asyncapi.generator.core.generator.model.SourceLanguage
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactKind
import dev.banking.asyncapi.generator.core.generator.plan.GenerationTask
import org.junit.jupiter.api.Test
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

    @Test
    fun `render delegates Kotlin client task to Kotlin generator`() {
        val result =
            generator.render(
                task =
                    springKafkaClientTask(
                        language = SourceLanguage.KOTLIN,
                    ),
                generationInput = fixtures.generationInputWithUserSignupChannel(),
            )

        assertEquals(
            setOf(
                "com/example/client/producer/UserEventsProducer.kt",
                "com/example/client/consumer/UserEventsConsumer.kt",
            ),
            result.artifacts.map { it.relativePath }.toSet(),
        )
        assertTrue(result.artifacts.all { it.kind == GeneratedArtifactKind.SOURCE })
    }

    @Test
    fun `render delegates Java client task to Java generator`() {
        val result =
            generator.render(
                task =
                    springKafkaClientTask(
                        language = SourceLanguage.JAVA,
                    ),
                generationInput = fixtures.generationInputWithUserSignupChannel(),
            )

        assertEquals(
            setOf(
                "com/example/client/producer/UserEventsProducer.java",
                "com/example/client/consumer/UserEventsConsumer.java",
            ),
            result.artifacts.map { it.relativePath }.toSet(),
        )
        assertTrue(result.artifacts.all { it.kind == GeneratedArtifactKind.SOURCE })
    }

    @Test
    fun `render respects producer and consumer task options`() {
        val result =
            generator.render(
                task =
                    springKafkaClientTask(
                        language = SourceLanguage.KOTLIN,
                        generateProducers = false,
                        generateConsumers = true,
                    ),
                generationInput = fixtures.generationInputWithUserSignupChannel(),
            )

        assertEquals(
            listOf("com/example/client/consumer/UserEventsConsumer.kt"),
            result.artifacts.map { it.relativePath },
        )
    }

    @Test
    fun `render applies configured validation annotations to Kotlin client contracts`() {
        val result =
            generator.render(
                task =
                    springKafkaClientTask(
                        language = SourceLanguage.KOTLIN,
                        validationAnnotations = validationAnnotations,
                    ),
                generationInput = fixtures.generationInputWithUserSignupChannel(),
            )

        val producerContent =
            result.artifacts.single {
                it.relativePath == "com/example/client/producer/UserEventsProducer.kt"
            }.content
        val consumerContent =
            result.artifacts.single {
                it.relativePath == "com/example/client/consumer/UserEventsConsumer.kt"
            }.content

        listOf(producerContent, consumerContent).forEach { content ->
            assertTrue(content.contains("import com.example.validation.ValidatedClientContract"))
            assertTrue(content.contains("import com.example.validation.ValidPayload"))
            assertTrue(content.contains("@ValidatedClientContract"))
            assertTrue(content.contains("@ValidPayload"))
        }
    }

    @Test
    fun `render applies configured validation annotations to Java client contracts`() {
        val result =
            generator.render(
                task =
                    springKafkaClientTask(
                        language = SourceLanguage.JAVA,
                        validationAnnotations = validationAnnotations,
                    ),
                generationInput = fixtures.generationInputWithUserSignupChannel(),
            )

        val producerContent =
            result.artifacts.single {
                it.relativePath == "com/example/client/producer/UserEventsProducer.java"
            }.content
        val consumerContent =
            result.artifacts.single {
                it.relativePath == "com/example/client/consumer/UserEventsConsumer.java"
            }.content

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
    fun `render generates Kotlin producer payload methods in canonical order`() {
        val result =
            generator.render(
                task =
                    springKafkaClientTask(
                        language = SourceLanguage.KOTLIN,
                        additionalPayloadTypes =
                            linkedSetOf(
                                AdditionalProducerPayloadType.STRING,
                                AdditionalProducerPayloadType.BYTE_ARRAY,
                            ),
                        generateConsumers = false,
                        validationAnnotations = validationAnnotations,
                    ),
                generationInput = fixtures.generationInputWithUserSignupChannel(),
            )

        val producerContent = result.artifacts.single().content
        val contractMethod = producerContent.indexOf("fun sendUserSignedUp(")
        val byteArrayMethod = producerContent.indexOf("fun sendUserSignedUpByteArray(")
        val stringMethod = producerContent.indexOf("fun sendUserSignedUpString(")

        assertTrue(contractMethod >= 0)
        assertTrue(byteArrayMethod > contractMethod)
        assertTrue(stringMethod > byteArrayMethod)
        assertTrue(producerContent.contains("payload: UserSignedUpPayload"))
        assertTrue(producerContent.contains("payload: ByteArray"))
        assertTrue(producerContent.contains("payload: String"))
        assertEquals(
            1,
            producerContent.lineSequence().count { line -> line.trim() == "@ValidPayload" },
        )
    }

    @Test
    fun `render keeps the contract producer method when an additional payload type is configured`() {
        val result =
            generator.render(
                task =
                    springKafkaClientTask(
                        language = SourceLanguage.KOTLIN,
                        additionalPayloadTypes =
                            setOf(AdditionalProducerPayloadType.BYTE_ARRAY),
                        validationAnnotations = validationAnnotations,
                    ),
                generationInput = fixtures.generationInputWithUserSignupChannel(),
            )

        val producerContent =
            result.artifacts.single {
                it.relativePath == "com/example/client/producer/UserEventsProducer.kt"
            }.content
        val consumerContent =
            result.artifacts.single {
                it.relativePath == "com/example/client/consumer/UserEventsConsumer.kt"
            }.content

        assertTrue(producerContent.contains("import com.example.model.UserSignedUpPayload"))
        assertTrue(producerContent.contains("fun sendUserSignedUp("))
        assertTrue(producerContent.contains("payload: UserSignedUpPayload"))
        assertTrue(producerContent.contains("fun sendUserSignedUpByteArray("))
        assertTrue(producerContent.contains("payload: ByteArray"))
        assertEquals(
            1,
            producerContent.lineSequence().count { line -> line.trim() == "@ValidPayload" },
        )
        assertTrue(consumerContent.contains("import com.example.model.UserSignedUpPayload"))
        assertTrue(consumerContent.contains("payload: UserSignedUpPayload"))
        assertTrue(consumerContent.contains("@ValidPayload"))
    }

    @Test
    fun `render does not duplicate payloadless producer methods`() {
        val generationInput = fixtures.generationInputWithUserSignupChannel()
        val channel = generationInput.channels.single()
        val payloadlessInput =
            generationInput.copy(
                channels =
                    listOf(
                        channel.copy(
                            messages =
                                channel.messages.map { message ->
                                    message.copy(schema = null)
                                },
                        ),
                    ),
            )

        SourceLanguage.entries.forEach { language ->
            val result =
                generator.render(
                    task =
                        springKafkaClientTask(
                            language = language,
                            additionalPayloadTypes =
                                AdditionalProducerPayloadType.entries.toSet(),
                            generateConsumers = false,
                        ),
                    generationInput = payloadlessInput,
                )

            val producerContent = result.artifacts.single().content
            assertEquals(
                1,
                producerContent.lineSequence().count { line ->
                    line.contains("sendUserSignedUp(")
                },
            )
            assertFalse(producerContent.contains("sendUserSignedUpByteArray"))
            assertFalse(producerContent.contains("sendUserSignedUpString"))
        }
    }

    @Test
    fun `render leaves the Kotlin producer record value type to the implementation`() {
        val result =
            generator.render(
                task =
                    springKafkaClientTask(
                        language = SourceLanguage.KOTLIN,
                    ),
                generationInput = fixtures.generationInputWithUserSignupChannel(),
            )

        val producerContent =
            result.artifacts.single {
                it.relativePath == "com/example/client/producer/UserEventsProducer.kt"
            }.content
        assertTrue(producerContent.contains("interface UserEventsProducer {"))
        assertTrue(producerContent.contains("CompletableFuture<RecordMetadata>"))
    }

    @Test
    fun `render leaves the Java producer record value type to the implementation`() {
        val result =
            generator.render(
                task =
                    springKafkaClientTask(
                        language = SourceLanguage.JAVA,
                    ),
                generationInput = fixtures.generationInputWithUserSignupChannel(),
            )

        val producerContent =
            result.artifacts.single {
                it.relativePath == "com/example/client/producer/UserEventsProducer.java"
            }.content
        assertTrue(producerContent.contains("interface UserEventsProducer {"))
        assertTrue(producerContent.contains("CompletableFuture<RecordMetadata>"))
    }

    @Test
    fun `render preserves contract header scalar types in Java and Kotlin clients`() {
        val generationInput = fixtures.generationInputWithUserSignupChannel()
        val channel = generationInput.channels.single()
        val message = channel.messages.single()
        val inputWithTypedHeaders =
            generationInput.copy(
                channels =
                    listOf(
                        channel.copy(
                            messages =
                                listOf(
                                    message.copy(
                                        headers =
                                            AnalyzedMessageHeaders(
                                                properties =
                                                    linkedMapOf(
                                                        "occurredAt" to
                                                            SchemaFixtures.inline(type = "string", format = "date-time"),
                                                        "attempt" to
                                                            SchemaFixtures.inline(type = "integer", format = "int32"),
                                                        "retryEnabled" to SchemaFixtures.inline(type = "boolean"),
                                                        "amount" to
                                                            SchemaFixtures.inline(type = "number", multipleOf = 0.01),
                                                        "nullableCode" to
                                                            SchemaFixtures.inline(type = listOf("string", "null")),
                                                    ),
                                                requiredProperties =
                                                    listOf(
                                                        "occurredAt",
                                                        "attempt",
                                                        "nullableCode",
                                                    ),
                                            ),
                                    ),
                                ),
                        ),
                    ),
            )

        SourceLanguage.entries.forEach { language ->
            val result =
                generator.render(
                    task = springKafkaClientTask(language = language),
                    generationInput = inputWithTypedHeaders,
                )

            val extension = if (language == SourceLanguage.KOTLIN) "kt" else "java"
            val generatedContracts =
                listOf("producer/UserEventsProducer", "consumer/UserEventsConsumer")
                    .map { relativePath ->
                        result.artifacts.single {
                            it.relativePath == "com/example/client/$relativePath.$extension"
                        }.content
                    }

            generatedContracts.forEach { content ->
                if (language == SourceLanguage.KOTLIN) {
                    assertTrue(content.contains("import java.math.BigDecimal"))
                    assertTrue(content.contains("import java.time.OffsetDateTime"))
                    assertTrue(content.contains("occurredAt: OffsetDateTime"))
                    assertTrue(content.contains("attempt: Int"))
                    assertTrue(content.contains("retryEnabled: Boolean? = null"))
                    assertTrue(content.contains("amount: BigDecimal? = null"))
                    assertTrue(content.contains("nullableCode: String?"))
                    assertFalse(content.contains("nullableCode: String? = null"))
                } else {
                    assertTrue(content.contains("import java.math.BigDecimal;"))
                    assertTrue(content.contains("import java.time.OffsetDateTime;"))
                    assertTrue(content.contains("@NotNull OffsetDateTime occurredAt"))
                    assertTrue(content.contains("@NotNull Integer attempt"))
                    assertTrue(content.contains("@Nullable Boolean retryEnabled"))
                    assertTrue(content.contains("@Nullable BigDecimal amount"))
                    assertTrue(content.contains("@Nullable String nullableCode"))
                }
            }
        }
    }

    @Test
    fun `render rejects a parameterized topic without a configured property mapping`() {
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
                generator.render(
                    task = springKafkaClientTask(language = SourceLanguage.KOTLIN),
                    generationInput = parameterizedInput,
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
    fun `render leaves multi-message handler selection to the application`() {
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
            val result =
                generator.render(
                    task = springKafkaClientTask(language = language),
                    generationInput = sharedPayloadInput,
                )

            val extension = if (language == SourceLanguage.KOTLIN) "kt" else "java"
            val consumerContent =
                result.artifacts.single {
                    it.relativePath == "com/example/client/consumer/UserEventsConsumer.$extension"
                }.content

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

    private fun springKafkaClientTask(
        language: SourceLanguage,
        generateProducers: Boolean = true,
        additionalPayloadTypes: Set<AdditionalProducerPayloadType> = emptySet(),
        generateConsumers: Boolean = true,
        validationAnnotations: ClientValidationAnnotations = ClientValidationAnnotations(),
    ): GenerationTask.SpringKafkaClient =
        GenerationTask.SpringKafkaClient(
            language = language,
            clientPackage = "com.example.client",
            modelPackage = "com.example.model",
            generateProducers = generateProducers,
            additionalPayloadTypes = additionalPayloadTypes,
            generateConsumers = generateConsumers,
            validationAnnotations = validationAnnotations,
        )
}
