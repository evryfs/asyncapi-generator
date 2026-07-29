package dev.banking.asyncapi.generator.core.generator.java.kafka

import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedChannel
import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedMessage
import dev.banking.asyncapi.generator.core.generator.java.kafka.spring.JavaSpringKafkaGenerator
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GenerateJavaSpringKafkaConfigurationTest {
    @TempDir
    lateinit var outputDir: Path

    private val packageName = "com.example.kafka"
    private val channel =
        AnalyzedChannel(
            channelName = "events",
            topic = "events",
            messages =
                listOf(
                    AnalyzedMessage(
                        messageName = "TestEvent",
                        payloadTypeName = "TestEventPayload",
                        schema = Schema(type = "string"),
                    ),
                ),
        )

    @Test
    fun `generates producer and consumer contracts by default`() {
        generate()

        assertTrue(producerContract().toFile().exists())
        assertTrue(consumerContract().toFile().exists())
    }

    @Test
    fun `configuration can generate only producer contracts`() {
        generate(
            generateProducers = true,
            generateConsumers = false,
        )

        assertTrue(producerContract().toFile().exists())
        assertFalse(consumerContract().toFile().exists())
    }

    @Test
    fun `configuration can generate only consumer contracts`() {
        generate(
            generateProducers = false,
            generateConsumers = true,
        )

        assertFalse(producerContract().toFile().exists())
        assertTrue(consumerContract().toFile().exists())
    }

    @Test
    fun `generates no contracts when both capabilities are disabled`() {
        generate(
            generateProducers = false,
            generateConsumers = false,
        )

        assertFalse(producerContract().toFile().exists())
        assertFalse(consumerContract().toFile().exists())
    }

    private fun generate(
        generateProducers: Boolean = true,
        generateConsumers: Boolean = true,
    ) {
        val generator =
            JavaSpringKafkaGenerator(
                outputDir = outputDir.toFile(),
                clientPackage = packageName,
                modelPackage = packageName,
                generateProducers = generateProducers,
                generateConsumers = generateConsumers,
            )
        generator.generate(listOf(channel))
    }

    private fun producerContract(): Path =
        outputDir.resolve("com/example/kafka/producer/EventsProducer.java")

    private fun consumerContract(): Path =
        outputDir.resolve("com/example/kafka/consumer/EventsConsumer.java")
}
