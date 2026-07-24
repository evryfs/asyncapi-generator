package com.example.account.client.producer;

import com.example.account.model.MyAccountClosedPayload;
import com.example.account.model.MyAccountClosureKey;
import com.example.account.model.MyAccountCreatedPayload;
import com.example.account.model.MyAccountUpdatedPayload;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.lang.Nullable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.validation.annotation.Validated;

/**
 * Defines the Spring Kafka producer contract for messages published to the {@code my.accounts.{environment}.lifecycle.v1}
 * AsyncAPI channel.
 *
 * <p>This interface does not publish Kafka records or register a Spring bean.
 * The application publishes messages by implementing this contract and
 * delegating to an application-configured Spring Kafka producer.
 *
 * <p>The generated {@code @Payload} and {@code @Header} annotations describe
 * each parameter's role in the message contract. They do not create the
 * outbound message. Implementations own topic resolution, record or message
 * creation, Kafka key and header mapping, serialization, message conversion,
 * and producer runtime configuration.
 *
 * <p>Generated methods return an exceptionally completed future until the
 * application overrides them. Inheriting this interface alone cannot publish
 * records.
 *
 * <p>This channel declares multiple message types. Each generated method is an
 * independent publication contract. Override every method the application
 * intends to publish.
 */
@Validated
public interface MyAccountLifecycleProducer {
    /**
     * Kafka topic address declared by this AsyncAPI channel, with channel parameters
     * mapped to Spring property placeholders.
     *
     * <p>Inject the resolved topic address into a producer implementation:
     *
     * <pre>{@code
     * public final class MyAccountLifecycleProducerImpl {
     *     private final String topicAddress;
     *
     *     public MyAccountLifecycleProducerImpl(
     *             @Value(MyAccountLifecycleProducer.MY_ACCOUNT_LIFECYCLE_TOPIC_ADDRESS)
     *             String topicAddress) {
     *         this.topicAddress = topicAddress;
     *     }
     * }
     * }</pre>
     */
    String MY_ACCOUNT_LIFECYCLE_TOPIC_ADDRESS = "my.accounts.${kafka.environment}.lifecycle.v1";

    /**
     * Publishes the {@code MyAccountCreated} message to this channel.
     *
     * <p>The generated default implementation does not publish a record. It
     * returns an exceptionally completed future until the application
     * overrides this method.
     *
     * @param payload Details about a newly created account.
     * @param X_EXAMPLE_CORRELATION_ID Value for the {@code X-EXAMPLE-CORRELATION-ID} Kafka message header. Implementations must add this value to the outgoing
     * Kafka record. Identifier used to correlate related messages.
     * @param X_EXAMPLE_SOURCE_SYSTEM Value for the {@code X-EXAMPLE-SOURCE-SYSTEM} Kafka message header. Implementations must add this value to the outgoing
     * Kafka record. Optional name of the system that produced the message.
     * @return future completed with {@link RecordMetadata} after a successful producer send;
     *   the generated default completes exceptionally until this method is overridden
     */
    default CompletableFuture<RecordMetadata> sendMyAccountCreated(
        @Payload @Valid MyAccountCreatedPayload payload,
        @Header(name = "X-EXAMPLE-CORRELATION-ID", required = true) @NotNull String X_EXAMPLE_CORRELATION_ID,
        @Header(name = "X-EXAMPLE-SOURCE-SYSTEM", required = false) @Nullable String X_EXAMPLE_SOURCE_SYSTEM
    ) {
        return CompletableFuture.failedFuture(
            new UnsupportedOperationException(
                "Generated producer method 'sendMyAccountCreated' has no implementation. Override it before use."
            )
        );
    }

    /**
     * Publishes the {@code MyAccountUpdated} message to this channel.
     *
     * <p>The generated default implementation does not publish a record. It
     * returns an exceptionally completed future until the application
     * overrides this method.
     *
     * @param payload Details about an account update.
     * @param messageKey Numeric identifier of the updated account.
     * @param X_EXAMPLE_CORRELATION_ID Value for the {@code X-EXAMPLE-CORRELATION-ID} Kafka message header. Implementations must add this value to the outgoing
     * Kafka record. Identifier used to correlate related messages.
     * @param X_EXAMPLE_SOURCE_SYSTEM Value for the {@code X-EXAMPLE-SOURCE-SYSTEM} Kafka message header. Implementations must add this value to the outgoing
     * Kafka record. Optional name of the system that produced the message.
     * @return future completed with {@link RecordMetadata} after a successful producer send;
     *   the generated default completes exceptionally until this method is overridden
     */
    default CompletableFuture<RecordMetadata> sendMyAccountUpdated(
        @Payload @Valid MyAccountUpdatedPayload payload,
        @Min(1L) @Max(9999999999L) @NotNull Long messageKey,
        @Header(name = "X-EXAMPLE-CORRELATION-ID", required = true) @NotNull String X_EXAMPLE_CORRELATION_ID,
        @Header(name = "X-EXAMPLE-SOURCE-SYSTEM", required = false) @Nullable String X_EXAMPLE_SOURCE_SYSTEM
    ) {
        return CompletableFuture.failedFuture(
            new UnsupportedOperationException(
                "Generated producer method 'sendMyAccountUpdated' has no implementation. Override it before use."
            )
        );
    }

    /**
     * Publishes the {@code MyAccountClosed} message to this channel.
     *
     * <p>The generated default implementation does not publish a record. It
     * returns an exceptionally completed future until the application
     * overrides this method.
     *
     * @param payload Details about a closed account.
     * @param messageKey Identifies a particular account closure.
     * @param X_EXAMPLE_CORRELATION_ID Value for the {@code X-EXAMPLE-CORRELATION-ID} Kafka message header. Implementations must add this value to the outgoing
     * Kafka record. Identifier used to correlate related messages.
     * @param X_EXAMPLE_SOURCE_SYSTEM Value for the {@code X-EXAMPLE-SOURCE-SYSTEM} Kafka message header. Implementations must add this value to the outgoing
     * Kafka record. Optional name of the system that produced the message.
     * @return future completed with {@link RecordMetadata} after a successful producer send;
     *   the generated default completes exceptionally until this method is overridden
     */
    default CompletableFuture<RecordMetadata> sendMyAccountClosed(
        @Payload @Valid MyAccountClosedPayload payload,
        @Valid @NotNull MyAccountClosureKey messageKey,
        @Header(name = "X-EXAMPLE-CORRELATION-ID", required = true) @NotNull String X_EXAMPLE_CORRELATION_ID,
        @Header(name = "X-EXAMPLE-SOURCE-SYSTEM", required = false) @Nullable String X_EXAMPLE_SOURCE_SYSTEM
    ) {
        return CompletableFuture.failedFuture(
            new UnsupportedOperationException(
                "Generated producer method 'sendMyAccountClosed' has no implementation. Override it before use."
            )
        );
    }
}
