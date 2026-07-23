package com.example.account.client.consumer;

import com.example.account.model.MyAccountUpdatedPayload;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.lang.Nullable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.validation.annotation.Validated;

/**
 * Defines the Spring Kafka consumer contract for messages received from the {@code my.accounts.{environment}.updated.v1}
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
 *
 * <p>Example implementation:
 *
 * <pre>{@code
 * @Component
 * public final class MyAccountUpdatedConsumerImpl implements MyAccountUpdatedConsumer {
 *     @Override
 *     @KafkaListener(topics = MyAccountUpdatedConsumer.MY_ACCOUNT_UPDATED_TOPIC_ADDRESS)
 *     public void listen(
 *             MyAccountUpdatedPayload payload,
 *             String receivedTopic,
 *             UUID receivedKey,
 *             String X_EXAMPLE_CORRELATION_ID,
 *             String X_EXAMPLE_SOURCE_SYSTEM
 *     ) {
 *         // Process the message.
 *     }
 * }
 * }</pre>
 */
@Validated
public interface MyAccountUpdatedConsumer {
    /**
     * Kafka topic address declared by this AsyncAPI channel, with channel parameters
     * mapped to Spring property placeholders.
     *
     * <p>Use this constant in {@code @KafkaListener(topics = ...)}.
     *
     * <p>To inject the resolved topic address:
     *
     * <pre>{@code
     * public final class MyAccountUpdatedConsumerImpl {
     *     private final String topicAddress;
     *
     *     public MyAccountUpdatedConsumerImpl(
     *             @Value(MyAccountUpdatedConsumer.MY_ACCOUNT_UPDATED_TOPIC_ADDRESS)
     *             String topicAddress) {
     *         this.topicAddress = topicAddress;
     *     }
     * }
     * }</pre>
     */
    String MY_ACCOUNT_UPDATED_TOPIC_ADDRESS = "my.accounts.${kafka.environment}.updated.v1";

    /**
     * Handles the {@code MyAccountUpdated} message received from this channel.
     *
     * @param payload Details about an account update.
     * @param receivedTopic Kafka topic from which the record was received.
     * @param receivedKey Unique account identifier used as the Kafka record key.
     * @param X_EXAMPLE_CORRELATION_ID Value bound from the {@code X-EXAMPLE-CORRELATION-ID} Kafka message header. Identifier used to correlate related
     *   messages.
     * @param X_EXAMPLE_SOURCE_SYSTEM Value bound from the {@code X-EXAMPLE-SOURCE-SYSTEM} Kafka message header. Optional name of the system that produced the
     *   message.
     */
    void listen(
        @Payload @Valid MyAccountUpdatedPayload payload,
        @Header(name = KafkaHeaders.RECEIVED_TOPIC, required = true) @NotNull String receivedTopic,
        @Header(name = KafkaHeaders.RECEIVED_KEY, required = true) @NotNull UUID receivedKey,
        @Header(name = "X-EXAMPLE-CORRELATION-ID", required = true) @NotNull String X_EXAMPLE_CORRELATION_ID,
        @Header(name = "X-EXAMPLE-SOURCE-SYSTEM", required = false) @Nullable String X_EXAMPLE_SOURCE_SYSTEM
    );
}
