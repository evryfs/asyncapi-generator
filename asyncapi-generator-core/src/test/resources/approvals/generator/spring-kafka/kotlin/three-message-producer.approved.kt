package com.example.account.client.producer

import com.example.account.model.MyAccountClosedPayload
import com.example.account.model.MyAccountClosureKey
import com.example.account.model.MyAccountCreatedPayload
import com.example.account.model.MyAccountUpdatedPayload
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import java.util.concurrent.CompletableFuture
import org.apache.kafka.clients.producer.RecordMetadata
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.validation.annotation.Validated

/**
 * Producer contract for publishing messages to the `my.accounts.{environment}.lifecycle.v1` topic.
 * The contract exposes message payloads and contract-defined headers as method parameters.
 * Messages with a `bindings.kafka.key` schema also expose a typed Kafka record key.
 */
@Validated
interface MyAccountLifecycleProducer {
    companion object {
        /** Spring-resolvable Kafka topic address declared by this AsyncAPI channel. */
        const val MY_ACCOUNT_LIFECYCLE_TOPIC_ADDRESS: String = "my.accounts.\${kafka.environment}.lifecycle.v1"
    }

    /**
     * The generated default implementation does not publish a record. It
     * returns an exceptionally completed future until the application
     * overrides this method.
     *
     * @param payload Details about a newly created account.
     * @param X_EXAMPLE_CORRELATION_ID Value for the `X-EXAMPLE-CORRELATION-ID` Kafka message header. Implementations must add this value to the outgoing Kafka
     * record. Identifier used to correlate related messages.
     * @param X_EXAMPLE_SOURCE_SYSTEM Value for the `X-EXAMPLE-SOURCE-SYSTEM` Kafka message header. Implementations must add this value to the outgoing Kafka
     * record. Optional name of the system that produced the message.
     * @return Future completed with Kafka record metadata after successful publication.
     *   The generated default completes exceptionally until this method is overridden.
     */
    fun sendMyAccountCreated(
        @Payload
        @Valid
        payload: MyAccountCreatedPayload,

        @Header(name = "X-EXAMPLE-CORRELATION-ID", required = true)
        X_EXAMPLE_CORRELATION_ID: String,
        @Header(name = "X-EXAMPLE-SOURCE-SYSTEM", required = false)
        X_EXAMPLE_SOURCE_SYSTEM: String? = null,
    ): CompletableFuture<RecordMetadata> =
        CompletableFuture.failedFuture(
            UnsupportedOperationException(
                "Generated producer method 'sendMyAccountCreated' has no implementation. Override it before use.",
            ),
        )

    /**
     * The generated default implementation does not publish a record. It
     * returns an exceptionally completed future until the application
     * overrides this method.
     *
     * @param payload Details about an account update.
     * @param messageKey Numeric identifier of the updated account.
     * @param X_EXAMPLE_CORRELATION_ID Value for the `X-EXAMPLE-CORRELATION-ID` Kafka message header. Implementations must add this value to the outgoing Kafka
     * record. Identifier used to correlate related messages.
     * @param X_EXAMPLE_SOURCE_SYSTEM Value for the `X-EXAMPLE-SOURCE-SYSTEM` Kafka message header. Implementations must add this value to the outgoing Kafka
     * record. Optional name of the system that produced the message.
     * @return Future completed with Kafka record metadata after successful publication.
     *   The generated default completes exceptionally until this method is overridden.
     */
    fun sendMyAccountUpdated(
        @Payload
        @Valid
        payload: MyAccountUpdatedPayload,

        @Min(1L)
        @Max(9999999999L)
        messageKey: Long,

        @Header(name = "X-EXAMPLE-CORRELATION-ID", required = true)
        X_EXAMPLE_CORRELATION_ID: String,
        @Header(name = "X-EXAMPLE-SOURCE-SYSTEM", required = false)
        X_EXAMPLE_SOURCE_SYSTEM: String? = null,
    ): CompletableFuture<RecordMetadata> =
        CompletableFuture.failedFuture(
            UnsupportedOperationException(
                "Generated producer method 'sendMyAccountUpdated' has no implementation. Override it before use.",
            ),
        )

    /**
     * The generated default implementation does not publish a record. It
     * returns an exceptionally completed future until the application
     * overrides this method.
     *
     * @param payload Details about a closed account.
     * @param messageKey Identifies a particular account closure.
     * @param X_EXAMPLE_CORRELATION_ID Value for the `X-EXAMPLE-CORRELATION-ID` Kafka message header. Implementations must add this value to the outgoing Kafka
     * record. Identifier used to correlate related messages.
     * @param X_EXAMPLE_SOURCE_SYSTEM Value for the `X-EXAMPLE-SOURCE-SYSTEM` Kafka message header. Implementations must add this value to the outgoing Kafka
     * record. Optional name of the system that produced the message.
     * @return Future completed with Kafka record metadata after successful publication.
     *   The generated default completes exceptionally until this method is overridden.
     */
    fun sendMyAccountClosed(
        @Payload
        @Valid
        payload: MyAccountClosedPayload,

        @Valid
        messageKey: MyAccountClosureKey,

        @Header(name = "X-EXAMPLE-CORRELATION-ID", required = true)
        X_EXAMPLE_CORRELATION_ID: String,
        @Header(name = "X-EXAMPLE-SOURCE-SYSTEM", required = false)
        X_EXAMPLE_SOURCE_SYSTEM: String? = null,
    ): CompletableFuture<RecordMetadata> =
        CompletableFuture.failedFuture(
            UnsupportedOperationException(
                "Generated producer method 'sendMyAccountClosed' has no implementation. Override it before use.",
            ),
        )
}
