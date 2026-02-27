package dev.banking.asyncapi.generator.core.generator.java.kafka

import dev.banking.asyncapi.generator.core.generator.AbstractJavaGeneratorClass
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertTrue

class GenerateJavaSpringKafkaClientTest : AbstractJavaGeneratorClass() {

    @Test
    fun `should generate full spring kafka ecosystem for Java`() {
        val yaml = File("src/test/resources/generator/asyncapi_spring_kafka_client_example.yaml")
        val modelPackage = "dev.banking.ace.userservice.v1.model"
        val clientPackage = "dev.banking.ace.userservice.v1.client"

        generateElement(
            yaml = yaml,
            modelPackage = modelPackage,
            clientPackage = clientPackage,
            generateModels = true,
            generateSpringKafkaClient = true,
        )

        val outputDir = File("target/generated-sources/asyncapi")
        val modelPath = "dev/banking/ace/userservice/v1/model"
        val clientPath = "dev/banking/ace/userservice/v1/client"

        val modelDir = outputDir.resolve(modelPath)
        assertTrue(modelDir.resolve("UserSignedUp.java").exists(), "Model UserSignedUp missing")
        assertTrue(modelDir.resolve("UserLoggedIn.java").exists(), "Model UserLoggedIn missing")

        val clientDir = outputDir.resolve(clientPath)
        assertTrue(clientDir.resolve("UserEventsListener.java").exists(), "Listener missing")
        assertTrue(clientDir.resolve("UserEventsHandler.java").exists(), "Handler missing")
        assertTrue(clientDir.resolve("UserEventsProducer.java").exists(), "Producer missing")

        val userListenerContent = clientDir.resolve("UserEventsListener.java").readText()
        assertTrue(userListenerContent.contains("if (payload instanceof UserSignedUp)"), "Instanceof check missing")
        assertTrue(userListenerContent.contains("import $modelPackage.UserSignedUp;"), "Import missing")
        assertTrue(
            userListenerContent.contains("import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;"),
            "Missing ConditionalOnBean import",
        )
        assertTrue(userListenerContent.contains("@ConditionalOnBean(UserEventsHandler.class)"), "Missing @ConditionalOnBean annotation")

        val userProducerContent = clientDir.resolve("UserEventsProducer.java").readText()
        assertTrue(
            userProducerContent.contains("@ConditionalOnProperty(name = \"kafka.topics.userEvents.topic\")"),
            "Missing @ConditionalOnProperty annotation",
        )
        assertTrue(
            userProducerContent.contains("@Value(\"\${kafka.topics.userEvents.topic}\")"),
            "Producer should read topic from kafka.topics.userEvents.topic",
        )
        assertTrue(
            userListenerContent.contains("@ConditionalOnProperty(name = \"kafka.topics.userEvents.topic\")"),
            "Listener should be conditional on topic property",
        )
        assertTrue(
            userListenerContent.contains("@KafkaListener(topics = \"\${kafka.topics.userEvents.topic}\""),
            "Listener should read topic from kafka.topics.userEvents.topic",
        )
        assertTrue(
            userListenerContent.contains("groupId = \"\${spring.kafka.consumer.group-id}\""),
            "Listener should use Spring groupId placeholder",
        )
    }

    @Test
    fun `should apply custom topic property prefix and suffix for Java`() {
        val yaml = File("src/test/resources/generator/asyncapi_spring_kafka_client_example.yaml")
        val modelPackage = "dev.banking.ace.userservice.v1.model"
        val clientPackage = "dev.banking.ace.userservice.v1.client"
        val outputDir = File("target/generated-sources/asyncapi-prefix-suffix-java")

        generateElement(
            yaml = yaml,
            modelPackage = modelPackage,
            clientPackage = clientPackage,
            generateModels = true,
            generateSpringKafkaClient = true,
            codegenOutputDirectory = outputDir,
            kafkaTopicsPropertyPrefix = "my.property",
            kafkaTopicsPropertySuffix = "name",
        )
        val clientDir = outputDir.resolve("dev/banking/ace/userservice/v1/client")
        val producerContent = clientDir.resolve("UserEventsProducer.java").readText()
        val listenerContent = clientDir.resolve("UserEventsListener.java").readText()
        assertTrue(
            producerContent.contains("@Value(\"\${my.property.userEvents.name}\")"),
            "Producer should use custom topic property key",
        )
        assertTrue(
            listenerContent.contains("@KafkaListener(topics = \"\${my.property.userEvents.name}\""),
            "Listener should use custom topic property key",
        )
    }
}
