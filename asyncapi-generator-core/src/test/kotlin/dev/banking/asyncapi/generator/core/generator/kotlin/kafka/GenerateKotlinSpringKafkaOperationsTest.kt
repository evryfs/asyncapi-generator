package dev.banking.asyncapi.generator.core.generator.kotlin.kafka

import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedChannel
import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedMessage
import dev.banking.asyncapi.generator.core.generator.kotlin.kafka.spring.KotlinSpringKafkaGenerator
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GenerateKotlinSpringKafkaOperationsTest {
    private val outputDir = File("target/test-output/kafka-ops")
    private val packageName = "com.example.kafka"
    private val dummyMessage = AnalyzedMessage("TestEvent", Schema(type = "string"))

    @Test
    fun `should generate ONLY producer when isProducer=true`() {
        outputDir.deleteRecursively()

        val channel =
            AnalyzedChannel(
                channelName = "events",
                topic = "topic",
                isProducer = true,
                isConsumer = false,
                messages = listOf(dummyMessage),
            )

        val generator =
            KotlinSpringKafkaGenerator(
                outputDir,
                packageName,
                packageName,
                "kafka.topics",
                "topic",
            )
        generator.generate(listOf(channel))

        val packagePath = packageName.replace('.', '/')
        assertTrue(outputDir.resolve("$packagePath/TopicEventsProducerTestEvent.kt").exists(), "Producer should exist")
        assertFalse(outputDir.resolve("$packagePath/TopicEventsListenerTestEvent.kt").exists(), "Listener should NOT exist")
        assertFalse(outputDir.resolve("$packagePath/TopicEventsHandlerTestEvent.kt").exists(), "Handler should NOT exist")
    }

    @Test
    fun `should generate ONLY listener when isConsumer=true`() {
        outputDir.deleteRecursively()

        val channel =
            AnalyzedChannel(
                channelName = "events",
                topic = "topic",
                isProducer = false,
                isConsumer = true,
                messages = listOf(dummyMessage),
            )

        val generator =
            KotlinSpringKafkaGenerator(
                outputDir,
                packageName,
                packageName,
                "kafka.topics",
                "topic",
            )
        generator.generate(listOf(channel))

        val packagePath = packageName.replace('.', '/')
        assertFalse(outputDir.resolve("$packagePath/TopicEventsProducerTestEvent.kt").exists(), "Producer should NOT exist")
        assertTrue(outputDir.resolve("$packagePath/TopicEventsListenerTestEvent.kt").exists(), "Listener should exist")
        assertTrue(outputDir.resolve("$packagePath/TopicEventsHandlerTestEvent.kt").exists(), "Handler should exist")
    }

    @Test
    fun `should generate BOTH when flags are true`() {
        outputDir.deleteRecursively()

        val channel =
            AnalyzedChannel(
                channelName = "events",
                topic = "topic",
                isProducer = true,
                isConsumer = true,
                messages = listOf(dummyMessage),
            )

        val generator =
            KotlinSpringKafkaGenerator(
                outputDir,
                packageName,
                packageName,
                "kafka.topics",
                "topic",
            )
        generator.generate(listOf(channel))

        val packagePath = packageName.replace('.', '/')
        assertTrue(outputDir.resolve("$packagePath/TopicEventsProducerTestEvent.kt").exists(), "Producer should exist")
        assertTrue(outputDir.resolve("$packagePath/TopicEventsListenerTestEvent.kt").exists(), "Listener should exist")
    }

    @Test
    fun `should generate NONE when flags are false (Edge Case)`() {
        outputDir.deleteRecursively()

        val channel =
            AnalyzedChannel(
                channelName = "events",
                topic = "topic",
                isProducer = false,
                isConsumer = false,
                messages = listOf(dummyMessage),
            )

        val generator =
            KotlinSpringKafkaGenerator(
                outputDir,
                packageName,
                packageName,
                "kafka.topics",
                "topic",
            )
        generator.generate(listOf(channel))

        val packagePath = packageName.replace('.', '/')
        assertFalse(outputDir.resolve("$packagePath/TopicEventsProducerTestEvent.kt").exists(), "Producer should NOT exist")
        assertFalse(outputDir.resolve("$packagePath/TopicEventsListenerTestEvent.kt").exists(), "Listener should NOT exist")
    }
}
