package dev.banking.asyncapi.generator.core.generator.java.kafka

import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedChannel
import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedMessage
import dev.banking.asyncapi.generator.core.generator.java.kafka.spring.JavaSpringKafkaGenerator
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertTrue

class GenerateJavaPrimitivePayloadTest {
    @Test
    fun `should generate typed KafkaTemplate for single string payload`() {
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
                        AnalyzedMessage("SimpleStringMessage", stringSchema),
                    ),
            )
        val generator =
            JavaSpringKafkaGenerator(
                outputDir,
                packageName,
                packageName,
                "kafka.topics",
                "topic",
            )
        generator.generate(listOf(channel))
        val producerFile = outputDir.resolve(packageName.replace('.', '/') + "/SimpleTopicProducer.java")
        assertTrue(producerFile.exists(), "Producer should be generated")
        val producerContent = producerFile.readText()
        assertTrue(
            producerContent.contains("KafkaTemplate<String, String>"),
            "Producer should use typed KafkaTemplate for single payload",
        )
    }

    @Test
    fun `should use Object KafkaTemplate for multiple payloads`() {
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
                        AnalyzedMessage("StringMessage", stringSchema),
                        AnalyzedMessage("IntMessage", intSchema),
                    ),
            )
        val generator =
            JavaSpringKafkaGenerator(
                outputDir,
                packageName,
                packageName,
                "kafka.topics",
                "topic",
            )
        generator.generate(listOf(channel))
        val producerFile = outputDir.resolve(packageName.replace('.', '/') + "/MultiTopicProducer.java")
        assertTrue(producerFile.exists(), "Producer should be generated")
        val producerContent = producerFile.readText()
        assertTrue(
            producerContent.contains("KafkaTemplate<String, Object>"),
            "Producer should use Object KafkaTemplate for multiple payloads",
        )
    }
}
