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

        val modelFile = modelDir.resolve("DeadLetterQueueEventPayload.java")
        assertFalse(modelFile.exists(), "Open payload should not generate a model class")

        val handlerContent = handlerDir.resolve("TopicUserDlqHandlerDeadLetterQueueEvent.java").readText()
        assertTrue(handlerContent.contains("ConsumerRecord<String, Object>"))

        val listenerContent = listenerDir.resolve("TopicUserDlqListenerDeadLetterQueueEvent.java").readText()
        assertTrue(listenerContent.contains("ConsumerRecord<String, Object>"))

        val producerContent = producerDir.resolve("TopicUserDlqProducerDeadLetterQueueEvent.java").readText()
        assertTrue(producerContent.contains("KafkaTemplate<String, Object>"))
        assertTrue(producerContent.contains("void sendDeadLetterQueueEvent"))
    }

    @Test
    fun `should use Object for open payload in spring kafka simple clients`() {
        val yaml = File("src/test/resources/generator/asyncapi_open_payload_kafka_inline.yaml")
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

        val modelFile = modelDir.resolve("DeadLetterQueueEventPayload.java")
        assertFalse(modelFile.exists(), "Open payload should not generate a model class")

        val producerContent = producerDir.resolve("UserDlqProducerDeadLetterQueueEvent.java").readText()
        assertTrue(producerContent.contains("KafkaTemplate<String, Object>"))
        assertTrue(producerContent.contains("void sendDeadLetterQueueEvent"))

        val consumerContent = consumerDir.resolve("UserDlqConsumer.java").readText()
        assertTrue(consumerContent.contains("ConsumerRecord<String, Object>"))
        assertTrue(consumerContent.contains("void onDeadLetterQueueEvent"))
    }
}
