package com.example.account.client.producer;

import com.example.account.model.MyAccountKey;
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
 * Defines the Spring Kafka producer contract for messages published to the {@code my.accounts.{environment}.updated.v1}
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
 * <p>Additional payload methods accept final serialized record values. Send
 * them with application-configured compatible serializers; do not pass them
 * through a JSON message converter that can serialize the payload again.
 */
@Validated
public interface MyAccountUpdatedProducer {
    /**
     * Kafka topic address declared by this AsyncAPI channel, with channel parameters
     * mapped to Spring property placeholders.
     *
     * <p>Inject the resolved topic address into a producer implementation:
     *
     * <pre>{@code
     * public final class MyAccountUpdatedProducerImpl {
     *     private final String topicAddress;
     *
     *     public MyAccountUpdatedProducerImpl(
     *             @Value(MyAccountUpdatedProducer.MY_ACCOUNT_UPDATED_TOPIC_ADDRESS)
     *             String topicAddress) {
     *         this.topicAddress = topicAddress;
     *     }
     * }
     * }</pre>
     */
    String MY_ACCOUNT_UPDATED_TOPIC_ADDRESS = "my.accounts.${kafka.environment}.updated.v1";

    /**
     * Publishes the {@code MyAccountUpdated} message to this channel.
     *
     * <p>The generated default implementation does not publish a record. It
     * returns an exceptionally completed future until the application
     * overrides this method.
     *
     * @param payload Details about an account update.
     * @param messageKey Identifies an account within an institution.
     * @param xExampleCorrelationId Identifier used to correlate related messages.
     * @param xExampleSourceSystem Optional name of the system that produced the message.
     * @return future completed with {@link RecordMetadata} after a successful producer send;
     *   the generated default completes exceptionally until this method is overridden
     */
    default CompletableFuture<RecordMetadata> sendMyAccountUpdated(
        @Payload @Valid MyAccountUpdatedPayload payload,
        @Valid @NotNull MyAccountKey messageKey,
        @Header(name = "X-EXAMPLE-CORRELATION-ID", required = true) @NotNull String xExampleCorrelationId,
        @Header(name = "X-EXAMPLE-SOURCE-SYSTEM", required = false) @Nullable String xExampleSourceSystem
    ) {
        return CompletableFuture.failedFuture(
            new UnsupportedOperationException(
                "Generated producer method 'sendMyAccountUpdated' has no implementation. Override it before use."
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
     * @param payload Final serialized Kafka record value as a byte array.
     * The bytes must conform to the payload schema and content type declared by the AsyncAPI contract.
     * The generated contract does not serialize or validate the byte content.
     * A compatible serializer such as ByteArraySerializer is application-owned.
     * @param messageKey Identifies an account within an institution.
     * @param xExampleCorrelationId Identifier used to correlate related messages.
     * @param xExampleSourceSystem Optional name of the system that produced the message.
     * @return future completed with {@link RecordMetadata} after a successful producer send;
     *   the generated default completes exceptionally until this method is overridden
     */
    default CompletableFuture<RecordMetadata> sendMyAccountUpdatedByteArray(
        @Payload byte[] payload,
        @Valid @NotNull MyAccountKey messageKey,
        @Header(name = "X-EXAMPLE-CORRELATION-ID", required = true) @NotNull String xExampleCorrelationId,
        @Header(name = "X-EXAMPLE-SOURCE-SYSTEM", required = false) @Nullable String xExampleSourceSystem
    ) {
        return CompletableFuture.failedFuture(
            new UnsupportedOperationException(
                "Generated producer method 'sendMyAccountUpdatedByteArray' has no implementation. Override it before use."
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
     * @param payload Final serialized textual Kafka record value.
     * The text must conform to the payload schema and content type declared by the AsyncAPI contract.
     * The generated contract does not serialize or validate the text.
     * Encoding and serializer configuration are application-owned; StringSerializer uses UTF-8 by default.
     * @param messageKey Identifies an account within an institution.
     * @param xExampleCorrelationId Identifier used to correlate related messages.
     * @param xExampleSourceSystem Optional name of the system that produced the message.
     * @return future completed with {@link RecordMetadata} after a successful producer send;
     *   the generated default completes exceptionally until this method is overridden
     */
    default CompletableFuture<RecordMetadata> sendMyAccountUpdatedString(
        @Payload String payload,
        @Valid @NotNull MyAccountKey messageKey,
        @Header(name = "X-EXAMPLE-CORRELATION-ID", required = true) @NotNull String xExampleCorrelationId,
        @Header(name = "X-EXAMPLE-SOURCE-SYSTEM", required = false) @Nullable String xExampleSourceSystem
    ) {
        return CompletableFuture.failedFuture(
            new UnsupportedOperationException(
                "Generated producer method 'sendMyAccountUpdatedString' has no implementation. Override it before use."
            )
        );
    }
}
