package dev.banking.asyncapi.generator.core.generator.java.kafka

import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedChannel
import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedMessage
import dev.banking.asyncapi.generator.core.generator.java.kafka.spring.JavaSpringKafkaGenerator
import dev.banking.asyncapi.generator.core.generator.output.GenerationResult
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GenerateJavaSpringKafkaConfigurationTest {
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
        val result = render()

        assertTrue(result.artifacts.any { it.relativePath == producerContract })
        assertTrue(result.artifacts.any { it.relativePath == consumerContract })
    }

    @Test
    fun `configuration can generate only producer contracts`() {
        val result = render(
            generateProducers = true,
            generateConsumers = false,
        )

        assertTrue(result.artifacts.any { it.relativePath == producerContract })
        assertFalse(result.artifacts.any { it.relativePath == consumerContract })
    }

    @Test
    fun `configuration can generate only consumer contracts`() {
        val result = render(
            generateProducers = false,
            generateConsumers = true,
        )

        assertFalse(result.artifacts.any { it.relativePath == producerContract })
        assertTrue(result.artifacts.any { it.relativePath == consumerContract })
    }

    @Test
    fun `generates no contracts when both capabilities are disabled`() {
        val result = render(
            generateProducers = false,
            generateConsumers = false,
        )

        assertFalse(result.artifacts.any { it.relativePath == producerContract })
        assertFalse(result.artifacts.any { it.relativePath == consumerContract })
    }

    private fun render(
        generateProducers: Boolean = true,
        generateConsumers: Boolean = true,
    ): GenerationResult {
        val generator =
            JavaSpringKafkaGenerator(
                clientPackage = packageName,
                modelPackage = packageName,
                generateProducers = generateProducers,
                generateConsumers = generateConsumers,
            )
        return generator.render(listOf(channel))
    }

    private val producerContract = "com/example/kafka/producer/EventsProducer.java"

    private val consumerContract = "com/example/kafka/consumer/EventsConsumer.java"
}
