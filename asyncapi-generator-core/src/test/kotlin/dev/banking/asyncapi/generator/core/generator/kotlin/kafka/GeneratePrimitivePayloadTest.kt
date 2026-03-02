package dev.banking.asyncapi.generator.core.generator.kotlin.kafka

import dev.banking.asyncapi.generator.core.generator.AbstractKotlinGeneratorClass
import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedChannel
import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedMessage
import dev.banking.asyncapi.generator.core.generator.kotlin.kafka.spring.KotlinSpringKafkaGenerator
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import org.junit.jupiter.api.Test
import java.io.File
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
                        AnalyzedMessage("SimpleStringMessage", stringSchema),
                    ),
            )
        val generator =
            KotlinSpringKafkaGenerator(
                outputDir,
                packageName,
                packageName,
                "kafka.topics",
                "topic",
                File("target/generated-resources/asyncapi"),
            )
        generator.generate(listOf(channel))
        val handlerFile =
            outputDir.resolve(packageName.replace('.', '/') + "/handler/TopicSimpleTopicHandlerSimpleStringMessage.kt")
        assertTrue(handlerFile.exists())

        val content = handlerFile.readText()
        assertTrue(
            content.contains("fun onSimpleStringMessage(record: ConsumerRecord<String, String>)"),
            "Should use ConsumerRecord with String payload",
        )
        val producerFile =
            outputDir.resolve(packageName.replace('.', '/') + "/producer/TopicSimpleTopicProducerSimpleStringMessage.kt")
        assertTrue(producerFile.exists(), "Producer should be generated")
        val producerContent = producerFile.readText()
        assertTrue(
            producerContent.contains("KafkaTemplate<String, String>"),
            "Producer should use typed KafkaTemplate for single payload",
        )
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
                        AnalyzedMessage("StringMessage", stringSchema),
                        AnalyzedMessage("IntMessage", intSchema),
                    ),
            )
        val generator =
            KotlinSpringKafkaGenerator(
                outputDir,
                packageName,
                packageName,
                "kafka.topics",
                "topic",
                File("target/generated-resources/asyncapi"),
            )
        generator.generate(listOf(channel))
        val producerFileA =
            outputDir.resolve(packageName.replace('.', '/') + "/producer/TopicMultiTopicProducerStringMessage.kt")
        val producerFileB =
            outputDir.resolve(packageName.replace('.', '/') + "/producer/TopicMultiTopicProducerIntMessage.kt")
        assertTrue(producerFileA.exists(), "StringMessage producer should be generated")
        assertTrue(producerFileB.exists(), "IntMessage producer should be generated")
        val producerContentA = producerFileA.readText()
        val producerContentB = producerFileB.readText()
        assertTrue(
            producerContentA.contains("KafkaTemplate<String, String>"),
            "StringMessage producer should use typed KafkaTemplate",
        )
        assertTrue(
            producerContentB.contains("KafkaTemplate<String, Int>"),
            "IntMessage producer should use typed KafkaTemplate",
        )
    }
}
