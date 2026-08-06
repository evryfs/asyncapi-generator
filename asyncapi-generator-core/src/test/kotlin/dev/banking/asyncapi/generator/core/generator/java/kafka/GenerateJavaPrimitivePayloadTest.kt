package dev.banking.asyncapi.generator.core.generator.java.kafka

import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedChannel
import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedMessage
import dev.banking.asyncapi.generator.core.generator.java.kafka.spring.JavaSpringKafkaGenerator
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GenerateJavaPrimitivePayloadTest {

    @Test
    fun `should generate producer contract for single string payload`() {
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
                            schema = stringSchema
                        )
                    ),
            )
        val generator =
            JavaSpringKafkaGenerator(
                clientPackage = packageName,
                modelPackage = packageName,
            )
        val result = generator.render(listOf(channel))
        val consumerContent =
            result.artifacts.single {
                it.relativePath == "com/example/primitive/consumer/SimpleTopicConsumer.java"
            }.content
        assertTrue(
            consumerContent.contains("void listenSimpleStringMessage("),
            "Consumer should expose the contract method",
        )
        assertTrue(
            consumerContent.contains("@Payload String payload"),
            "Consumer should expose the primitive payload type directly",
        )
        assertFalse(
            consumerContent.contains("receivedKey"),
            "Consumer should omit a key not declared by the contract",
        )
        assertFalse(
            consumerContent.contains("ConsumerRecord"),
            "Consumer contract should not own listener record mapping"
        )

        val producerContent =
            result.artifacts.single {
                it.relativePath == "com/example/primitive/producer/SimpleTopicProducer.java"
            }.content
        assertTrue(
            producerContent.contains("interface SimpleTopicProducer {"),
            "Producer should be generated as a contract interface",
        )
        assertTrue(
            producerContent.contains("@Payload String payload"),
            "Producer should expose the primitive payload type directly",
        )
        assertFalse(producerContent.contains("messageKey"), "Producer should omit a key not declared by the contract")
        assertFalse(
            producerContent.contains("import org.springframework.kafka.core.KafkaTemplate"),
            "Producer contract should not own KafkaTemplate wiring",
        )
        assertTrue(
            producerContent.contains("CompletableFuture<RecordMetadata> sendSimpleStringMessage("),
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
            JavaSpringKafkaGenerator(
                clientPackage = packageName,
                modelPackage = packageName,
            )
        val result = generator.render(listOf(channel))
        val producerContent =
            result.artifacts.single {
                it.relativePath == "com/example/primitive/multi/producer/MultiTopicProducer.java"
            }.content
        assertTrue(
            producerContent.contains("interface MultiTopicProducer {"),
            "Channel producer should be generated as a contract interface",
        )
        assertTrue(
            producerContent.contains("CompletableFuture<RecordMetadata> sendStringMessage("),
            "StringMessage should have a dedicated producer method",
        )
        assertTrue(
            producerContent.contains("@Payload String payload"),
            "StringMessage producer should expose the primitive payload type directly",
        )
        assertTrue(
            producerContent.contains("CompletableFuture<RecordMetadata> sendIntMessage("),
            "IntMessage should have a dedicated producer method",
        )
        assertTrue(
            producerContent.contains("@Payload Integer payload"),
            "IntMessage producer should expose the primitive payload type directly",
        )
        assertFalse(
            producerContent.contains("import org.springframework.kafka.core.KafkaTemplate"),
            "Producer contract should not own KafkaTemplate wiring",
        )
    }
}
