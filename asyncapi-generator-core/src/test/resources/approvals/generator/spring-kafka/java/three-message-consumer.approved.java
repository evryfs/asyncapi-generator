package com.example.account.client.consumer;

import com.example.account.model.MyAccountClosedPayload;
import com.example.account.model.MyAccountClosureKey;
import com.example.account.model.MyAccountCreatedPayload;
import com.example.account.model.MyAccountUpdatedPayload;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
 *
 * <p>This channel declares multiple message types. To activate class-level
 * dispatch, add {@code @KafkaListener} to the implementation class, override
 * each selected method, and add {@code @KafkaHandler} to each override. Spring
 * dispatches records by the converted payload's runtime type, so selected
 * handlers must use distinct payload types.
 *
 * <p>Generated methods are unannotated no-op defaults. Inheriting a method does
 * not register it as a Kafka handler. Records without a selected handler are
 * not routed to these defaults.
 *
 * <p>Applications that intentionally ignore a declared message should configure
 * a {@code RecordFilterStrategy} before handler dispatch or provide an explicit
 * no-op {@code @KafkaHandler} override. Filters should distinguish expected
 * ignored messages from unknown messages so new or malformed message types are
 * not silently discarded.
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
     * <p>The generated default implementation performs no action and is not a
     * Kafka handler. Override this method and add {@code @KafkaHandler} to
     * select it for class-level listener dispatch.
     *
     * @param payload Details about a newly created account.
     * @param receivedTopic Kafka topic from which the record was received.
     * @param xExampleCorrelationId Identifier used to correlate related messages.
     * @param xExampleSourceSystem Optional name of the system that produced the message.
     */
    default void listenMyAccountCreated(
        @Payload @Valid MyAccountCreatedPayload payload,
        @Header(name = KafkaHeaders.RECEIVED_TOPIC, required = true) @NotNull String receivedTopic,
        @Header(name = "X-EXAMPLE-CORRELATION-ID", required = true) @NotNull String xExampleCorrelationId,
        @Header(name = "X-EXAMPLE-SOURCE-SYSTEM", required = false) @Nullable String xExampleSourceSystem
    ) {
    }

    /**
     * Handles the {@code MyAccountUpdated} message received from this channel.
     *
     * <p>The generated default implementation performs no action and is not a
     * Kafka handler. Override this method and add {@code @KafkaHandler} to
     * select it for class-level listener dispatch.
     *
     * @param payload Details about an account update.
     * @param receivedTopic Kafka topic from which the record was received.
     * @param receivedKey Numeric identifier of the updated account.
     * @param xExampleCorrelationId Identifier used to correlate related messages.
     * @param xExampleSourceSystem Optional name of the system that produced the message.
     */
    default void listenMyAccountUpdated(
        @Payload @Valid MyAccountUpdatedPayload payload,
        @Header(name = KafkaHeaders.RECEIVED_TOPIC, required = true) @NotNull String receivedTopic,
        @Header(name = KafkaHeaders.RECEIVED_KEY, required = true) @Min(1L) @Max(9999999999L) @NotNull Long receivedKey,
        @Header(name = "X-EXAMPLE-CORRELATION-ID", required = true) @NotNull String xExampleCorrelationId,
        @Header(name = "X-EXAMPLE-SOURCE-SYSTEM", required = false) @Nullable String xExampleSourceSystem
    ) {
    }

    /**
     * Handles the {@code MyAccountClosed} message received from this channel.
     *
     * <p>The generated default implementation performs no action and is not a
     * Kafka handler. Override this method and add {@code @KafkaHandler} to
     * select it for class-level listener dispatch.
     *
     * @param payload Details about a closed account.
     * @param receivedTopic Kafka topic from which the record was received.
     * @param receivedKey Identifies a particular account closure.
     * @param xExampleCorrelationId Identifier used to correlate related messages.
     * @param xExampleSourceSystem Optional name of the system that produced the message.
     */
    default void listenMyAccountClosed(
        @Payload @Valid MyAccountClosedPayload payload,
        @Header(name = KafkaHeaders.RECEIVED_TOPIC, required = true) @NotNull String receivedTopic,
        @Header(name = KafkaHeaders.RECEIVED_KEY, required = true) @Valid @NotNull MyAccountClosureKey receivedKey,
        @Header(name = "X-EXAMPLE-CORRELATION-ID", required = true) @NotNull String xExampleCorrelationId,
        @Header(name = "X-EXAMPLE-SOURCE-SYSTEM", required = false) @Nullable String xExampleSourceSystem
    ) {
    }
}
