package com.example.account.client.consumer

import com.example.account.model.MyAccountClosedPayload
import com.example.account.model.MyAccountCreatedPayload
import com.example.account.model.MyAccountUpdatedPayload
import jakarta.validation.Valid
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.validation.annotation.Validated

/**
 * Defines the Spring Kafka consumer contract for messages received from the `my.accounts.{environment}.lifecycle.v1`
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
 */
@Validated
interface MyAccountLifecycleConsumer {
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
         * class MyAccountLifecycleConsumerImpl(
         *     @param:Value(MyAccountLifecycleConsumer.MY_ACCOUNT_LIFECYCLE_TOPIC_ADDRESS)
         *     private val topicAddress: String,
         * )
         * ```
         */
        const val MY_ACCOUNT_LIFECYCLE_TOPIC_ADDRESS: String = "my.accounts.\${kafka.environment}.lifecycle.v1"
    }

    /**
     * Handles the `MyAccountCreated` message received from this channel.
     *
     * @param [payload] Details about a newly created account.
     * @param [receivedTopic] Kafka topic from which the record was received.
     * @param [receivedKey] Kafka record key, or `null` when the record has no key.
     * @param [X_EXAMPLE_CORRELATION_ID] Value bound from the `X-EXAMPLE-CORRELATION-ID` Kafka message header. Identifier used to correlate related messages.
     * @param [X_EXAMPLE_SOURCE_SYSTEM] Value bound from the `X-EXAMPLE-SOURCE-SYSTEM` Kafka message header. Optional name of the system that produced the
     *   message.
     */
    fun listenMyAccountCreated(
        @Payload
        @Valid
        payload: MyAccountCreatedPayload,

        @Header(name = KafkaHeaders.RECEIVED_TOPIC, required = true)
        receivedTopic: String,

        @Header(name = KafkaHeaders.RECEIVED_KEY, required = false)
        receivedKey: String?,

        @Header(name = "X-EXAMPLE-CORRELATION-ID", required = true)
        X_EXAMPLE_CORRELATION_ID: String,
        @Header(name = "X-EXAMPLE-SOURCE-SYSTEM", required = false)
        X_EXAMPLE_SOURCE_SYSTEM: String? = null,
    )

    /**
     * Handles the `MyAccountUpdated` message received from this channel.
     *
     * @param [payload] Details about an account update.
     * @param [receivedTopic] Kafka topic from which the record was received.
     * @param [receivedKey] Kafka record key, or `null` when the record has no key.
     * @param [X_EXAMPLE_CORRELATION_ID] Value bound from the `X-EXAMPLE-CORRELATION-ID` Kafka message header. Identifier used to correlate related messages.
     * @param [X_EXAMPLE_SOURCE_SYSTEM] Value bound from the `X-EXAMPLE-SOURCE-SYSTEM` Kafka message header. Optional name of the system that produced the
     *   message.
     */
    fun listenMyAccountUpdated(
        @Payload
        @Valid
        payload: MyAccountUpdatedPayload,

        @Header(name = KafkaHeaders.RECEIVED_TOPIC, required = true)
        receivedTopic: String,

        @Header(name = KafkaHeaders.RECEIVED_KEY, required = false)
        receivedKey: String?,

        @Header(name = "X-EXAMPLE-CORRELATION-ID", required = true)
        X_EXAMPLE_CORRELATION_ID: String,
        @Header(name = "X-EXAMPLE-SOURCE-SYSTEM", required = false)
        X_EXAMPLE_SOURCE_SYSTEM: String? = null,
    )

    /**
     * Handles the `MyAccountClosed` message received from this channel.
     *
     * @param [payload] Details about a closed account.
     * @param [receivedTopic] Kafka topic from which the record was received.
     * @param [receivedKey] Kafka record key, or `null` when the record has no key.
     * @param [X_EXAMPLE_CORRELATION_ID] Value bound from the `X-EXAMPLE-CORRELATION-ID` Kafka message header. Identifier used to correlate related messages.
     * @param [X_EXAMPLE_SOURCE_SYSTEM] Value bound from the `X-EXAMPLE-SOURCE-SYSTEM` Kafka message header. Optional name of the system that produced the
     *   message.
     */
    fun listenMyAccountClosed(
        @Payload
        @Valid
        payload: MyAccountClosedPayload,

        @Header(name = KafkaHeaders.RECEIVED_TOPIC, required = true)
        receivedTopic: String,

        @Header(name = KafkaHeaders.RECEIVED_KEY, required = false)
        receivedKey: String?,

        @Header(name = "X-EXAMPLE-CORRELATION-ID", required = true)
        X_EXAMPLE_CORRELATION_ID: String,
        @Header(name = "X-EXAMPLE-SOURCE-SYSTEM", required = false)
        X_EXAMPLE_SOURCE_SYSTEM: String? = null,
    )
}
