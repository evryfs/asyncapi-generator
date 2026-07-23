package com.example.account.client.consumer

import com.example.account.model.MyAccountUpdatedPayload
import jakarta.validation.Valid
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.validation.annotation.Validated

/**
 * Consumer contract for handling messages from the `my.accounts.{environment}.updated.v1` topic.
 * The contract exposes the Kafka record key, message payload, and contract-defined headers as method parameters.
 */
@Validated
interface MyAccountUpdatedConsumer {
    companion object {
        /** Spring-resolvable Kafka topic address declared by this AsyncAPI channel. */
        const val MY_ACCOUNT_UPDATED_TOPIC_ADDRESS: String = "my.accounts.\${kafka.environment}.updated.v1"
    }

    /**
     * @param payload Details about an account update.
     * @param topic Kafka topic from which the record was received.
     * @param key Kafka record key.
     * @param correlationId Identifier used to correlate related messages.
     * @param sourceSystem Optional name of the system that produced the message.
     */
    fun listen(
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
}
