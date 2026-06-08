package dev.banking.asyncapi.generator.core.generator.kafka.spring

import dev.banking.asyncapi.generator.core.fixtures.GenerationInputFixtures
import dev.banking.asyncapi.generator.core.generator.model.GeneratorName
import dev.banking.asyncapi.generator.core.generator.plan.GenerationTask
import dev.banking.asyncapi.generator.core.generator.plan.SpringKafkaClientType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertTrue

class SpringKafkaClientGenerationTest {
    private val generator = SpringKafkaClientGeneration()
    private val fixtures = GenerationInputFixtures()

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `generate delegates Kotlin simple client task to Kotlin simple generator`() {
        val sourceOutputDirectory = tempDir.resolve("kotlin-sources").toFile()
        val resourceOutputDirectory = tempDir.resolve("kotlin-resources").toFile()

        generator.generate(
            task =
                springKafkaClientTask(
                    language = GeneratorName.KOTLIN,
                    clientType = SpringKafkaClientType.SIMPLE,
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
    fun `generate delegates Kotlin full client task to Kotlin full generator`() {
        val sourceOutputDirectory = tempDir.resolve("kotlin-full-sources").toFile()
        val resourceOutputDirectory = tempDir.resolve("kotlin-full-resources").toFile()

        generator.generate(
            task =
                springKafkaClientTask(
                    language = GeneratorName.KOTLIN,
                    clientType = SpringKafkaClientType.FULL,
                    topicPropertyPrefix = "custom.topics",
                ),
            generationInput = fixtures.generationInputWithUserSignupChannel(),
            sourceOutputDirectory = sourceOutputDirectory,
            resourceOutputDirectory = resourceOutputDirectory,
        )

        assertTrue(
            sourceOutputDirectory.resolve("com/example/client/config/AsyncApiKafkaAutoConfiguration.kt").exists(),
        )
        assertTrue(
            sourceOutputDirectory.resolve("com/example/client/listener/TopicUserEventsListenerUserSignedUp.kt").exists(),
        )
        assertTrue(
            sourceOutputDirectory.resolve("com/example/client/handler/TopicUserEventsHandlerUserSignedUp.kt").exists(),
        )
        assertTrue(
            sourceOutputDirectory.resolve("com/example/client/producer/TopicUserEventsProducerUserSignedUp.kt").exists(),
        )
        val producerContent =
            sourceOutputDirectory
                .resolve("com/example/client/producer/TopicUserEventsProducerUserSignedUp.kt")
                .readText()
        assertTrue(
            producerContent.contains("custom.topics.userEvents"),
        )
        assertTrue(
            resourceOutputDirectory
                .resolve("META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports")
                .exists(),
        )
    }

    @Test
    fun `generate delegates Java simple client task to Java simple generator`() {
        val sourceOutputDirectory = tempDir.resolve("java-sources").toFile()
        val resourceOutputDirectory = tempDir.resolve("java-resources").toFile()

        generator.generate(
            task =
                springKafkaClientTask(
                    language = GeneratorName.JAVA,
                    clientType = SpringKafkaClientType.SIMPLE,
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
    fun `generate delegates Java full client task to Java full generator`() {
        val sourceOutputDirectory = tempDir.resolve("java-full-sources").toFile()
        val resourceOutputDirectory = tempDir.resolve("java-full-resources").toFile()

        generator.generate(
            task =
                springKafkaClientTask(
                    language = GeneratorName.JAVA,
                    clientType = SpringKafkaClientType.FULL,
                ),
            generationInput = fixtures.generationInputWithUserSignupChannel(),
            sourceOutputDirectory = sourceOutputDirectory,
            resourceOutputDirectory = resourceOutputDirectory,
        )

        assertTrue(
            sourceOutputDirectory.resolve("com/example/client/config/AsyncApiKafkaAutoConfiguration.java").exists(),
        )
        assertTrue(
            sourceOutputDirectory.resolve("com/example/client/listener/TopicUserEventsListenerUserSignedUp.java").exists(),
        )
        assertTrue(
            sourceOutputDirectory.resolve("com/example/client/handler/TopicUserEventsHandlerUserSignedUp.java").exists(),
        )
        assertTrue(
            sourceOutputDirectory.resolve("com/example/client/producer/TopicUserEventsProducerUserSignedUp.java").exists(),
        )
        assertTrue(
            resourceOutputDirectory
                .resolve("META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports")
                .exists(),
        )
    }

    private fun springKafkaClientTask(
        language: GeneratorName,
        clientType: SpringKafkaClientType,
        topicPropertyPrefix: String = "kafka.topics",
    ): GenerationTask.SpringKafkaClient =
        GenerationTask.SpringKafkaClient(
            language = language,
            clientType = clientType,
            clientPackage = "com.example.client",
            modelPackage = "com.example.model",
            topicPropertyPrefix = topicPropertyPrefix,
        )
}
