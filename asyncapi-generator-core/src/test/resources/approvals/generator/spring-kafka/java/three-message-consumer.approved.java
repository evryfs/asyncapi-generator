package com.example.account.client.consumer;

import com.example.account.model.MyAccountClosedPayload;
import com.example.account.model.MyAccountCreatedPayload;
import com.example.account.model.MyAccountUpdatedPayload;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.lang.Nullable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.validation.annotation.Validated;

/**
 * Defines the Spring Kafka consumer contract for messages received from the {@code my.accounts.{environment}.lifecycle.v1}
 * AsyncAPI channel.
 *
 * <p>This interface does not register Kafka listeners. The application activates
 * consumption by implementing this contract in a Spring bean and configuring
 * {@code @KafkaListener} on the implementation.
 *
 * <p>The generated parameter annotations define how Spring binds the message
 * payload, Kafka record metadata, and any AsyncAPI-defined message headers.
 *
 * <p>Kafka metadata parameters use {@link KafkaHeaders}, provided by Spring Kafka.
 * See {@link KafkaHeaders} for the metadata constants available in the application's
 * Spring Kafka version.
 */
@Validated
public interface MyAccountLifecycleConsumer {
    /**
     * Kafka topic address declared by this AsyncAPI channel, with channel parameters
     * mapped to Spring property placeholders.
     *
     * <p>Use this constant in {@code @KafkaListener(topics = ...)}.
     *
     * <p>To inject the resolved topic address:
     *
     * <pre>{@code
     * public final class MyAccountLifecycleConsumerImpl {
     *     private final String topicAddress;
     *
     *     public MyAccountLifecycleConsumerImpl(
     *             @Value(MyAccountLifecycleConsumer.MY_ACCOUNT_LIFECYCLE_TOPIC_ADDRESS)
     *             String topicAddress) {
     *         this.topicAddress = topicAddress;
     *     }
     * }
     * }</pre>
     */
    String MY_ACCOUNT_LIFECYCLE_TOPIC_ADDRESS = "my.accounts.${kafka.environment}.lifecycle.v1";

    /**
     * Handles the {@code MyAccountCreated} message received from this channel.
     *
     * @param payload Details about a newly created account.
     * @param receivedTopic Kafka topic from which the record was received.
     * @param receivedKey Kafka record key, or {@code null} when the record has no key.
     * @param correlationId Identifier used to correlate related messages.
     * @param sourceSystem Optional name of the system that produced the message.
     */
    void listenMyAccountCreated(
        @Payload @Valid @NotNull MyAccountCreatedPayload payload,
        @Header(name = KafkaHeaders.RECEIVED_TOPIC, required = true) @NotNull String receivedTopic,
        @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) @Nullable String receivedKey,
        @Header(name = "correlationId", required = true) @NotNull String correlationId,
        @Header(name = "sourceSystem", required = false) @Nullable String sourceSystem
    );

    /**
     * Handles the {@code MyAccountUpdated} message received from this channel.
     *
     * @param payload Details about an account update.
     * @param receivedTopic Kafka topic from which the record was received.
     * @param receivedKey Kafka record key, or {@code null} when the record has no key.
     * @param correlationId Identifier used to correlate related messages.
     * @param sourceSystem Optional name of the system that produced the message.
     */
    void listenMyAccountUpdated(
        @Payload @Valid @NotNull MyAccountUpdatedPayload payload,
        @Header(name = KafkaHeaders.RECEIVED_TOPIC, required = true) @NotNull String receivedTopic,
        @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) @Nullable String receivedKey,
        @Header(name = "correlationId", required = true) @NotNull String correlationId,
        @Header(name = "sourceSystem", required = false) @Nullable String sourceSystem
    );

    /**
     * Handles the {@code MyAccountClosed} message received from this channel.
     *
     * @param payload Details about a closed account.
     * @param receivedTopic Kafka topic from which the record was received.
     * @param receivedKey Kafka record key, or {@code null} when the record has no key.
     * @param correlationId Identifier used to correlate related messages.
     * @param sourceSystem Optional name of the system that produced the message.
     */
    void listenMyAccountClosed(
        @Payload @Valid @NotNull MyAccountClosedPayload payload,
        @Header(name = KafkaHeaders.RECEIVED_TOPIC, required = true) @NotNull String receivedTopic,
        @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) @Nullable String receivedKey,
        @Header(name = "correlationId", required = true) @NotNull String correlationId,
        @Header(name = "sourceSystem", required = false) @Nullable String sourceSystem
    );
}
