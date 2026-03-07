package dev.banking.asyncapi.generator.core.generator.kotlin.kafka

import dev.banking.asyncapi.generator.core.generator.AbstractKotlinGeneratorClass
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertTrue

class GenerateKotlinSpringKafkaOpenPayloadClientTest : AbstractKotlinGeneratorClass() {

    @Test
    fun `should use typealias for open payload in spring kafka clients`() {
        val yaml = File("src/test/resources/generator/asyncapi_open_payload_kafka.yaml")
        val modelPackage = "dev.banking.test.dlq.model"
        val clientPackage = "dev.banking.test.dlq.client"

        generateElement(
            yaml = yaml,
            modelPackage = modelPackage,
            clientPackage = clientPackage,
            generateModels = true,
            generateSpringKafkaClient = true,
        )

        val outputDir = File("target/generated-sources/asyncapi")
        val modelDir = outputDir.resolve("dev/banking/test/dlq/model")
        val handlerDir = outputDir.resolve("dev/banking/test/dlq/client/handler")
        val listenerDir = outputDir.resolve("dev/banking/test/dlq/client/listener")
        val producerDir = outputDir.resolve("dev/banking/test/dlq/client/producer")

        val modelFile = modelDir.resolve("DlqPayload.kt")
        assertTrue(modelFile.exists(), "DlqPayload typealias should be generated")

        val handlerContent = handlerDir.resolve("TopicUserDlqHandlerDlqPayload.kt").readText()
        assertTrue(handlerContent.contains("ConsumerRecord<String, DlqPayload>"))
        assertTrue(handlerContent.contains("import $modelPackage.DlqPayload"))

        val listenerContent = listenerDir.resolve("TopicUserDlqListenerDlqPayload.kt").readText()
        assertTrue(listenerContent.contains("ConsumerRecord<String, DlqPayload>"))
        assertTrue(listenerContent.contains("import $modelPackage.DlqPayload"))

        val producerContent = producerDir.resolve("TopicUserDlqProducerDlqPayload.kt").readText()
        assertTrue(producerContent.contains("KafkaTemplate<String, DlqPayload>"))
        assertTrue(producerContent.contains("fun sendDlqPayload"))
        assertTrue(producerContent.contains("import $modelPackage.DlqPayload"))
    }

    @Test
    fun `should use typealias for open payload in spring kafka simple clients`() {
        val yaml = File("src/test/resources/generator/asyncapi_open_payload_kafka.yaml")
        val modelPackage = "dev.banking.test.dlq.model"
        val clientPackage = "dev.banking.test.dlq.client"

        generateElement(
            yaml = yaml,
            modelPackage = modelPackage,
            clientPackage = clientPackage,
            generateModels = true,
            generateSpringKafkaClient = true,
            configOptions = mapOf("client.type" to "spring-kafka-simple"),
        )

        val outputDir = File("target/generated-sources/asyncapi")
        val modelDir = outputDir.resolve("dev/banking/test/dlq/model")
        val producerDir = outputDir.resolve("dev/banking/test/dlq/client/producer")
        val consumerDir = outputDir.resolve("dev/banking/test/dlq/client/consumer")

        val modelFile = modelDir.resolve("DlqPayload.kt")
        assertTrue(modelFile.exists(), "DlqPayload typealias should be generated")

        val producerContent = producerDir.resolve("UserDlqProducerDlqPayload.kt").readText()
        assertTrue(producerContent.contains("KafkaTemplate<String, DlqPayload>"))
        assertTrue(producerContent.contains("fun sendDlqPayload"))
        assertTrue(producerContent.contains("import $modelPackage.DlqPayload"))

        val consumerContent = consumerDir.resolve("UserDlqConsumer.kt").readText()
        assertTrue(consumerContent.contains("ConsumerRecord<String, DlqPayload>"))
        assertTrue(consumerContent.contains("fun onDlqPayload"))
        assertTrue(consumerContent.contains("import $modelPackage.DlqPayload"))
    }
}
