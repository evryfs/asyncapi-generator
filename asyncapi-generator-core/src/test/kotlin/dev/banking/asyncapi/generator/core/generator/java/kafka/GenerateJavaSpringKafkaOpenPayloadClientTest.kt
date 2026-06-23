package dev.banking.asyncapi.generator.core.generator.java.kafka

import dev.banking.asyncapi.generator.core.generator.AbstractJavaGeneratorClass
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GenerateJavaSpringKafkaOpenPayloadClientTest : AbstractJavaGeneratorClass() {

    @Test
    fun `should use Object for open payload in spring kafka clients`() {
        val yaml = File("src/test/resources/generator/asyncapi_open_payload_kafka_inline.yaml")
        val modelPackage = "dev.banking.test.raw.model"
        val clientPackage = "dev.banking.test.raw.client"

        generateElement(
            yaml = yaml,
            modelPackage = modelPackage,
            clientPackage = clientPackage,
            generateModels = true,
            generateSpringKafkaClient = true,
        )

        val outputDir = File("target/generated-sources/asyncapi")
        val modelDir = outputDir.resolve("dev/banking/test/raw/model")
        val producerDir = outputDir.resolve("dev/banking/test/raw/client/producer")
        val consumerDir = outputDir.resolve("dev/banking/test/raw/client/consumer")

        val modelFile = modelDir.resolve("RawEventPayload.java")
        assertFalse(modelFile.exists(), "Open payload should not generate a model class")

        val producerContent = producerDir.resolve("UserRawEventsProducerRawEvent.java").readText()
        assertTrue(producerContent.contains("KafkaTemplate<String, Object>"))
        assertTrue(producerContent.contains("CompletableFuture<SendResult<String, Object>> sendRawEvent"))

        val consumerContent = consumerDir.resolve("UserRawEventsConsumer.java").readText()
        assertTrue(consumerContent.contains("ConsumerRecord<String, Object>"))
        assertTrue(consumerContent.contains("void onRawEvent"))
    }
}
