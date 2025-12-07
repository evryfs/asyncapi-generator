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
            generateSpringKafkaClient = true
        )

        val outputDir = File("target/generated-sources/asyncapi")
        val modelPath = "dev/banking/ace/userservice/v1/model"
        val clientPath = "dev/banking/ace/userservice/v1/client"

        val modelDir = outputDir.resolve(modelPath)
        assertTrue(modelDir.resolve("UserSignedUp.kt").exists(), "UserSignedUp model missing")
        assertTrue(modelDir.resolve("UserLoggedIn.kt").exists(), "UserLoggedIn model missing")

        val clientDir = outputDir.resolve(clientPath)
        assertTrue(clientDir.resolve("KafkaMessage.kt").exists(), "KafkaMessage wrapper missing")
        assertTrue(clientDir.resolve("UserEventsListener.kt").exists(), "UserEvents Listener missing")
        assertTrue(clientDir.resolve("UserEventsHandler.kt").exists(), "UserEvents Handler missing")
        assertTrue(clientDir.resolve("UserEventsProducer.kt").exists(), "UserEvents Producer missing")

        val userListenerContent = clientDir.resolve("UserEventsListener.kt").readText()
        assertTrue(userListenerContent.contains("is UserSignedUp"), "Listener dispatch missing UserSignedUp")
        assertTrue(userListenerContent.contains("import $modelPackage.UserSignedUp"), "Missing correct Model Import")
        assertTrue(userListenerContent.contains("import org.springframework.boot.autoconfigure.condition.ConditionalOnBean"), "Missing ConditionalOnBean import")
        assertTrue(userListenerContent.contains("@ConditionalOnBean(UserEventsHandler::class)"), "Missing @ConditionalOnBean annotation")
    }
}
