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
 * Consumer contract for handling messages from the `my.accounts.{environment}.lifecycle.v1` topic.
 * The contract exposes the Kafka record key, message payload, and contract-defined headers as method parameters.
 */
@Validated
interface MyAccountLifecycleConsumer {
    companion object {
        /** Spring-resolvable Kafka topic address declared by this AsyncAPI channel. */
        const val MY_ACCOUNT_LIFECYCLE_TOPIC_ADDRESS: String = "my.accounts.\${kafka.environment}.lifecycle.v1"
    }

    /**
     * @param payload Details about a newly created account.
     * @param topic Kafka topic from which the record was received.
     * @param key Kafka record key.
     * @param correlationId Identifier used to correlate related messages.
     * @param sourceSystem Optional name of the system that produced the message.
     */
    fun listenMyAccountCreated(
        @Payload
        @Valid
        payload: MyAccountCreatedPayload,

        @Header(name = KafkaHeaders.RECEIVED_TOPIC, required = true)
        topic: String,

        @Header(name = KafkaHeaders.RECEIVED_KEY, required = false)
        key: String?,

        @Header(name = "correlationId", required = true)
        correlationId: String,
        @Header(name = "sourceSystem", required = false)
        sourceSystem: String? = null,
    )

    /**
     * @param payload Details about an account update.
     * @param topic Kafka topic from which the record was received.
     * @param key Kafka record key.
     * @param correlationId Identifier used to correlate related messages.
     * @param sourceSystem Optional name of the system that produced the message.
     */
    fun listenMyAccountUpdated(
        @Payload
        @Valid
        payload: MyAccountUpdatedPayload,

        @Header(name = KafkaHeaders.RECEIVED_TOPIC, required = true)
        topic: String,

        @Header(name = KafkaHeaders.RECEIVED_KEY, required = false)
        key: String?,

        @Header(name = "correlationId", required = true)
        correlationId: String,
        @Header(name = "sourceSystem", required = false)
        sourceSystem: String? = null,
    )

    /**
     * @param payload Details about a closed account.
     * @param topic Kafka topic from which the record was received.
     * @param key Kafka record key.
     * @param correlationId Identifier used to correlate related messages.
     * @param sourceSystem Optional name of the system that produced the message.
     */
    fun listenMyAccountClosed(
        @Payload
        @Valid
        payload: MyAccountClosedPayload,

        @Header(name = KafkaHeaders.RECEIVED_TOPIC, required = true)
        topic: String,

        @Header(name = KafkaHeaders.RECEIVED_KEY, required = false)
        key: String?,

        @Header(name = "correlationId", required = true)
        correlationId: String,
        @Header(name = "sourceSystem", required = false)
        sourceSystem: String? = null,
    )
}
