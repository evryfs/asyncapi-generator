package dev.banking.asyncapi.generator.core.generator.java.kafka

import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedChannel
import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedMessage
import dev.banking.asyncapi.generator.core.generator.java.kafka.spring.JavaSpringKafkaGenerator
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GenerateJavaPrimitivePayloadTest {

    @Test
    fun `should generate producer contract for single string payload`() {
        val outputDir = File("target/generated-sources/asyncapi")
        val packageName = "com.example.primitive"
        val stringSchema = Schema(type = "string")
        val channel =
            AnalyzedChannel(
                channelName = "simple/topic",
                topic = "simple.topic.v1",
                isProducer = true,
                isConsumer = true,
                messages =
                    listOf(
                        AnalyzedMessage(
                            messageName = "SimpleStringMessage",
                            payloadTypeName = "SimpleStringMessagePayload",
                            schema = stringSchema
                        )
                    ),
            )
        val generator =
            JavaSpringKafkaGenerator(
                outputDir,
                packageName,
                packageName,
            )
        generator.generate(listOf(channel))
        val producerFile =
            outputDir.resolve(
                packageName.replace(
                    '.',
                    '/'
                ) + "/producer/SimpleTopicProducerSimpleStringMessage.java"
            )
        assertTrue(producerFile.exists(), "Producer should be generated")
        val producerContent = producerFile.readText()
        assertTrue(
            producerContent.contains("interface SimpleTopicProducerSimpleStringMessage"),
            "Producer should be generated as a contract interface",
        )
        assertTrue(
            producerContent.contains("@Valid @NotNull String payload"),
            "Producer should expose the primitive payload type directly",
        )
        assertTrue(producerContent.contains("@NotNull String key"), "Producer should expose the Kafka record key")
        assertFalse(producerContent.contains("KafkaTemplate"), "Producer contract should not own KafkaTemplate wiring")
        assertFalse(producerContent.contains("CompletableFuture"), "Producer contract should not own send results")
    }

    @Test
    fun `should generate one producer per payload for multiple messages`() {
        val outputDir = File("target/generated-sources/asyncapi")
        val packageName = "com.example.primitive.multi"
        val stringSchema = Schema(type = "string")
        val intSchema = Schema(type = "integer")
        val channel =
            AnalyzedChannel(
                channelName = "multi/topic",
                topic = "multi.topic.v1",
                isProducer = true,
                isConsumer = true,
                messages =
                    listOf(
                        AnalyzedMessage(
                            messageName = "StringMessage",
                            payloadTypeName = "StringMessagePayload",
                            schema = stringSchema,
                        ),
                        AnalyzedMessage(
                            messageName = "IntMessage",
                            payloadTypeName = "IntMessagePayload",
                            schema = intSchema,
                        ),
                    ),
            )
        val generator =
            JavaSpringKafkaGenerator(
                outputDir,
                packageName,
                packageName,
            )
        generator.generate(listOf(channel))
        val producerFileA =
            outputDir.resolve(packageName.replace('.', '/') + "/producer/MultiTopicProducerStringMessage.java")
        val producerFileB =
            outputDir.resolve(packageName.replace('.', '/') + "/producer/MultiTopicProducerIntMessage.java")
        assertTrue(producerFileA.exists(), "StringMessage producer should be generated")
        assertTrue(producerFileB.exists(), "IntMessage producer should be generated")
        val producerContentA = producerFileA.readText()
        val producerContentB = producerFileB.readText()
        assertTrue(
            producerContentA.contains("interface MultiTopicProducerStringMessage"),
            "StringMessage producer should be generated as a contract interface",
        )
        assertTrue(
            producerContentA.contains("@Valid @NotNull String payload"),
            "StringMessage producer should expose the primitive payload type directly",
        )
        assertTrue(
            producerContentB.contains("interface MultiTopicProducerIntMessage"),
            "IntMessage producer should be generated as a contract interface",
        )
        assertTrue(
            producerContentB.contains("@Valid @NotNull Integer payload"),
            "IntMessage producer should expose the primitive payload type directly",
        )
        assertFalse(producerContentA.contains("KafkaTemplate"), "StringMessage producer should not own KafkaTemplate wiring")
        assertFalse(producerContentB.contains("KafkaTemplate"), "IntMessage producer should not own KafkaTemplate wiring")
    }
}
