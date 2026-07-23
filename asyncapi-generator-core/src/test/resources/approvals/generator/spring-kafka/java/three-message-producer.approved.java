package com.example.account.client.producer;

import com.example.account.model.MyAccountClosedPayload;
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
 * Producer contract for publishing messages to the {@code my.accounts.{environment}.lifecycle.v1} topic.
 * The contract exposes message payloads and contract-defined headers as method parameters.
 * Messages with a bindings.kafka.key schema also expose a typed Kafka record key.
 */
@Validated
public interface MyAccountLifecycleProducer {
    /** Spring-resolvable Kafka topic address declared by this AsyncAPI channel. */
    String MY_ACCOUNT_LIFECYCLE_TOPIC_ADDRESS = "my.accounts.${kafka.environment}.lifecycle.v1";

    /**
     * @param payload Details about a newly created account.
     * @param X_EXAMPLE_CORRELATION_ID Value for the {@code X-EXAMPLE-CORRELATION-ID} Kafka message header. Implementations must add this value to the outgoing
     * Kafka record. Identifier used to correlate related messages.
     * @param X_EXAMPLE_SOURCE_SYSTEM Value for the {@code X-EXAMPLE-SOURCE-SYSTEM} Kafka message header. Implementations must add this value to the outgoing
     * Kafka record. Optional name of the system that produced the message.
     * @return future completed with Kafka record metadata after successful publication
     */
    CompletableFuture<RecordMetadata> sendMyAccountCreated(
        @Payload @Valid MyAccountCreatedPayload payload,
        @Header(name = "X-EXAMPLE-CORRELATION-ID", required = true) @NotNull String X_EXAMPLE_CORRELATION_ID,
        @Header(name = "X-EXAMPLE-SOURCE-SYSTEM", required = false) @Nullable String X_EXAMPLE_SOURCE_SYSTEM
    );

    /**
     * @param payload Details about an account update.
     * @param messageKey Numeric identifier of the updated account.
     * @param X_EXAMPLE_CORRELATION_ID Value for the {@code X-EXAMPLE-CORRELATION-ID} Kafka message header. Implementations must add this value to the outgoing
     * Kafka record. Identifier used to correlate related messages.
     * @param X_EXAMPLE_SOURCE_SYSTEM Value for the {@code X-EXAMPLE-SOURCE-SYSTEM} Kafka message header. Implementations must add this value to the outgoing
     * Kafka record. Optional name of the system that produced the message.
     * @return future completed with Kafka record metadata after successful publication
     */
    CompletableFuture<RecordMetadata> sendMyAccountUpdated(
        @Payload @Valid MyAccountUpdatedPayload payload,
        @Min(1L) @Max(9999999999L) @NotNull Long messageKey,
        @Header(name = "X-EXAMPLE-CORRELATION-ID", required = true) @NotNull String X_EXAMPLE_CORRELATION_ID,
        @Header(name = "X-EXAMPLE-SOURCE-SYSTEM", required = false) @Nullable String X_EXAMPLE_SOURCE_SYSTEM
    );

    /**
     * @param payload Details about a closed account.
     * @param messageKey Indicates the account-key partition group.
     * @param X_EXAMPLE_CORRELATION_ID Value for the {@code X-EXAMPLE-CORRELATION-ID} Kafka message header. Implementations must add this value to the outgoing
     * Kafka record. Identifier used to correlate related messages.
     * @param X_EXAMPLE_SOURCE_SYSTEM Value for the {@code X-EXAMPLE-SOURCE-SYSTEM} Kafka message header. Implementations must add this value to the outgoing
     * Kafka record. Optional name of the system that produced the message.
     * @return future completed with Kafka record metadata after successful publication
     */
    CompletableFuture<RecordMetadata> sendMyAccountClosed(
        @Payload @Valid MyAccountClosedPayload payload,
        @Nullable Boolean messageKey,
        @Header(name = "X-EXAMPLE-CORRELATION-ID", required = true) @NotNull String X_EXAMPLE_CORRELATION_ID,
        @Header(name = "X-EXAMPLE-SOURCE-SYSTEM", required = false) @Nullable String X_EXAMPLE_SOURCE_SYSTEM
    );
}
