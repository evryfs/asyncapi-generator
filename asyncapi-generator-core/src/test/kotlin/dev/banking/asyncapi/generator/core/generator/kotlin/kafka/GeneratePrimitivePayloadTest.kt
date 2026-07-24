package dev.banking.asyncapi.generator.core.generator.kotlin.kafka

import dev.banking.asyncapi.generator.core.generator.AbstractKotlinGeneratorClass
import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedChannel
import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedMessage
import dev.banking.asyncapi.generator.core.generator.kotlin.kafka.spring.KotlinSpringKafkaGenerator
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GeneratePrimitivePayloadTest : AbstractKotlinGeneratorClass() {
    @Test
    fun `should generate client for primitive string payload`() {
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
                            schema = stringSchema,
                        ),
                    ),
            )
        val generator =
            KotlinSpringKafkaGenerator(
                outputDir,
                packageName,
                packageName,
            )
        generator.generate(listOf(channel))
        val consumerFile =
            outputDir.resolve(packageName.replace('.', '/') + "/consumer/SimpleTopicConsumer.kt")
        assertTrue(consumerFile.exists())

        val content = consumerFile.readText()
        assertTrue(
            content.contains("fun listenSimpleStringMessage("),
            "Consumer should expose the contract method",
        )
        assertTrue(content.contains("payload: String"), "Consumer should expose the primitive payload type directly")
        assertFalse(content.contains("receivedKey:"), "Consumer should omit a key not declared by the contract")
        assertFalse(content.contains("ConsumerRecord"), "Consumer contract should not own listener record mapping")
        val producerFile =
            outputDir.resolve(
                packageName.replace('.', '/') + "/producer/SimpleTopicProducer.kt",
            )
        assertTrue(producerFile.exists(), "Producer should be generated")
        val producerContent = producerFile.readText()
        assertTrue(
            producerContent.contains("interface SimpleTopicProducer {"),
            "Producer should be generated as a contract interface",
        )
        assertTrue(
            producerContent.contains("fun sendSimpleStringMessage("),
            "Producer should expose the message-qualified contract method",
        )
        assertTrue(
            producerContent.contains("payload: String"),
            "Producer should expose the primitive payload type directly",
        )
        assertFalse(producerContent.contains("messageKey:"), "Producer should omit a key not declared by the contract")
        assertFalse(producerContent.contains("KafkaTemplate"), "Producer contract should not own KafkaTemplate wiring")
        assertTrue(
            producerContent.contains("CompletableFuture<RecordMetadata>"),
            "Producer contract should expose the asynchronous send result",
        )
    }

    @Test
    fun `should generate one producer method per payload for multiple messages`() {
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
            KotlinSpringKafkaGenerator(
                outputDir,
                packageName,
                packageName,
            )
        generator.generate(listOf(channel))
        val producerFile =
            outputDir.resolve(packageName.replace('.', '/') + "/producer/MultiTopicProducer.kt")
        assertTrue(producerFile.exists(), "Channel producer should be generated")
        val producerContent = producerFile.readText()
        assertTrue(
            producerContent.contains("interface MultiTopicProducer {"),
            "Channel producer should be generated as a contract interface",
        )
        assertTrue(
            producerContent.contains("fun sendStringMessage("),
            "StringMessage should have a dedicated producer method",
        )
        assertTrue(
            producerContent.contains("payload: String"),
            "StringMessage producer should expose the primitive payload type directly",
        )
        assertTrue(
            producerContent.contains("fun sendIntMessage("),
            "IntMessage should have a dedicated producer method",
        )
        assertTrue(
            producerContent.contains("payload: Int"),
            "IntMessage producer should expose the primitive payload type directly",
        )
        assertFalse(producerContent.contains("KafkaTemplate"), "Producer contract should not own KafkaTemplate wiring")
    }
}
