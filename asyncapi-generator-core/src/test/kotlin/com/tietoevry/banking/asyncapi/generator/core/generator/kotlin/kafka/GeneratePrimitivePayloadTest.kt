package com.tietoevry.banking.asyncapi.generator.core.generator.kotlin.kafka

import com.tietoevry.banking.asyncapi.generator.core.generator.AbstractKotlinGeneratorClass
import com.tietoevry.banking.asyncapi.generator.core.generator.analyzer.AnalyzedChannel
import com.tietoevry.banking.asyncapi.generator.core.generator.analyzer.AnalyzedMessage
import com.tietoevry.banking.asyncapi.generator.core.generator.kotlin.kafka.spring.KotlinSpringKafkaGenerator
import com.tietoevry.banking.asyncapi.generator.core.model.schemas.Schema
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertTrue

class GeneratePrimitivePayloadTest : AbstractKotlinGeneratorClass() {

    @Test
    fun `should generate client for primitive string payload`() {
        val outputDir = File("target/generated-sources/asyncapi")
        val packageName = "com.example.primitive"

        val stringSchema = Schema(type = "string")
        val channel = AnalyzedChannel(
            channelName = "simple/topic",
            topic = "simple.topic.v1",
            isProducer = true,
            isConsumer = true,
            messages = listOf(
                AnalyzedMessage("SimpleStringMessage", stringSchema)
            )
        )
        val generator = KotlinSpringKafkaGenerator(outputDir, packageName, packageName)
        generator.generate(listOf(channel))

        val handlerFile = outputDir.resolve(packageName.replace('.', '/') + "/SimpleTopicHandler.kt")
        assertTrue(handlerFile.exists())

        val content = handlerFile.readText()
        assertTrue(content.contains("fun onSimpleStringMessage(message: KafkaMessage<String>)"), "Should use String type")
    }
}
