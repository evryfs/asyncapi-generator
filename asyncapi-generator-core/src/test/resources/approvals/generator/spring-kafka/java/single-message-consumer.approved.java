package com.example.account.client.consumer;

import com.example.account.model.MyAccountKey;
import com.example.account.model.MyAccountUpdatedPayload;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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
 * <p>The generated method is an unannotated no-op default. Override it and add
 * {@code @KafkaListener} to the implementation method to activate consumption.
 *
 * <p>Example implementation:
 *
 * <pre>{@code
 * @Component
 * public final class MyAccountUpdatedConsumerImpl implements MyAccountUpdatedConsumer {
 *     @Override
 *     @KafkaListener(topics = MyAccountUpdatedConsumer.MY_ACCOUNT_UPDATED_TOPIC_ADDRESS)
 *     public void listenMyAccountUpdated(
 *             MyAccountUpdatedPayload payload,
 *             String receivedTopic,
 *             MyAccountKey receivedKey,
 *             String xExampleCorrelationId,
 *             String xExampleSourceSystem
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
     * <p>The generated default implementation performs no action. Override this
     * method and add {@code @KafkaListener} to activate message consumption.
     *
     * @param payload Details about an account update.
     * @param receivedTopic Kafka topic from which the record was received.
     * @param receivedKey Identifies an account within an institution.
     * @param xExampleCorrelationId Identifier used to correlate related messages.
     * @param xExampleSourceSystem Optional name of the system that produced the message.
     */
    default void listenMyAccountUpdated(
        @Payload @Valid MyAccountUpdatedPayload payload,
        @Header(name = KafkaHeaders.RECEIVED_TOPIC, required = true) @NotNull String receivedTopic,
        @Header(name = KafkaHeaders.RECEIVED_KEY, required = true) @Valid @NotNull MyAccountKey receivedKey,
        @Header(name = "X-EXAMPLE-CORRELATION-ID", required = true) @NotNull String xExampleCorrelationId,
        @Header(name = "X-EXAMPLE-SOURCE-SYSTEM", required = false) @Nullable String xExampleSourceSystem
    ) {
    }
}
