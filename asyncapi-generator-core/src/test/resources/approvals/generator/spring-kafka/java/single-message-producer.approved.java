package com.example.account.client.producer;

import com.example.account.model.MyAccountUpdatedPayload;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.lang.Nullable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.validation.annotation.Validated;

/**
 * Producer contract for publishing messages to the {@code my.accounts.{environment}.updated.v1} topic.
 * The contract exposes the Kafka record key, message payload, and contract-defined headers as method parameters.
 */
@Validated
public interface MyAccountUpdatedProducer {
    /** Spring-resolvable Kafka topic address declared by this AsyncAPI channel. */
    String MY_ACCOUNT_UPDATED_TOPIC_ADDRESS = "my.accounts.${kafka.environment}.updated.v1";

    /**
     * @param payload Details about an account update.
     * @param key Kafka record key.
     * @param correlationId Identifier used to correlate related messages.
     * @param sourceSystem Optional name of the system that produced the message.
     * @return future completed with Kafka record metadata after successful publication
     */
    CompletableFuture<RecordMetadata> send(
        @Payload @Valid @NotNull MyAccountUpdatedPayload payload,
        @Header(name = KafkaHeaders.KEY, required = true) @NotNull String key,
        @Header(name = "correlationId", required = true) @NotNull String correlationId,
        @Header(name = "sourceSystem", required = false) @Nullable String sourceSystem
    );
}
