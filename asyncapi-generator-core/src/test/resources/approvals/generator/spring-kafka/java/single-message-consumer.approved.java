package com.example.account.client.consumer;

import com.example.account.model.MyAccountUpdatedPayload;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.lang.Nullable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.validation.annotation.Validated;

/**
 * Consumer contract for handling messages from the {@code my.accounts.{environment}.updated.v1} topic.
 * The contract exposes the Kafka record key, message payload, and contract-defined headers as method parameters.
 */
@Validated
public interface MyAccountUpdatedConsumer {
    /** Spring-resolvable Kafka topic address declared by this AsyncAPI channel. */
    String MY_ACCOUNT_UPDATED_TOPIC_ADDRESS = "my.accounts.${kafka.environment}.updated.v1";

    /**
     * @param payload Details about an account update.
     * @param topic Kafka topic from which the record was received.
     * @param key Kafka record key.
     * @param correlationId Identifier used to correlate related messages.
     * @param sourceSystem Optional name of the system that produced the message.
     */
    void listen(
        @Payload @Valid @NotNull MyAccountUpdatedPayload payload,
        @Header(name = KafkaHeaders.RECEIVED_TOPIC, required = true) @NotNull String topic,
        @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) @Nullable String key,
        @Header(name = "correlationId", required = true) @NotNull String correlationId,
        @Header(name = "sourceSystem", required = false) @Nullable String sourceSystem
    );
}
