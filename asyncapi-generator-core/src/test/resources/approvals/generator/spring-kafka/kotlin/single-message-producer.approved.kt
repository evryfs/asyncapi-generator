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
 * Example implementation:
 *
 * ```kotlin
 * @Component
 * class MyAccountUpdatedProducerImpl(
 *     @param:Value(MyAccountUpdatedProducer.MY_ACCOUNT_UPDATED_TOPIC_ADDRESS)
 *     private val topicAddress: String,
 *     private val kafkaTemplate: KafkaTemplate<*, *>,
 * ) : MyAccountUpdatedProducer {
 *     override fun sendMyAccountUpdated(
 *         payload: MyAccountUpdatedPayload,
 *         messageKey: MyAccountKey,
 *         X_EXAMPLE_CORRELATION_ID: String,
 *         X_EXAMPLE_SOURCE_SYSTEM: String?,
 *     ): CompletableFuture<RecordMetadata> {
 *         val message =
 *             MessageBuilder.withPayload(payload)
 *                 .setHeader(KafkaHeaders.TOPIC, topicAddress)
 *                 .setHeader(KafkaHeaders.KEY, messageKey)
 *                 .setHeader("X-EXAMPLE-CORRELATION-ID", X_EXAMPLE_CORRELATION_ID)
 *                 .setHeader("X-EXAMPLE-SOURCE-SYSTEM", X_EXAMPLE_SOURCE_SYSTEM)
 *                 .build()
 *
 *         return kafkaTemplate.send(message)
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
     * @param [X_EXAMPLE_CORRELATION_ID] Value for the `X-EXAMPLE-CORRELATION-ID` Kafka message header. Implementations must add this value to the outgoing Kafka
     *   record. Identifier used to correlate related messages.
     * @param [X_EXAMPLE_SOURCE_SYSTEM] Value for the `X-EXAMPLE-SOURCE-SYSTEM` Kafka message header. Implementations must add this value to the outgoing Kafka
     *   record. Optional name of the system that produced the message.
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
}
