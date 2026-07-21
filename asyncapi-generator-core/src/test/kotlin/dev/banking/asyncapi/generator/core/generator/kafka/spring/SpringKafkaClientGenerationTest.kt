package dev.banking.asyncapi.generator.core.generator.kafka.spring

import dev.banking.asyncapi.generator.core.fixtures.GenerationInputFixtures
import dev.banking.asyncapi.generator.core.generator.configuration.ClientValidationAnnotations
import dev.banking.asyncapi.generator.core.generator.configuration.QualifiedTypeName
import dev.banking.asyncapi.generator.core.generator.model.SourceLanguage
import dev.banking.asyncapi.generator.core.generator.plan.GenerationTask
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
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
            sourceOutputDirectory.resolve("com/example/client/producer/UserEventsProducerUserSignedUp.kt").exists(),
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
            sourceOutputDirectory.resolve("com/example/client/producer/UserEventsProducerUserSignedUp.java").exists(),
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
            sourceOutputDirectory.resolve("com/example/client/producer/UserEventsProducerUserSignedUp.kt").exists(),
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
                .resolve("com/example/client/producer/UserEventsProducerUserSignedUp.kt")
                .readText()
        val consumerContent =
            sourceOutputDirectory
                .resolve("com/example/client/consumer/UserEventsConsumer.kt")
                .readText()

        listOf(producerContent, consumerContent).forEach { content ->
            assertTrue(content.contains("import com.example.validation.ValidatedClientContract"))
            assertTrue(content.contains("import com.example.validation.ValidPayload"))
            assertTrue(content.contains("@ValidatedClientContract"))
            assertTrue(content.contains("@param:ValidPayload"))
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
                .resolve("com/example/client/producer/UserEventsProducerUserSignedUp.java")
                .readText()
        val consumerContent =
            sourceOutputDirectory
                .resolve("com/example/client/consumer/UserEventsConsumer.java")
                .readText()

        listOf(producerContent, consumerContent).forEach { content ->
            assertTrue(content.contains("import com.example.validation.ValidatedClientContract;"))
            assertTrue(content.contains("import com.example.validation.ValidPayload;"))
            assertTrue(content.contains("@ValidatedClientContract"))
            assertTrue(content.contains("@ValidPayload @NotNull Object payload"))
        }
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
