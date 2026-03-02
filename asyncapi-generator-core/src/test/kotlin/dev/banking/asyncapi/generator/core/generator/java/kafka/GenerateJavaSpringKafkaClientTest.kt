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
        val autoconfigDir = clientDir.resolve("config")
        val handlerDir = clientDir.resolve("handler")
        val listenerDir = clientDir.resolve("listener")
        val producerDir = clientDir.resolve("producer")
        assertTrue(
            listenerDir.resolve("TopicUserEventsListenerUserSignedUp.java").exists(),
            "UserSignedUp Listener missing",
        )
        assertTrue(
            handlerDir.resolve("TopicUserEventsHandlerUserSignedUp.java").exists(),
            "UserSignedUp Handler missing",
        )
        assertTrue(
            listenerDir.resolve("TopicUserEventsListenerUserLoggedIn.java").exists(),
            "UserLoggedIn Listener missing",
        )
        assertTrue(
            handlerDir.resolve("TopicUserEventsHandlerUserLoggedIn.java").exists(),
            "UserLoggedIn Handler missing",
        )
        assertTrue(producerDir.resolve("TopicUserEventsProducerUserSignedUp.java").exists(), "UserSignedUp Producer missing")
        assertTrue(producerDir.resolve("TopicUserEventsProducerUserLoggedIn.java").exists(), "UserLoggedIn Producer missing")
        val userSignedUpListenerContent = listenerDir.resolve("TopicUserEventsListenerUserSignedUp.java").readText()
        assertTrue(
            userSignedUpListenerContent.contains("ConsumerRecord<String, UserSignedUp>"),
            "Listener should be typed to UserSignedUp",
        )
        assertTrue(userSignedUpListenerContent.contains("import $modelPackage.UserSignedUp;"), "Import missing")
        assertTrue(
            userSignedUpListenerContent.contains("import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;"),
            "Missing ConditionalOnBean import",
        )
        assertTrue(
            userSignedUpListenerContent.contains("@ConditionalOnBean(TopicUserEventsHandlerUserSignedUp.class)"),
            "Missing @ConditionalOnBean annotation",
        )
        val userProducerContent = producerDir.resolve("TopicUserEventsProducerUserSignedUp.java").readText()
        assertTrue(
            userProducerContent.contains("@ConditionalOnProperty(name = \"kafka.topics.userEvents.topic\")"),
            "Missing @ConditionalOnProperty annotation",
        )
        assertTrue(
            userProducerContent.contains("@Value(\"\${kafka.topics.userEvents.topic}\")"),
            "Producer should read topic from kafka.topics.userEvents.topic",
        )
        assertTrue(
            userSignedUpListenerContent.contains("@ConditionalOnProperty(name = \"kafka.topics.userEvents.topic\")"),
            "Listener should be conditional on topic property",
        )
        assertTrue(
            userSignedUpListenerContent.contains("@KafkaListener(topics = \"\${kafka.topics.userEvents.topic}\""),
            "Listener should read topic from kafka.topics.userEvents.topic",
        )
        assertTrue(
            userSignedUpListenerContent.contains("groupId = \"\${spring.kafka.consumer.group-id}\""),
            "Listener should use Spring groupId placeholder",
        )

        val autoConfigContent = autoconfigDir.resolve("AsyncApiKafkaAutoConfiguration.java").readText()
        assertTrue(autoConfigContent.contains("@ComponentScan"), "Auto-configuration should include ComponentScan")
        assertTrue(
            autoConfigContent.contains("basePackages = \"$clientPackage\""),
            "Auto-configuration should scan the client package",
        )

        val importsFile =
            File("target/generated-resources/asyncapi/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports")
        assertTrue(importsFile.exists(), "Auto-configuration imports file should be generated")
        val importsContent = importsFile.readText()
        assertTrue(
            importsContent.contains("$clientPackage.config.AsyncApiKafkaAutoConfiguration"),
            "Auto-configuration imports should include generated config",
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
        val producerDir = clientDir.resolve("producer")
        val listenerDir = clientDir.resolve("listener")
        val producerContent = producerDir.resolve("TopicUserEventsProducerUserSignedUp.java").readText()
        val listenerContent = listenerDir.resolve("TopicUserEventsListenerUserSignedUp.java").readText()
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
