package com.example.account.client.consumer

import com.example.account.model.MyAccountUpdatedPayload
import jakarta.validation.Valid
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.validation.annotation.Validated

/**
 * Defines the Spring Kafka consumer contract for messages received from the `my.accounts.{environment}.updated.v1`
 * AsyncAPI channel.
 *
 * This interface does not register Kafka listeners. The application activates
 * consumption by implementing this contract in a Spring bean and configuring
 * `@KafkaListener` on the implementation.
 *
 * The generated parameter annotations define how Spring binds the message
 * payload, Kafka record metadata, and any AsyncAPI-defined message headers.
 *
 * Kafka metadata parameters use [KafkaHeaders], provided by Spring Kafka.
 * See [KafkaHeaders] for the metadata constants available in the application's
 * Spring Kafka version.
 *
 * Example implementation:
 *
 * ```kotlin
 * @Component
 * class MyAccountUpdatedConsumerImpl : MyAccountUpdatedConsumer {
 *     @KafkaListener(
 *         topics = [MyAccountUpdatedConsumer.MY_ACCOUNT_UPDATED_TOPIC_ADDRESS],
 *     )
 *     override fun listen(
 *         payload: MyAccountUpdatedPayload,
 *         receivedTopic: String,
 *         receivedKey: String?,
 *         correlationId: String,
 *         sourceSystem: String?,
 *     ) {
 *         // Process the message.
 *     }
 * }
 * ```
 */
@Validated
interface MyAccountUpdatedConsumer {
    companion object {
        /**
         * Kafka topic address declared by this AsyncAPI channel, with channel parameters
         * mapped to Spring property placeholders.
         *
         * Use this constant in `@KafkaListener(topics = [...])`.
         *
         * To inject the resolved topic address:
         *
         * ```kotlin
         * class MyAccountUpdatedConsumerImpl(
         *     @param:Value(MyAccountUpdatedConsumer.MY_ACCOUNT_UPDATED_TOPIC_ADDRESS)
         *     private val topicAddress: String,
         * )
         * ```
         */
        const val MY_ACCOUNT_UPDATED_TOPIC_ADDRESS: String = "my.accounts.\${kafka.environment}.updated.v1"
    }

    /**
     * Handles the `MyAccountUpdated` message received from this channel.
     *
     * @param [payload] Details about an account update.
     * @param [receivedTopic] Kafka topic from which the record was received.
     * @param [receivedKey] Kafka record key, or `null` when the record has no key.
     * @param [correlationId] Identifier used to correlate related messages.
     * @param [sourceSystem] Optional name of the system that produced the message.
     */
    fun listen(
        @Payload
        @Valid
        payload: MyAccountUpdatedPayload,

        @Header(name = KafkaHeaders.RECEIVED_TOPIC, required = true)
        receivedTopic: String,

        @Header(name = KafkaHeaders.RECEIVED_KEY, required = false)
        receivedKey: String?,

        @Header(name = "correlationId", required = true)
        correlationId: String,
        @Header(name = "sourceSystem", required = false)
        sourceSystem: String? = null,
    )
}
