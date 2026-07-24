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
 * Producer contract for publishing messages to the `my.accounts.{environment}.updated.v1` topic.
 * The contract exposes message payloads and contract-defined headers as method parameters.
 * Messages with a `bindings.kafka.key` schema also expose a typed Kafka record key.
 */
@Validated
interface MyAccountUpdatedProducer {
    companion object {
        /** Spring-resolvable Kafka topic address declared by this AsyncAPI channel. */
        const val MY_ACCOUNT_UPDATED_TOPIC_ADDRESS: String = "my.accounts.\${kafka.environment}.updated.v1"
    }

    /**
     * @param payload Details about an account update.
     * @param messageKey Identifies an account within an institution.
     * @param X_EXAMPLE_CORRELATION_ID Value for the `X-EXAMPLE-CORRELATION-ID` Kafka message header. Implementations must add this value to the outgoing Kafka
     * record. Identifier used to correlate related messages.
     * @param X_EXAMPLE_SOURCE_SYSTEM Value for the `X-EXAMPLE-SOURCE-SYSTEM` Kafka message header. Implementations must add this value to the outgoing Kafka
     * record. Optional name of the system that produced the message.
     * @return Future completed with Kafka record metadata after successful publication.
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
    ): CompletableFuture<RecordMetadata>
}
