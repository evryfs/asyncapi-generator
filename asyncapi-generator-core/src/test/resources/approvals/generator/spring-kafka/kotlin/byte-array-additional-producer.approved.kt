package com.example.account.client.producer

import com.example.account.model.MyAccountKey
import com.example.account.model.MyAccountUpdatedPayload
import jakarta.validation.Valid
import java.util.concurrent.CompletableFuture
import org.apache.kafka.clients.producer.RecordMetadata
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.validation.annotation.Validated

/**
 * Defines the Spring Kafka producer contract for messages published to the `my.accounts.{environment}.updated.v1` AsyncAPI
 * channel.
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
 * Additional payload methods accept final serialized record values. Send them
 * with application-configured compatible serializers; do not pass them through
 * a JSON message converter that can serialize the payload again.
 *
 * Example implementation for forwarding an already serialized record value:
 *
 * Contract-defined headers still require application-owned encoding before
 * they are added to the `ProducerRecord` headers.
 *
 * ```kotlin
 * @Component
 * class MyAccountUpdatedProducerImpl(
 *     @param:Value(MyAccountUpdatedProducer.MY_ACCOUNT_UPDATED_TOPIC_ADDRESS)
 *     private val topicAddress: String,
 *     private val kafkaTemplate: KafkaTemplate<MyAccountKey, ByteArray>,
 * ) : MyAccountUpdatedProducer {
 *     override fun sendMyAccountUpdatedByteArray(
 *         payload: ByteArray,
 *         messageKey: MyAccountKey,
 *         X_EXAMPLE_CORRELATION_ID: String,
 *         X_EXAMPLE_SOURCE_SYSTEM: String?,
 *     ): CompletableFuture<RecordMetadata> {
 *         val record = ProducerRecord<MyAccountKey, ByteArray>(
 *             topicAddress,
 *             messageKey,
 *             payload,
 *         )
 *         return kafkaTemplate.send(record)
 *             .thenApply { result -> result.recordMetadata }
 *     }
 * }
 * ```
 */
@Validated
interface MyAccountUpdatedProducer {
    companion object {
        /**
         * Kafka topic address declared by this AsyncAPI channel, with channel parameters
         * mapped to Spring property placeholders.
         *
         * Inject the resolved topic address into a producer implementation:
         *
         * ```kotlin
         * class MyAccountUpdatedProducerImpl(
         *     @param:Value(MyAccountUpdatedProducer.MY_ACCOUNT_UPDATED_TOPIC_ADDRESS)
         *     private val topicAddress: String,
         * )
         * ```
         */
        const val MY_ACCOUNT_UPDATED_TOPIC_ADDRESS: String = "my.accounts.\${kafka.environment}.updated.v1"
    }

    /**
     * Publishes the `MyAccountUpdated` message to this channel.
     *
     * The generated default implementation does not publish a record. It
     * returns an exceptionally completed future until the application
     * overrides this method.
     *
     * @param [payload] Details about an account update.
     * @param [messageKey] Identifies an account within an institution.
     * @param [X_EXAMPLE_CORRELATION_ID] Identifier used to correlate related messages.
     * @param [X_EXAMPLE_SOURCE_SYSTEM] Optional name of the system that produced the message.
     * @return Future completed with [RecordMetadata] after a successful producer send.
     *   The generated default completes exceptionally until this method is overridden.
     */
    fun sendMyAccountUpdated(
        @Payload
        @Valid
        payload: MyAccountUpdatedPayload,

        @Valid
        messageKey: MyAccountKey,

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
     * Publishes the `MyAccountUpdated` message to this channel.
     *
     * The generated default implementation does not publish a record. It
     * returns an exceptionally completed future until the application
     * overrides this method.
     *
     * @param [payload] Final serialized Kafka record value as a byte array.
     *   The bytes must conform to the payload schema and content type declared by the AsyncAPI contract.
     *   The generated contract does not serialize or validate the byte content.
     *   A compatible serializer such as ByteArraySerializer is application-owned.
     * @param [messageKey] Identifies an account within an institution.
     * @param [X_EXAMPLE_CORRELATION_ID] Identifier used to correlate related messages.
     * @param [X_EXAMPLE_SOURCE_SYSTEM] Optional name of the system that produced the message.
     * @return Future completed with [RecordMetadata] after a successful producer send.
     *   The generated default completes exceptionally until this method is overridden.
     */
    fun sendMyAccountUpdatedByteArray(
        @Payload
        payload: ByteArray,

        @Valid
        messageKey: MyAccountKey,

        @Header(name = "X-EXAMPLE-CORRELATION-ID", required = true)
        X_EXAMPLE_CORRELATION_ID: String,
        @Header(name = "X-EXAMPLE-SOURCE-SYSTEM", required = false)
        X_EXAMPLE_SOURCE_SYSTEM: String? = null,
    ): CompletableFuture<RecordMetadata> =
        CompletableFuture.failedFuture(
            UnsupportedOperationException(
                "Generated producer method 'sendMyAccountUpdatedByteArray' has no implementation. Override it before use.",
            ),
        )
}
