package dev.banking.asyncapi.generator.core.generator.java.kafka

import dev.banking.asyncapi.generator.core.generator.AbstractJavaGeneratorClass
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GenerateJavaSpringKafkaTest : AbstractJavaGeneratorClass() {
    @Test
    fun `should generate spring kafka client for Java`() {
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

        val producerFile = producerDir.resolve("UserEventsProducer.java")
        assertTrue(producerFile.exists(), "Producer should be generated")
        val producerContent = producerFile.readText()
        assertTrue(producerContent.contains("interface UserEventsProducer {"))
        assertFalse(producerContent.contains("@Validated"))
        assertFalse(producerContent.contains("import jakarta.validation.Valid;"))
        assertTrue(producerContent.contains("Producer contract for publishing messages to the {@code user.events.v1} topic."))
        assertTrue(producerContent.contains("sendUserSignedUp"))
        assertTrue(producerContent.contains("@NotNull UserSignedUpPayload payload"))
        assertTrue(producerContent.contains("@NotNull String key"))
        assertFalse(producerContent.contains("KafkaTemplate"))
        assertFalse(producerContent.contains("ProducerRecord"))
        assertTrue(producerContent.contains("CompletableFuture<RecordMetadata> sendUserSignedUp"))
        assertTrue(!producerContent.contains("@Component"), "Producer should not be annotated")

        val consumerFile = consumerDir.resolve("UserEventsConsumer.java")
        assertTrue(consumerFile.exists(), "Consumer should be generated")
        val consumerContent = consumerFile.readText()
        assertTrue(consumerContent.contains("interface UserEventsConsumer"))
        assertFalse(consumerContent.contains("@Validated"))
        assertFalse(consumerContent.contains("import jakarta.validation.Valid;"))
        assertTrue(consumerContent.contains("Consumer contract for handling messages from the {@code user.events.v1} topic."))
        assertTrue(consumerContent.contains("void listenUserSignedUp"))
        assertTrue(consumerContent.contains("@NotNull UserSignedUpPayload payload"))
        assertTrue(consumerContent.contains("@Nullable String key"))
        assertFalse(consumerContent.contains("ConsumerRecord"))
        assertFalse(consumerContent.contains("default void"), "Consumer methods should be abstract")
        assertTrue(!consumerContent.contains("@KafkaListener"), "Consumer should not be annotated")
    }

    @Test
    fun `should generate header classes for spring kafka client in Java`() {
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

        val consumerContent = clientDir.resolve("consumer/UserEventsConsumer.java").readText()
        assertFalse(consumerContent.contains("import dev.banking.test.userservice.v1.client.header.TopicUserEventsHeadersUserSignup;"))
        assertFalse(consumerContent.contains("ConsumerRecord"))
        assertTrue(consumerContent.contains("void listen("))
        assertTrue(consumerContent.contains("@NotNull UserSignupPayload payload"))
        assertTrue(consumerContent.contains("@Nullable String key,"))
        assertTrue(consumerContent.contains("@Nullable String correlationId,"))
        assertTrue(consumerContent.contains("@Nullable String applicationInstanceId"))
        assertTrue(consumerContent.contains("@param correlationId Correlation ID set by application"))
        assertTrue(
            consumerContent.contains(
                "@param applicationInstanceId Unique identifier for a given instance of the publishing application",
            ),
        )
        assertFalse(consumerContent.contains("default void"), "Consumer methods should be abstract")

        val producerContent = clientDir.resolve("producer/UserEventsProducer.java").readText()
        assertFalse(producerContent.contains("import dev.banking.test.userservice.v1.client.header.TopicUserEventsHeadersUserSignup;"))
        assertTrue(producerContent.contains("import java.util.concurrent.CompletableFuture;"))
        assertTrue(producerContent.contains("import org.apache.kafka.clients.producer.RecordMetadata;"))
        assertFalse(producerContent.contains("import java.nio.charset.StandardCharsets;"))
        assertTrue(producerContent.contains("interface UserEventsProducer {"))
        assertTrue(producerContent.contains("CompletableFuture<RecordMetadata> send("))
        assertTrue(producerContent.contains("@NotNull UserSignupPayload payload"))
        assertTrue(producerContent.contains("@NotNull String key"))
        assertTrue(producerContent.contains("@Nullable String correlationId,"))
        assertTrue(producerContent.contains("@Nullable String applicationInstanceId"))
        assertTrue(producerContent.contains("@param correlationId Correlation ID set by application"))
        assertTrue(
            producerContent.contains(
                "@param applicationInstanceId Unique identifier for a given instance of the publishing application",
            ),
        )
        assertFalse(producerContent.contains("record.headers().add"))
    }

    @Test
    fun `should not reference typed headers when Kafka header generation is disabled for Java`() {
        val yaml = File("src/test/resources/generator/asyncapi_message_headers.yaml")
        val modelPackage = "dev.banking.test.userservice.v1.model"
        val clientPackage = "dev.banking.test.userservice.v1.client"
        val outputDir = File("target/generated-sources/asyncapi-java-spring-kafka-no-headers")
        val resourceOutputDirectory = File("target/generated-resources/asyncapi-java-spring-kafka-no-headers")
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

        val consumerContent = clientDir.resolve("consumer/UserEventsConsumer.java").readText()
        assertFalse(consumerContent.contains(".client.header."))
        assertFalse(consumerContent.contains("TopicUserEventsHeadersUserSignup"))
        assertFalse(consumerContent.contains("ConsumerRecord"))
        assertTrue(consumerContent.contains("void listen("))
        assertTrue(consumerContent.contains("@NotNull UserSignupPayload payload"))
        assertTrue(consumerContent.contains("@Nullable String key"))
        assertFalse(consumerContent.contains("correlationId"))
        assertFalse(consumerContent.contains("applicationInstanceId"))
        assertFalse(consumerContent.contains("default void"), "Consumer methods should be abstract")

        val producerContent = clientDir.resolve("producer/UserEventsProducer.java").readText()
        assertFalse(producerContent.contains(".client.header."))
        assertFalse(producerContent.contains("TopicUserEventsHeadersUserSignup"))
        assertFalse(producerContent.contains("StandardCharsets"))
        assertFalse(producerContent.contains("record.headers().add"))
        assertTrue(producerContent.contains("CompletableFuture<RecordMetadata> send("))
        assertTrue(producerContent.contains("@NotNull UserSignupPayload payload"))
        assertTrue(producerContent.contains("@NotNull String key"))
        assertFalse(producerContent.contains("correlationId"))
    }

    @Test
    fun `should generate spring kafka client with native avro payload type for Java`() {
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
        val consumerContent = clientDir.resolve("consumer/UserEventsConsumer.java").readText()
        val producerContent = clientDir.resolve("producer/UserEventsProducer.java").readText()

        assertTrue(consumerContent.contains("import com.example.avro.UserCreated;"))
        assertTrue(consumerContent.contains("@NotNull UserCreated payload"))
        assertTrue(consumerContent.contains("@Nullable String key"))
        assertFalse(consumerContent.contains("ConsumerRecord"))
        assertTrue(producerContent.contains("import com.example.avro.UserCreated;"))
        assertTrue(producerContent.contains("@NotNull UserCreated payload"))
        assertFalse(producerContent.contains("KafkaTemplate"))
        assertTrue(producerContent.contains("CompletableFuture<RecordMetadata> send("))
    }

    @Test
    fun `should generate spring kafka client with external native avro payload type for Java`() {
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
        val consumerContent = clientDir.resolve("consumer/UserEventsConsumer.java").readText()
        val producerContent = clientDir.resolve("producer/UserEventsProducer.java").readText()

        assertTrue(consumerContent.contains("import com.example.external.avro.UserCreatedAvro;"))
        assertTrue(consumerContent.contains("@NotNull UserCreatedAvro payload"))
        assertTrue(consumerContent.contains("@Nullable String key"))
        assertFalse(consumerContent.contains("ConsumerRecord"))
        assertTrue(producerContent.contains("import com.example.external.avro.UserCreatedAvro;"))
        assertTrue(producerContent.contains("@NotNull UserCreatedAvro payload"))
        assertFalse(producerContent.contains("KafkaTemplate"))
        assertTrue(producerContent.contains("CompletableFuture<RecordMetadata> sendUserCreatedAvro"))
    }

    @Test
    fun `should generate spring kafka client with native protobuf payload type for Java`() {
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
        val consumerContent = clientDir.resolve("consumer/UserEventsConsumer.java").readText()
        val producerContent = clientDir.resolve("producer/UserEventsProducer.java").readText()

        assertTrue(consumerContent.contains("import com.example.protobuf.UserCreated;"))
        assertTrue(consumerContent.contains("@NotNull UserCreated payload"))
        assertTrue(consumerContent.contains("@Nullable String key"))
        assertFalse(consumerContent.contains("ConsumerRecord"))
        assertTrue(producerContent.contains("import com.example.protobuf.UserCreated;"))
        assertTrue(producerContent.contains("@NotNull UserCreated payload"))
        assertFalse(producerContent.contains("KafkaTemplate"))
        assertTrue(producerContent.contains("CompletableFuture<RecordMetadata> send("))
    }

    @Test
    fun `should generate spring kafka client with external native protobuf payload type for Java`() {
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
        val consumerContent = clientDir.resolve("consumer/UserEventsConsumer.java").readText()
        val producerContent = clientDir.resolve("producer/UserEventsProducer.java").readText()

        assertTrue(consumerContent.contains("import com.example.external.protobuf.UserCreatedProtobuf;"))
        assertTrue(consumerContent.contains("@NotNull UserCreatedProtobuf payload"))
        assertTrue(consumerContent.contains("@Nullable String key"))
        assertFalse(consumerContent.contains("ConsumerRecord"))
        assertTrue(producerContent.contains("import com.example.external.protobuf.UserCreatedProtobuf;"))
        assertTrue(producerContent.contains("@NotNull UserCreatedProtobuf payload"))
        assertFalse(producerContent.contains("KafkaTemplate"))
        assertTrue(producerContent.contains("CompletableFuture<RecordMetadata> sendUserCreatedProtobuf"))
    }
}
