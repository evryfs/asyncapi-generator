package dev.banking.asyncapi.generator.core.generator.kotlin.kafka

import dev.banking.asyncapi.generator.core.generator.AbstractKotlinGeneratorClass
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GenerateKotlinSpringKafkaOpenPayloadClientTest : AbstractKotlinGeneratorClass() {
    @Test
    fun `should use typealias for open payload in spring kafka clients`() {
        val yaml = File("src/test/resources/generator/asyncapi_open_payload_kafka.yaml")
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

        val modelFile = modelDir.resolve("RawEvent.kt")
        assertTrue(modelFile.exists(), "RawEvent typealias should be generated")

        val producerContent = producerDir.resolve("UserRawEventsProducerRawEvent.kt").readText()
        assertTrue(producerContent.contains("interface UserRawEventsProducerRawEvent"))
        assertTrue(producerContent.contains("fun sendRawEvent"))
        assertTrue(producerContent.contains("payload: RawEvent"))
        assertTrue(producerContent.contains("import $modelPackage.RawEvent"))
        assertFalse(producerContent.contains("KafkaTemplate"))
        assertFalse(producerContent.contains("CompletableFuture"))

        val consumerContent = consumerDir.resolve("UserRawEventsConsumer.kt").readText()
        assertTrue(consumerContent.contains("interface UserRawEventsConsumer"))
        assertTrue(consumerContent.contains("fun onRawEvent"))
        assertTrue(consumerContent.contains("payload: RawEvent"))
        assertTrue(consumerContent.contains("key: String?"))
        assertFalse(consumerContent.contains("ConsumerRecord"))
        assertTrue(consumerContent.contains("import $modelPackage.RawEvent"))
    }

    @Test
    fun `should use typealias for open payload inline in spring kafka clients`() {
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

        val modelFile = modelDir.resolve("RawEventPayload.kt")
        assertTrue(modelFile.exists(), "RawEventPayload typealias should be generated")

        val producerContent = producerDir.resolve("UserRawEventsProducerRawEvent.kt").readText()
        assertTrue(producerContent.contains("interface UserRawEventsProducerRawEvent"))
        assertTrue(producerContent.contains("fun sendRawEvent"))
        assertTrue(producerContent.contains("payload: RawEventPayload"))
        assertTrue(producerContent.contains("import $modelPackage.RawEventPayload"))
        assertFalse(producerContent.contains("KafkaTemplate"))
        assertFalse(producerContent.contains("CompletableFuture"))

        val consumerContent = consumerDir.resolve("UserRawEventsConsumer.kt").readText()
        assertTrue(consumerContent.contains("interface UserRawEventsConsumer"))
        assertTrue(consumerContent.contains("fun onRawEvent"))
        assertTrue(consumerContent.contains("payload: RawEventPayload"))
        assertTrue(consumerContent.contains("key: String?"))
        assertFalse(consumerContent.contains("ConsumerRecord"))
        assertTrue(consumerContent.contains("import $modelPackage.RawEventPayload"))
    }
}
