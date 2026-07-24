package com.example.account.client.consumer;

import com.example.account.model.MyAccountClosedPayload;
import com.example.account.model.MyAccountClosureKey;
import com.example.account.model.MyAccountCreatedPayload;
import com.example.account.model.MyAccountUpdatedPayload;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.kafka.annotation.KafkaHandler;
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
 *
 * <p>This channel declares multiple message types. Add {@code @KafkaListener} to the
 * implementation class and use {@code @KafkaHandler} methods to dispatch records by
 * the converted payload's runtime type. Each handler must therefore use a distinct
 * payload type.
 *
 * <p>Every generated method must be implemented. Applications that intentionally
 * process only a subset of the declared messages must still explicitly implement
 * the remaining methods. These may be intentional no-op handlers, or the
 * application may configure a {@code RecordFilterStrategy} to discard excluded
 * records before handler delivery.
 *
 * <p>No default no-op methods are generated, so ignoring a message type remains
 * an explicit application decision. When filtering is used, filtered records are
 * not delivered to a handler. Their acknowledgement and offset behavior is
 * controlled by the application's listener container and filter configuration.
 * Filters should distinguish expected ignored messages from unknown messages so
 * new or malformed message types are not silently discarded.
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
     * @param X_EXAMPLE_CORRELATION_ID Value bound from the {@code X-EXAMPLE-CORRELATION-ID} Kafka message header. Identifier used to correlate related
     *   messages.
     * @param X_EXAMPLE_SOURCE_SYSTEM Value bound from the {@code X-EXAMPLE-SOURCE-SYSTEM} Kafka message header. Optional name of the system that produced the
     *   message.
     */
    @KafkaHandler
    void listenMyAccountCreated(
        @Payload @Valid MyAccountCreatedPayload payload,
        @Header(name = KafkaHeaders.RECEIVED_TOPIC, required = true) @NotNull String receivedTopic,
        @Header(name = "X-EXAMPLE-CORRELATION-ID", required = true) @NotNull String X_EXAMPLE_CORRELATION_ID,
        @Header(name = "X-EXAMPLE-SOURCE-SYSTEM", required = false) @Nullable String X_EXAMPLE_SOURCE_SYSTEM
    );

    /**
     * Handles the {@code MyAccountUpdated} message received from this channel.
     *
     * @param payload Details about an account update.
     * @param receivedTopic Kafka topic from which the record was received.
     * @param receivedKey Numeric identifier of the updated account.
     * @param X_EXAMPLE_CORRELATION_ID Value bound from the {@code X-EXAMPLE-CORRELATION-ID} Kafka message header. Identifier used to correlate related
     *   messages.
     * @param X_EXAMPLE_SOURCE_SYSTEM Value bound from the {@code X-EXAMPLE-SOURCE-SYSTEM} Kafka message header. Optional name of the system that produced the
     *   message.
     */
    @KafkaHandler
    void listenMyAccountUpdated(
        @Payload @Valid MyAccountUpdatedPayload payload,
        @Header(name = KafkaHeaders.RECEIVED_TOPIC, required = true) @NotNull String receivedTopic,
        @Header(name = KafkaHeaders.RECEIVED_KEY, required = true) @Min(1L) @Max(9999999999L) @NotNull Long receivedKey,
        @Header(name = "X-EXAMPLE-CORRELATION-ID", required = true) @NotNull String X_EXAMPLE_CORRELATION_ID,
        @Header(name = "X-EXAMPLE-SOURCE-SYSTEM", required = false) @Nullable String X_EXAMPLE_SOURCE_SYSTEM
    );

    /**
     * Handles the {@code MyAccountClosed} message received from this channel.
     *
     * @param payload Details about a closed account.
     * @param receivedTopic Kafka topic from which the record was received.
     * @param receivedKey Identifies a particular account closure.
     * @param X_EXAMPLE_CORRELATION_ID Value bound from the {@code X-EXAMPLE-CORRELATION-ID} Kafka message header. Identifier used to correlate related
     *   messages.
     * @param X_EXAMPLE_SOURCE_SYSTEM Value bound from the {@code X-EXAMPLE-SOURCE-SYSTEM} Kafka message header. Optional name of the system that produced the
     *   message.
     */
    @KafkaHandler
    void listenMyAccountClosed(
        @Payload @Valid MyAccountClosedPayload payload,
        @Header(name = KafkaHeaders.RECEIVED_TOPIC, required = true) @NotNull String receivedTopic,
        @Header(name = KafkaHeaders.RECEIVED_KEY, required = true) @Valid @NotNull MyAccountClosureKey receivedKey,
        @Header(name = "X-EXAMPLE-CORRELATION-ID", required = true) @NotNull String X_EXAMPLE_CORRELATION_ID,
        @Header(name = "X-EXAMPLE-SOURCE-SYSTEM", required = false) @Nullable String X_EXAMPLE_SOURCE_SYSTEM
    );
}
