package com.example.account.client.producer;

import com.example.account.model.MyAccountUpdatedPayload;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.producer.RecordMetadata;
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
     * @param messageKey Kafka record key.
     * @param X_EXAMPLE_CORRELATION_ID Value for the {@code X-EXAMPLE-CORRELATION-ID} Kafka message header. Implementations must add this value to the outgoing
     * Kafka record. Identifier used to correlate related messages.
     * @param X_EXAMPLE_SOURCE_SYSTEM Value for the {@code X-EXAMPLE-SOURCE-SYSTEM} Kafka message header. Implementations must add this value to the outgoing
     * Kafka record. Optional name of the system that produced the message.
     * @return future completed with Kafka record metadata after successful publication
     */
    CompletableFuture<RecordMetadata> send(
        @Payload @Valid MyAccountUpdatedPayload payload,
        String messageKey,
        @Header(name = "X-EXAMPLE-CORRELATION-ID", required = true) @NotNull String X_EXAMPLE_CORRELATION_ID,
        @Header(name = "X-EXAMPLE-SOURCE-SYSTEM", required = false) @Nullable String X_EXAMPLE_SOURCE_SYSTEM
    );
}
