package dev.banking.asyncapi.generator.core.generator.kotlin.kafka

import dev.banking.asyncapi.generator.core.generator.AbstractKotlinGeneratorClass
import org.junit.jupiter.api.Test
import java.io.File
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
        assertTrue(producerContent.contains("class UserEventsProducerUserSignedUp"))
        assertTrue(producerContent.contains("KafkaTemplate<String, UserSignedUpPayload>"))
        assertTrue(producerContent.contains("sendUserSignedUp"))
        assertTrue(!producerContent.contains("@Component"), "Producer should not be annotated")

        val consumerFile = consumerDir.resolve("UserEventsConsumer.kt")
        assertTrue(consumerFile.exists(), "Consumer should be generated")
        val consumerContent = consumerFile.readText()
        assertTrue(consumerContent.contains("interface UserEventsConsumer"))
        assertTrue(consumerContent.contains("fun onUserSignedUp"))
        assertTrue(consumerContent.contains("ConsumerRecord<String, UserSignedUpPayload>"))
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
        val headerDir = outputDir.resolve("dev/banking/test/userservice/v1/client/header")
        assertTrue(headerDir.exists(), "Spring Kafka client should generate header classes")
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
        assertTrue(producerContent.contains("KafkaTemplate<String, UserCreated>"))
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
        assertTrue(producerContent.contains("KafkaTemplate<String, UserCreatedAvro>"))
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
        assertTrue(producerContent.contains("KafkaTemplate<String, UserCreated>"))
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
        assertTrue(producerContent.contains("KafkaTemplate<String, UserCreatedProtobuf>"))
    }
}
