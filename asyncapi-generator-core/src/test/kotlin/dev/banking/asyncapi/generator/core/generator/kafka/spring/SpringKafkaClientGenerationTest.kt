package dev.banking.asyncapi.generator.core.generator.kafka.spring

import dev.banking.asyncapi.generator.core.fixtures.GenerationInputFixtures
import dev.banking.asyncapi.generator.core.generator.model.GeneratorName
import dev.banking.asyncapi.generator.core.generator.plan.GenerationTask
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SpringKafkaClientGenerationTest {
    private val generator = SpringKafkaClientGeneration()
    private val fixtures = GenerationInputFixtures()

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `generate delegates Kotlin client task to Kotlin generator`() {
        val sourceOutputDirectory = tempDir.resolve("kotlin-sources").toFile()
        val resourceOutputDirectory = tempDir.resolve("kotlin-resources").toFile()

        generator.generate(
            task =
                springKafkaClientTask(
                    language = GeneratorName.KOTLIN,
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
                    language = GeneratorName.JAVA,
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
                    language = GeneratorName.KOTLIN,
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

    private fun springKafkaClientTask(
        language: GeneratorName,
        generateProducers: Boolean = true,
        generateConsumers: Boolean = true,
    ): GenerationTask.SpringKafkaClient =
        GenerationTask.SpringKafkaClient(
            language = language,
            clientPackage = "com.example.client",
            modelPackage = "com.example.model",
            generateHeaders = true,
            generateProducers = generateProducers,
            generateConsumers = generateConsumers,
        )
}
