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
 * Defines the Spring Kafka producer contract for messages published to the `my.accounts.{environment}.lifecycle.v1`
 * AsyncAPI channel.
 *
 * This interface does not publish Kafka records or register a Spring bean. The
 * application publishes messages by implementing this contract and delegating
 * to an application-configured Spring Kafka producer.
 *
 * The generated `@Payload` and `@Header` annotations describe each parameter's
 * role in the message contract. They do not create the outbound message.
 * Implementations own topic resolution, record or message creation, Kafka key
 * and header mapping, serialization, message conversion, and producer runtime
 * configuration.
 *
 * Generated methods return an exceptionally completed future until the
 * application overrides them. Inheriting this interface alone cannot publish
 * records.
 *
 * This channel declares multiple message types. Each generated method is an
 * independent publication contract. Override every method the application
 * intends to publish.
 */
@Validated
interface MyAccountLifecycleProducer {
    companion object {
        /**
         * Kafka topic address declared by this AsyncAPI channel, with channel parameters
         * mapped to Spring property placeholders.
         *
         * Inject the resolved topic address into a producer implementation:
         *
         * ```kotlin
         * class MyAccountLifecycleProducerImpl(
         *     @param:Value(MyAccountLifecycleProducer.MY_ACCOUNT_LIFECYCLE_TOPIC_ADDRESS)
         *     private val topicAddress: String,
         * )
         * ```
         */
        const val MY_ACCOUNT_LIFECYCLE_TOPIC_ADDRESS: String = "my.accounts.\${kafka.environment}.lifecycle.v1"
    }

    /**
     * Publishes the `MyAccountCreated` message to this channel.
     *
     * The generated default implementation does not publish a record. It
     * returns an exceptionally completed future until the application
     * overrides this method.
     *
     * @param [payload] Details about a newly created account.
     * @param [X_EXAMPLE_CORRELATION_ID] Identifier used to correlate related messages.
     * @param [X_EXAMPLE_SOURCE_SYSTEM] Optional name of the system that produced the message.
     * @return Future completed with [RecordMetadata] after a successful producer send.
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
     * Publishes the `MyAccountUpdated` message to this channel.
     *
     * The generated default implementation does not publish a record. It
     * returns an exceptionally completed future until the application
     * overrides this method.
     *
     * @param [payload] Details about an account update.
     * @param [messageKey] Numeric identifier of the updated account.
     * @param [X_EXAMPLE_CORRELATION_ID] Identifier used to correlate related messages.
     * @param [X_EXAMPLE_SOURCE_SYSTEM] Optional name of the system that produced the message.
     * @return Future completed with [RecordMetadata] after a successful producer send.
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
     * Publishes the `MyAccountClosed` message to this channel.
     *
     * The generated default implementation does not publish a record. It
     * returns an exceptionally completed future until the application
     * overrides this method.
     *
     * @param [payload] Details about a closed account.
     * @param [messageKey] Identifies a particular account closure.
     * @param [X_EXAMPLE_CORRELATION_ID] Identifier used to correlate related messages.
     * @param [X_EXAMPLE_SOURCE_SYSTEM] Optional name of the system that produced the message.
     * @return Future completed with [RecordMetadata] after a successful producer send.
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
