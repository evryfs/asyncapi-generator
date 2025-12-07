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
            generateSpringKafkaClient = true
        )

        val outputDir = File("target/generated-sources/asyncapi")
        val modelPath = "com/tietoevry/banking/ace/userservice/v1/model"
        val clientPath = "com/tietoevry/banking/ace/userservice/v1/client"

        val modelDir = outputDir.resolve(modelPath)
        assertTrue(modelDir.resolve("UserSignedUp.java").exists(), "Model UserSignedUp missing")
        assertTrue(modelDir.resolve("UserLoggedIn.java").exists(), "Model UserLoggedIn missing")

        val clientDir = outputDir.resolve(clientPath)
        assertTrue(clientDir.resolve("KafkaMessage.java").exists(), "KafkaMessage missing")
        assertTrue(clientDir.resolve("UserEventsListener.java").exists(), "Listener missing")
        assertTrue(clientDir.resolve("UserEventsHandler.java").exists(), "Handler missing")
        assertTrue(clientDir.resolve("UserEventsProducer.java").exists(), "Producer missing")

        val userListenerContent = clientDir.resolve("UserEventsListener.java").readText()
        assertTrue(userListenerContent.contains("if (payload instanceof UserSignedUp)"), "Instanceof check missing")
        assertTrue(userListenerContent.contains("import $modelPackage.UserSignedUp;"), "Import missing")
        assertTrue(userListenerContent.contains("import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;"), "Missing ConditionalOnBean import")
        assertTrue(userListenerContent.contains("@ConditionalOnBean(UserEventsHandler.class)"), "Missing @ConditionalOnBean annotation")
    }
}
