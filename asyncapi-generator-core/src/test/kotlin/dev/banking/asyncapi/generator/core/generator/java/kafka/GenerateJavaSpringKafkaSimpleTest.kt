package dev.banking.asyncapi.generator.core.generator.java.kafka

import dev.banking.asyncapi.generator.core.generator.AbstractJavaGeneratorClass
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertTrue

class GenerateJavaSpringKafkaSimpleTest : AbstractJavaGeneratorClass() {
    @Test
    fun `should generate simple spring kafka client for Java`() {
        val yaml = File("src/test/resources/generator/asyncapi_spring_kafka_client_example.yaml")
        val modelPackage = "dev.banking.ace.userservice.v1.model"
        val clientPackage = "dev.banking.ace.userservice.v1.client"

        generateElement(
            yaml = yaml,
            modelPackage = modelPackage,
            clientPackage = clientPackage,
            generateModels = true,
            generateSpringKafkaClient = true,
            configOptions = mapOf("client.type" to "spring-kafka-simple"),
        )

        val outputDir = File("target/generated-sources/asyncapi")
        val clientPath = "dev/banking/ace/userservice/v1/client"
        val producerDir = outputDir.resolve("$clientPath/producer")
        val consumerDir = outputDir.resolve("$clientPath/consumer")

        val producerFile = producerDir.resolve("UserEventsProducerUserSignedUp.java")
        assertTrue(producerFile.exists(), "Simple producer should be generated")
        val producerContent = producerFile.readText()
        assertTrue(producerContent.contains("class UserEventsProducerUserSignedUp"))
        assertTrue(producerContent.contains("KafkaTemplate<String, UserSignedUp>"))
        assertTrue(producerContent.contains("sendUserSignedUp"))
        assertTrue(!producerContent.contains("@Component"), "Simple producer should not be annotated")

        val consumerFile = consumerDir.resolve("UserEventsConsumer.java")
        assertTrue(consumerFile.exists(), "Simple consumer should be generated")
        val consumerContent = consumerFile.readText()
        assertTrue(consumerContent.contains("interface UserEventsConsumer"))
        assertTrue(consumerContent.contains("default void onUserSignedUp"))
        assertTrue(consumerContent.contains("ConsumerRecord<String, UserSignedUp>"))
        assertTrue(!consumerContent.contains("@KafkaListener"), "Simple consumer should not be annotated")
    }
}
