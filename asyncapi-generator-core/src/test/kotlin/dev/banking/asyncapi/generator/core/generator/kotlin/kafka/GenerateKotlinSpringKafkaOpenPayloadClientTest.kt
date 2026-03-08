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

        val modelFile = modelDir.resolve("DeadLetterQueueEvent.kt")
        assertTrue(modelFile.exists(), "DeadLetterQueueEvent typealias should be generated")

        val handlerContent = handlerDir.resolve("TopicUserDlqHandlerDeadLetterQueueEvent.kt").readText()
        assertTrue(handlerContent.contains("ConsumerRecord<String, DeadLetterQueueEvent>"))
        assertTrue(handlerContent.contains("import $modelPackage.DeadLetterQueueEvent"))

        val listenerContent = listenerDir.resolve("TopicUserDlqListenerDeadLetterQueueEvent.kt").readText()
        assertTrue(listenerContent.contains("ConsumerRecord<String, DeadLetterQueueEvent>"))
        assertTrue(listenerContent.contains("import $modelPackage.DeadLetterQueueEvent"))

        val producerContent = producerDir.resolve("TopicUserDlqProducerDeadLetterQueueEvent.kt").readText()
        assertTrue(producerContent.contains("KafkaTemplate<String, DeadLetterQueueEvent>"))
        assertTrue(producerContent.contains("fun sendDeadLetterQueueEvent"))
        assertTrue(producerContent.contains("import $modelPackage.DeadLetterQueueEvent"))
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

        val modelFile = modelDir.resolve("DeadLetterQueueEvent.kt")
        assertTrue(modelFile.exists(), "DeadLetterQueueEvent typealias should be generated")

        val producerContent = producerDir.resolve("UserDlqProducerDeadLetterQueueEvent.kt").readText()
        assertTrue(producerContent.contains("KafkaTemplate<String, DeadLetterQueueEvent>"))
        assertTrue(producerContent.contains("fun sendDeadLetterQueueEvent"))
        assertTrue(producerContent.contains("import $modelPackage.DeadLetterQueueEvent"))

        val consumerContent = consumerDir.resolve("UserDlqConsumer.kt").readText()
        assertTrue(consumerContent.contains("ConsumerRecord<String, DeadLetterQueueEvent>"))
        assertTrue(consumerContent.contains("fun onDeadLetterQueueEvent"))
        assertTrue(consumerContent.contains("import $modelPackage.DeadLetterQueueEvent"))
    }

    @Test
    fun `should use typealias for open payload inline in spring kafka simple clients`() {
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

        val modelFile = modelDir.resolve("DeadLetterQueueEventPayload.kt")
        assertTrue(modelFile.exists(), "DeadLetterQueueEventPayload typealias should be generated")

        val producerContent = producerDir.resolve("UserDlqProducerDeadLetterQueueEvent.kt").readText()
        assertTrue(producerContent.contains("KafkaTemplate<String, DeadLetterQueueEventPayload>"))
        assertTrue(producerContent.contains("fun sendDeadLetterQueueEvent"))
        assertTrue(producerContent.contains("import $modelPackage.DeadLetterQueueEventPayload"))

        val consumerContent = consumerDir.resolve("UserDlqConsumer.kt").readText()
        assertTrue(consumerContent.contains("ConsumerRecord<String, DeadLetterQueueEventPayload>"))
        assertTrue(consumerContent.contains("fun onDeadLetterQueueEvent"))
        assertTrue(consumerContent.contains("import $modelPackage.DeadLetterQueueEventPayload"))
    }
}
