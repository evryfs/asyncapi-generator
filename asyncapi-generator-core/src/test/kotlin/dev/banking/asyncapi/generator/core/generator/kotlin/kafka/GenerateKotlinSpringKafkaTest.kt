package dev.banking.asyncapi.generator.core.generator.kotlin.kafka

import dev.banking.asyncapi.generator.core.generator.AbstractKotlinGeneratorClass
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GenerateKotlinSpringKafkaTest : AbstractKotlinGeneratorClass() {
    @Test
    fun `should generate spring kafka client`() {
        val yaml = File("src/test/resources/generator/asyncapi_spring_kafka_client_example.yaml")
        val modelPackage = "dev.banking.test.userservice.v1.model"
        val clientPackage = "dev.banking.test.userservice.v1.client"

        generateElement(
            yaml = yaml,
            modelPackage = modelPackage,
            clientPackage = clientPackage,
            generateModels = true,
            generateSpringKafkaClient = true,
        )

        val outputDir = File("target/generated-sources/asyncapi")
        val clientPath = "dev/banking/test/userservice/v1/client"
        val producerDir = outputDir.resolve("$clientPath/producer")
        val consumerDir = outputDir.resolve("$clientPath/consumer")

        val producerFile = producerDir.resolve("UserEventsProducerUserSignedUp.kt")
        assertTrue(producerFile.exists(), "Producer should be generated")
        val producerContent = producerFile.readText()
        assertTrue(producerContent.contains("interface UserEventsProducerUserSignedUp"))
        assertTrue(producerContent.contains("@Validated"))
        assertTrue(producerContent.contains("Producer contract for publishing messages to the `user.events.v1` topic."))
        assertTrue(producerContent.contains("sendUserSignedUp"))
        assertTrue(producerContent.contains("@param:Valid"))
        assertTrue(producerContent.contains("payload: UserSignedUpPayload"))
        assertTrue(producerContent.contains("key: String"))
        assertFalse(producerContent.contains("KafkaTemplate"))
        assertFalse(producerContent.contains("ProducerRecord"))
        assertFalse(producerContent.contains("CompletableFuture"))
        assertTrue(!producerContent.contains("@Component"), "Producer should not be annotated")

        val consumerFile = consumerDir.resolve("UserEventsConsumer.kt")
        assertTrue(consumerFile.exists(), "Consumer should be generated")
        val consumerContent = consumerFile.readText()
        assertTrue(consumerContent.contains("interface UserEventsConsumer"))
        assertTrue(consumerContent.contains("fun onUserSignedUp"))
        assertTrue(consumerContent.contains("ConsumerRecord<String, UserSignedUpPayload>"))
        assertFalse(consumerContent.contains("{ }"), "Consumer methods should be abstract")
        assertTrue(!consumerContent.contains("@KafkaListener"), "Consumer should not be annotated")
    }

    @Test
    fun `should generate header classes for spring kafka client`() {
        val yaml = File("src/test/resources/generator/asyncapi_message_headers.yaml")
        val modelPackage = "dev.banking.test.userservice.v1.model"
        val clientPackage = "dev.banking.test.userservice.v1.client"

        generateElement(
            yaml = yaml,
            modelPackage = modelPackage,
            clientPackage = clientPackage,
            generateModels = true,
            generateSpringKafkaClient = true,
        )

        val outputDir = File("target/generated-sources/asyncapi")
        val clientDir = outputDir.resolve("dev/banking/test/userservice/v1/client")
        val headerDir = outputDir.resolve("dev/banking/test/userservice/v1/client/header")
        assertTrue(headerDir.exists(), "Spring Kafka client should generate header classes")

        val consumerContent = clientDir.resolve("consumer/UserEventsConsumer.kt").readText()
        assertTrue(consumerContent.contains("import dev.banking.test.userservice.v1.client.header.TopicUserEventsHeadersUserSignup"))
        assertTrue(
            consumerContent.contains(
                "fun onUserSignup(record: ConsumerRecord<String, UserSignupPayload>, " +
                    "headers: TopicUserEventsHeadersUserSignup)",
            ),
        )
        assertFalse(consumerContent.contains("{ }"), "Consumer methods should be abstract")

        val producerContent = clientDir.resolve("producer/UserEventsProducerUserSignup.kt").readText()
        assertFalse(producerContent.contains("import dev.banking.test.userservice.v1.client.header.TopicUserEventsHeadersUserSignup"))
        assertFalse(producerContent.contains("import java.util.concurrent.CompletableFuture"))
        assertFalse(producerContent.contains("import org.springframework.kafka.support.SendResult"))
        assertTrue(producerContent.contains("interface UserEventsProducerUserSignup"))
        assertTrue(producerContent.contains("fun sendUserSignup("))
        assertTrue(producerContent.contains("payload: UserSignupPayload"))
        assertTrue(producerContent.contains("correlationId: String? = null"))
        assertTrue(producerContent.contains("applicationInstanceId: String? = null"))
        assertTrue(producerContent.contains("@param correlationId Correlation ID set by application"))
        assertTrue(
            producerContent.contains(
                "@param applicationInstanceId Unique identifier for a given instance of the publishing application",
            ),
        )
        assertFalse(producerContent.contains("record.headers().add"))
    }

    @Test
    fun `should not reference typed headers when Kafka header generation is disabled`() {
        val yaml = File("src/test/resources/generator/asyncapi_message_headers.yaml")
        val modelPackage = "dev.banking.test.userservice.v1.model"
        val clientPackage = "dev.banking.test.userservice.v1.client"
        val outputDir = File("target/generated-sources/asyncapi-kotlin-spring-kafka-no-headers")
        val resourceOutputDirectory = File("target/generated-resources/asyncapi-kotlin-spring-kafka-no-headers")
        outputDir.deleteRecursively()
        resourceOutputDirectory.deleteRecursively()

        generateElement(
            yaml = yaml,
            codegenOutputDirectory = outputDir,
            resourceOutputDirectory = resourceOutputDirectory,
            modelPackage = modelPackage,
            clientPackage = clientPackage,
            generateModels = true,
            generateSpringKafkaClient = true,
            generateKafkaHeaders = false,
        )

        val clientDir = outputDir.resolve("dev/banking/test/userservice/v1/client")
        val headerDir = clientDir.resolve("header")
        assertFalse(headerDir.exists(), "Header classes should not be generated when Kafka headers are disabled")

        val consumerContent = clientDir.resolve("consumer/UserEventsConsumer.kt").readText()
        assertFalse(consumerContent.contains(".client.header."))
        assertFalse(consumerContent.contains("TopicUserEventsHeadersUserSignup"))
        assertTrue(consumerContent.contains("fun onUserSignup(record: ConsumerRecord<String, UserSignupPayload>)"))
        assertFalse(consumerContent.contains("{ }"), "Consumer methods should be abstract")

        val producerContent = clientDir.resolve("producer/UserEventsProducerUserSignup.kt").readText()
        assertFalse(producerContent.contains(".client.header."))
        assertFalse(producerContent.contains("TopicUserEventsHeadersUserSignup"))
        assertFalse(producerContent.contains("record.headers().add"))
        assertFalse(producerContent.contains("correlationId:"))
        assertFalse(producerContent.contains("applicationInstanceId:"))
        assertTrue(producerContent.contains("fun sendUserSignup("))
        assertTrue(producerContent.contains("payload: UserSignupPayload"))
        assertFalse(producerContent.contains("CompletableFuture"))
    }

    @Test
    fun `should generate spring kafka client with native avro payload type`() {
        val yaml = File("src/test/resources/generator/asyncapi_native_avro_spring_kafka_client.yaml")
        val modelPackage = "dev.banking.test.userservice.v1.model"
        val clientPackage = "dev.banking.test.userservice.v1.client"

        generateElement(
            yaml = yaml,
            modelPackage = modelPackage,
            clientPackage = clientPackage,
            generateModels = false,
            generateSpringKafkaClient = true,
        )

        val outputDir = File("target/generated-sources/asyncapi")
        val clientDir = outputDir.resolve("dev/banking/test/userservice/v1/client")
        val consumerContent = clientDir.resolve("consumer/UserEventsConsumer.kt").readText()
        val producerContent = clientDir.resolve("producer/UserEventsProducerUserCreated.kt").readText()

        assertTrue(consumerContent.contains("import com.example.avro.UserCreated"))
        assertTrue(consumerContent.contains("ConsumerRecord<String, UserCreated>"))
        assertTrue(producerContent.contains("import com.example.avro.UserCreated"))
        assertTrue(producerContent.contains("payload: UserCreated"))
        assertFalse(producerContent.contains("KafkaTemplate"))
        assertFalse(producerContent.contains("CompletableFuture"))
    }

    @Test
    fun `should generate spring kafka client with external native avro payload type`() {
        val yaml = File("src/test/resources/generator/native-assets/asyncapi_external_native_schema_assets.yaml")
        val modelPackage = "dev.banking.test.userservice.v1.model"
        val clientPackage = "dev.banking.test.userservice.v1.client"

        generateElement(
            yaml = yaml,
            modelPackage = modelPackage,
            clientPackage = clientPackage,
            generateModels = false,
            generateSpringKafkaClient = true,
        )

        val outputDir = File("target/generated-sources/asyncapi")
        val clientDir = outputDir.resolve("dev/banking/test/userservice/v1/client")
        val consumerContent = clientDir.resolve("consumer/UserEventsConsumer.kt").readText()
        val producerContent = clientDir.resolve("producer/UserEventsProducerUserCreatedAvro.kt").readText()

        assertTrue(consumerContent.contains("import com.example.external.avro.UserCreatedAvro"))
        assertTrue(consumerContent.contains("ConsumerRecord<String, UserCreatedAvro>"))
        assertTrue(producerContent.contains("import com.example.external.avro.UserCreatedAvro"))
        assertTrue(producerContent.contains("payload: UserCreatedAvro"))
        assertFalse(producerContent.contains("KafkaTemplate"))
        assertFalse(producerContent.contains("CompletableFuture"))
    }

    @Test
    fun `should generate spring kafka client with native protobuf payload type`() {
        val yaml = File("src/test/resources/generator/asyncapi_native_protobuf_spring_kafka_client.yaml")
        val modelPackage = "dev.banking.test.userservice.v1.model"
        val clientPackage = "dev.banking.test.userservice.v1.client"

        generateElement(
            yaml = yaml,
            modelPackage = modelPackage,
            clientPackage = clientPackage,
            generateModels = false,
            generateSpringKafkaClient = true,
        )

        val outputDir = File("target/generated-sources/asyncapi")
        val clientDir = outputDir.resolve("dev/banking/test/userservice/v1/client")
        val consumerContent = clientDir.resolve("consumer/UserEventsConsumer.kt").readText()
        val producerContent = clientDir.resolve("producer/UserEventsProducerUserCreated.kt").readText()

        assertTrue(consumerContent.contains("import com.example.protobuf.UserCreated"))
        assertTrue(consumerContent.contains("ConsumerRecord<String, UserCreated>"))
        assertTrue(producerContent.contains("import com.example.protobuf.UserCreated"))
        assertTrue(producerContent.contains("payload: UserCreated"))
        assertFalse(producerContent.contains("KafkaTemplate"))
        assertFalse(producerContent.contains("CompletableFuture"))
    }

    @Test
    fun `should generate spring kafka client with external native protobuf payload type`() {
        val yaml = File("src/test/resources/generator/native-assets/asyncapi_external_native_schema_assets.yaml")
        val modelPackage = "dev.banking.test.userservice.v1.model"
        val clientPackage = "dev.banking.test.userservice.v1.client"

        generateElement(
            yaml = yaml,
            modelPackage = modelPackage,
            clientPackage = clientPackage,
            generateModels = false,
            generateSpringKafkaClient = true,
        )

        val outputDir = File("target/generated-sources/asyncapi")
        val clientDir = outputDir.resolve("dev/banking/test/userservice/v1/client")
        val consumerContent = clientDir.resolve("consumer/UserEventsConsumer.kt").readText()
        val producerContent = clientDir.resolve("producer/UserEventsProducerUserCreatedProtobuf.kt").readText()

        assertTrue(consumerContent.contains("import com.example.external.protobuf.UserCreatedProtobuf"))
        assertTrue(consumerContent.contains("ConsumerRecord<String, UserCreatedProtobuf>"))
        assertTrue(producerContent.contains("import com.example.external.protobuf.UserCreatedProtobuf"))
        assertTrue(producerContent.contains("payload: UserCreatedProtobuf"))
        assertFalse(producerContent.contains("KafkaTemplate"))
        assertFalse(producerContent.contains("CompletableFuture"))
    }
}
