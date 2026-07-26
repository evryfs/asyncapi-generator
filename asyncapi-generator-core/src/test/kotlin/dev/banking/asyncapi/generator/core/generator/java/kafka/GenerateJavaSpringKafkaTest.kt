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
        assertTrue(
            producerContent.contains(
                "Defines the Spring Kafka producer contract for messages published to the " +
                    "{@code user.events.v1} AsyncAPI channel.",
            ),
        )
        assertTrue(producerContent.contains("sendUserSignedUp"))
        assertTrue(producerContent.contains("@Payload UserSignedUpPayload payload"))
        assertFalse(producerContent.contains("messageKey"))
        assertFalse(producerContent.contains("jakarta.validation.constraints.NotNull"))
        assertTrue(producerContent.contains("@Payload"))
        assertFalse(producerContent.contains("import org.springframework.messaging.handler.annotation.Header;"))
        assertFalse(producerContent.contains("import org.springframework.kafka.support.KafkaHeaders;"))
        assertFalse(producerContent.contains("@Header("))
        assertFalse(producerContent.contains("import org.springframework.kafka.core.KafkaTemplate"))
        assertFalse(producerContent.contains("ProducerRecord"))
        assertTrue(producerContent.contains("default CompletableFuture<RecordMetadata> sendUserSignedUp"))
        assertTrue(producerContent.contains("CompletableFuture.failedFuture("))
        assertTrue(producerContent.contains("new UnsupportedOperationException("))
        assertFalse(
            producerContent.lineSequence().any { line -> line.trim() == "@Component" },
            "Producer should not be annotated",
        )

        val consumerFile = consumerDir.resolve("UserEventsConsumer.java")
        assertTrue(consumerFile.exists(), "Consumer should be generated")
        val consumerContent = consumerFile.readText()
        assertTrue(consumerContent.contains("interface UserEventsConsumer"))
        assertFalse(consumerContent.contains("@Validated"))
        assertFalse(consumerContent.contains("import jakarta.validation.Valid;"))
        assertTrue(
            consumerContent.contains(
                "Defines the Spring Kafka consumer contract for messages received from the " +
                    "{@code user.events.v1} AsyncAPI channel.",
            ),
        )
        assertTrue(consumerContent.contains("void listenUserSignedUp"))
        assertTrue(consumerContent.contains("@Payload UserSignedUpPayload payload"))
        assertTrue(consumerContent.contains("import jakarta.validation.constraints.NotNull;"))
        assertTrue(
            consumerContent.contains(
                "@Header(name = KafkaHeaders.RECEIVED_TOPIC, required = true) @NotNull String receivedTopic",
            ),
        )
        assertFalse(consumerContent.contains("receivedKey"))
        assertFalse(consumerContent.contains("KafkaHeaders.RECEIVED_KEY"))
        assertFalse(consumerContent.contains("ConsumerRecord"))
        assertTrue(consumerContent.contains("default void"), "Consumer methods should have no-op defaults")
        assertFalse(
            consumerContent.lineSequence().any { line -> line.trimStart().startsWith("@KafkaListener") },
            "Consumer should not be annotated",
        )
    }

    @Test
    fun `should generate message headers as individual spring kafka client parameters in Java`() {
        val yaml = File("src/test/resources/generator/asyncapi_message_headers.yaml")
        val modelPackage = "dev.banking.test.userservice.v1.model"
        val clientPackage = "dev.banking.test.userservice.v1.client"
        val outputDir = File("target/generated-sources/asyncapi-java-message-headers")
        val resourceOutputDirectory = File("target/generated-resources/asyncapi-java-message-headers")
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
        )

        val clientDir = outputDir.resolve("dev/banking/test/userservice/v1/client")
        val headerDir = outputDir.resolve("dev/banking/test/userservice/v1/client/header")
        assertFalse(headerDir.exists(), "Spring Kafka clients should not generate typed header models")

        val consumerContent = clientDir.resolve("consumer/UserEventsConsumer.java").readText()
        assertFalse(consumerContent.contains("import dev.banking.test.userservice.v1.client.header.TopicUserEventsHeadersUserSignup;"))
        assertFalse(consumerContent.contains("ConsumerRecord"))
        assertTrue(consumerContent.contains("void listenUserSignup("))
        assertTrue(consumerContent.contains("@Payload UserSignupPayload payload"))
        assertFalse(consumerContent.contains("receivedKey"))
        assertFalse(consumerContent.contains("KafkaHeaders.RECEIVED_KEY"))
        assertTrue(consumerContent.contains("@Nullable String correlationId,"))
        assertTrue(consumerContent.contains("@Nullable String applicationInstanceId"))
        assertTrue(
            consumerContent.contains(
                "@param correlationId Value bound from the {@code correlationId} Kafka message header.",
            ),
        )
        assertTrue(consumerContent.contains("Correlation ID set by application"))
        assertTrue(
            consumerContent.contains(
                "@param applicationInstanceId Value bound from the {@code applicationInstanceId} Kafka message header.",
            ),
        )
        assertTrue(consumerContent.contains("Unique identifier for a given instance"))
        assertTrue(consumerContent.contains("default void"), "Consumer methods should have no-op defaults")

        val producerContent = clientDir.resolve("producer/UserEventsProducer.java").readText()
        assertFalse(producerContent.contains("import dev.banking.test.userservice.v1.client.header.TopicUserEventsHeadersUserSignup;"))
        assertTrue(producerContent.contains("import java.util.concurrent.CompletableFuture;"))
        assertTrue(producerContent.contains("import org.apache.kafka.clients.producer.RecordMetadata;"))
        assertFalse(producerContent.contains("import java.nio.charset.StandardCharsets;"))
        assertTrue(producerContent.contains("interface UserEventsProducer {"))
        assertTrue(producerContent.contains("CompletableFuture<RecordMetadata> sendUserSignup("))
        assertTrue(producerContent.contains("@Payload UserSignupPayload payload"))
        assertFalse(producerContent.contains("messageKey"))
        assertTrue(producerContent.contains("import org.springframework.messaging.handler.annotation.Header;"))
        assertFalse(producerContent.contains("import org.springframework.kafka.support.KafkaHeaders;"))
        assertFalse(producerContent.contains("KafkaHeaders.KEY"))
        assertTrue(producerContent.contains("@Nullable String correlationId,"))
        assertTrue(producerContent.contains("@Nullable String applicationInstanceId"))
        assertTrue(
            producerContent.contains(
                "@param correlationId Value for the {@code correlationId} Kafka message header.",
            ),
        )
        assertTrue(producerContent.contains("Correlation ID set by application"))
        assertTrue(
            producerContent.contains(
                "@param applicationInstanceId Value for the {@code applicationInstanceId} Kafka message header.",
            ),
        )
        assertTrue(producerContent.contains("Unique identifier for a given instance"))
        assertFalse(producerContent.contains("record.headers().add"))
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
        assertTrue(consumerContent.contains("@Payload UserCreated payload"))
        assertFalse(consumerContent.contains("receivedKey"))
        assertFalse(consumerContent.contains("ConsumerRecord"))
        assertTrue(producerContent.contains("import com.example.avro.UserCreated;"))
        assertTrue(producerContent.contains("@Payload UserCreated payload"))
        assertFalse(producerContent.contains("import org.springframework.kafka.core.KafkaTemplate"))
        assertTrue(producerContent.contains("CompletableFuture<RecordMetadata> sendUserCreated("))
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
        assertTrue(consumerContent.contains("@Payload UserCreatedAvro payload"))
        assertFalse(consumerContent.contains("receivedKey"))
        assertFalse(consumerContent.contains("ConsumerRecord"))
        assertTrue(producerContent.contains("import com.example.external.avro.UserCreatedAvro;"))
        assertTrue(producerContent.contains("@Payload UserCreatedAvro payload"))
        assertFalse(producerContent.contains("import org.springframework.kafka.core.KafkaTemplate"))
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
        assertTrue(consumerContent.contains("@Payload UserCreated payload"))
        assertFalse(consumerContent.contains("receivedKey"))
        assertFalse(consumerContent.contains("ConsumerRecord"))
        assertTrue(producerContent.contains("import com.example.protobuf.UserCreated;"))
        assertTrue(producerContent.contains("@Payload UserCreated payload"))
        assertFalse(producerContent.contains("import org.springframework.kafka.core.KafkaTemplate"))
        assertTrue(producerContent.contains("CompletableFuture<RecordMetadata> sendUserCreated("))
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
        assertTrue(consumerContent.contains("@Payload UserCreatedProtobuf payload"))
        assertFalse(consumerContent.contains("receivedKey"))
        assertFalse(consumerContent.contains("ConsumerRecord"))
        assertTrue(producerContent.contains("import com.example.external.protobuf.UserCreatedProtobuf;"))
        assertTrue(producerContent.contains("@Payload UserCreatedProtobuf payload"))
        assertFalse(producerContent.contains("import org.springframework.kafka.core.KafkaTemplate"))
        assertTrue(producerContent.contains("CompletableFuture<RecordMetadata> sendUserCreatedProtobuf"))
    }
}
