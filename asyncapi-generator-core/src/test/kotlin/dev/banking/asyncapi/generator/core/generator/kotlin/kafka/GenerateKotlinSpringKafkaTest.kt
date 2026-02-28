package dev.banking.asyncapi.generator.core.generator.kotlin.kafka

import dev.banking.asyncapi.generator.core.generator.AbstractKotlinGeneratorClass
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertTrue

class GenerateKotlinSpringKafkaTest : AbstractKotlinGeneratorClass() {

    @Test
    fun `should generate full spring kafka ecosystem`() {
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
        assertTrue(modelDir.resolve("UserSignedUp.kt").exists(), "UserSignedUp model missing")
        assertTrue(modelDir.resolve("UserLoggedIn.kt").exists(), "UserLoggedIn model missing")

        val clientDir = outputDir.resolve(clientPath)
        assertTrue(clientDir.resolve("UserEventsListener.kt").exists(), "UserEvents Listener missing")
        assertTrue(clientDir.resolve("UserEventsHandler.kt").exists(), "UserEvents Handler missing")
        assertTrue(clientDir.resolve("UserEventsProducer.kt").exists(), "UserEvents Producer missing")

        val userListenerContent = clientDir.resolve("UserEventsListener.kt").readText()
        assertTrue(userListenerContent.contains("is UserSignedUp"), "Listener dispatch missing UserSignedUp")
        assertTrue(userListenerContent.contains("import $modelPackage.UserSignedUp"), "Missing correct Model Import")
        assertTrue(
            userListenerContent.contains("import org.springframework.boot.autoconfigure.condition.ConditionalOnBean"),
            "Missing ConditionalOnBean import",
        )
        assertTrue(userListenerContent.contains("@ConditionalOnBean(UserEventsHandler::class)"), "Missing @ConditionalOnBean annotation")

        val userProducerContent = clientDir.resolve("UserEventsProducer.kt").readText()
        assertTrue(
            userProducerContent.contains("@ConditionalOnProperty(name = [\"kafka.topics.userEvents.topic\"])"),
            "Missing @ConditionalOnProperty annotation",
        )
        assertTrue(
            userProducerContent.contains("@Value(\"\\\${kafka.topics.userEvents.topic}\")"),
            "Producer should read topic from kafka.topics.userEvents.topic",
        )
        assertTrue(
            userListenerContent.contains("@ConditionalOnProperty(name = [\"kafka.topics.userEvents.topic\"])"),
            "Listener should be conditional on topic property",
        )
        assertTrue(
            userListenerContent.contains("@KafkaListener(topics = [\"\\\${kafka.topics.userEvents.topic}\"]"),
            "Listener should read topic from kafka.topics.userEvents.topic",
        )
    }

    @Test
    fun `should apply custom topic property prefix and suffix`() {
        val yaml = File("src/test/resources/generator/asyncapi_spring_kafka_client_example.yaml")
        val modelPackage = "dev.banking.ace.userservice.v1.model"
        val clientPackage = "dev.banking.ace.userservice.v1.client"
        val outputDir = File("target/generated-sources/asyncapi-prefix-suffix")

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
        val producerContent = clientDir.resolve("UserEventsProducer.kt").readText()
        val listenerContent = clientDir.resolve("UserEventsListener.kt").readText()
        assertTrue(
            producerContent.contains("@Value(\"\\\${my.property.userEvents.name}\")"),
            "Producer should use custom topic property key",
        )
        assertTrue(
            listenerContent.contains("@KafkaListener(topics = [\"\\\${my.property.userEvents.name}\"]"),
            "Listener should use custom topic property key",
        )
    }
}
