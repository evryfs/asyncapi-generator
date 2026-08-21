package com.example.account.client.consumer

import com.example.account.model.MyAccountClosedPayload
import com.example.account.model.MyAccountClosureKey
import com.example.account.model.MyAccountCreatedPayload
import com.example.account.model.MyAccountUpdatedPayload
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
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
 *
 * This channel declares multiple message types. To activate class-level
 * dispatch, add `@KafkaListener` to the implementation class, override each
 * selected method, and add `@KafkaHandler` to each override. Spring dispatches
 * records by the converted payload's runtime type, so selected handlers must
 * use distinct payload types.
 *
 * Generated methods are unannotated no-op defaults. Inheriting a method does
 * not register it as a Kafka handler. Records without a selected handler are
 * not routed to these defaults.
 *
 * Applications that intentionally ignore a declared message should configure
 * a `RecordFilterStrategy` before handler dispatch or provide an explicit
 * no-op `@KafkaHandler` override. Filters should distinguish expected ignored
 * messages from unknown messages so new or malformed message types are not
 * silently discarded.
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
     * The generated default implementation performs no action and is not a
     * Kafka handler. Override this method and add `@KafkaHandler` to select it
     * for class-level listener dispatch.
     *
     * @param [payload] Details about a newly created account.
     * @param [receivedTopic] Kafka topic from which the record was received.
     * @param [xExampleCorrelationId] Identifier used to correlate related messages.
     * @param [xExampleSourceSystem] Optional name of the system that produced the message.
     */
    fun listenMyAccountCreated(
        @Payload
        @Valid
        payload: MyAccountCreatedPayload,

        @Header(name = KafkaHeaders.RECEIVED_TOPIC, required = true)
        receivedTopic: String,

        @Header(name = "X-EXAMPLE-CORRELATION-ID", required = true)
        xExampleCorrelationId: String,
        @Header(name = "X-EXAMPLE-SOURCE-SYSTEM", required = false)
        xExampleSourceSystem: String? = null,
    ) = Unit

    /**
     * Handles the `MyAccountUpdated` message received from this channel.
     *
     * The generated default implementation performs no action and is not a
     * Kafka handler. Override this method and add `@KafkaHandler` to select it
     * for class-level listener dispatch.
     *
     * @param [payload] Details about an account update.
     * @param [receivedTopic] Kafka topic from which the record was received.
     * @param [receivedKey] Numeric identifier of the updated account.
     * @param [xExampleCorrelationId] Identifier used to correlate related messages.
     * @param [xExampleSourceSystem] Optional name of the system that produced the message.
     */
    fun listenMyAccountUpdated(
        @Payload
        @Valid
        payload: MyAccountUpdatedPayload,

        @Header(name = KafkaHeaders.RECEIVED_TOPIC, required = true)
        receivedTopic: String,

        @Header(name = KafkaHeaders.RECEIVED_KEY, required = true)
        @Min(1L)
        @Max(9999999999L)
        receivedKey: Long,

        @Header(name = "X-EXAMPLE-CORRELATION-ID", required = true)
        xExampleCorrelationId: String,
        @Header(name = "X-EXAMPLE-SOURCE-SYSTEM", required = false)
        xExampleSourceSystem: String? = null,
    ) = Unit

    /**
     * Handles the `MyAccountClosed` message received from this channel.
     *
     * The generated default implementation performs no action and is not a
     * Kafka handler. Override this method and add `@KafkaHandler` to select it
     * for class-level listener dispatch.
     *
     * @param [payload] Details about a closed account.
     * @param [receivedTopic] Kafka topic from which the record was received.
     * @param [receivedKey] Identifies a particular account closure.
     * @param [xExampleCorrelationId] Identifier used to correlate related messages.
     * @param [xExampleSourceSystem] Optional name of the system that produced the message.
     */
    fun listenMyAccountClosed(
        @Payload
        @Valid
        payload: MyAccountClosedPayload,

        @Header(name = KafkaHeaders.RECEIVED_TOPIC, required = true)
        receivedTopic: String,

        @Header(name = KafkaHeaders.RECEIVED_KEY, required = true)
        @Valid
        receivedKey: MyAccountClosureKey,

        @Header(name = "X-EXAMPLE-CORRELATION-ID", required = true)
        xExampleCorrelationId: String,
        @Header(name = "X-EXAMPLE-SOURCE-SYSTEM", required = false)
        xExampleSourceSystem: String? = null,
    ) = Unit
}
