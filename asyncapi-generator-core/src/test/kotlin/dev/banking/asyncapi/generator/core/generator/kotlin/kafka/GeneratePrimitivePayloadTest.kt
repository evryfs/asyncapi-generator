package dev.banking.asyncapi.generator.core.generator.kotlin.kafka

import dev.banking.asyncapi.generator.core.generator.AbstractKotlinGeneratorClass
import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedChannel
import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedMessage
import dev.banking.asyncapi.generator.core.generator.kotlin.kafka.spring.KotlinSpringKafkaGenerator
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GeneratePrimitivePayloadTest : AbstractKotlinGeneratorClass() {
    @Test
    fun `should generate client for primitive string payload`() {
        val packageName = "com.example.primitive"
        val stringSchema = Schema(type = "string")
        val channel =
            AnalyzedChannel(
                channelName = "simple/topic",
                topic = "simple.topic.v1",
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
                clientPackage = packageName,
                modelPackage = packageName,
            )
        val result = generator.render(listOf(channel))
        val content =
            result.artifacts.single {
                it.relativePath == "com/example/primitive/consumer/SimpleTopicConsumer.kt"
            }.content
        assertTrue(
            content.contains("fun listenSimpleStringMessage("),
            "Consumer should expose the contract method",
        )
        assertTrue(content.contains("payload: String"), "Consumer should expose the primitive payload type directly")
        assertFalse(content.contains("receivedKey:"), "Consumer should omit a key not declared by the contract")
        assertFalse(content.contains("ConsumerRecord"), "Consumer contract should not own listener record mapping")
        val producerContent =
            result.artifacts.single {
                it.relativePath == "com/example/primitive/producer/SimpleTopicProducer.kt"
            }.content
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
        assertFalse(
            producerContent.contains("import org.springframework.kafka.core.KafkaTemplate"),
            "Producer contract should not own KafkaTemplate wiring",
        )
        assertTrue(
            producerContent.contains("CompletableFuture<RecordMetadata>"),
            "Producer contract should expose the asynchronous send result",
        )
    }

    @Test
    fun `should generate one producer method per payload for multiple messages`() {
        val packageName = "com.example.primitive.multi"
        val stringSchema = Schema(type = "string")
        val intSchema = Schema(type = "integer")
        val channel =
            AnalyzedChannel(
                channelName = "multi/topic",
                topic = "multi.topic.v1",
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
                clientPackage = packageName,
                modelPackage = packageName,
            )
        val result = generator.render(listOf(channel))
        val producerContent =
            result.artifacts.single {
                it.relativePath == "com/example/primitive/multi/producer/MultiTopicProducer.kt"
            }.content
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
        assertFalse(
            producerContent.contains("import org.springframework.kafka.core.KafkaTemplate"),
            "Producer contract should not own KafkaTemplate wiring",
        )
    }
}
