package com.example.account.client.producer

import com.example.account.model.MyAccountClosedPayload
import com.example.account.model.MyAccountCreatedPayload
import com.example.account.model.MyAccountUpdatedPayload
import jakarta.validation.Valid
import java.util.concurrent.CompletableFuture
import org.apache.kafka.clients.producer.RecordMetadata
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.validation.annotation.Validated

/**
 * Producer contract for publishing messages to the `my.accounts.{environment}.lifecycle.v1` topic.
 * The contract exposes the Kafka record key, message payload, and contract-defined headers as method parameters.
 */
@Validated
interface MyAccountLifecycleProducer {
    companion object {
        /** Spring-resolvable Kafka topic address declared by this AsyncAPI channel. */
        const val MY_ACCOUNT_LIFECYCLE_TOPIC_ADDRESS: String = "my.accounts.\${kafka.environment}.lifecycle.v1"
    }

    /**
     * @param payload Details about a newly created account.
     * @param key Kafka record key.
     * @param correlationId Identifier used to correlate related messages.
     * @param sourceSystem Optional name of the system that produced the message.
     * @return Future completed with Kafka record metadata after successful publication.
     */
    fun sendMyAccountCreated(
        @Payload
        @Valid
        payload: MyAccountCreatedPayload,

        @Header(name = KafkaHeaders.KEY, required = true)
        key: String,

        @Header(name = "correlationId", required = true)
        correlationId: String,
        @Header(name = "sourceSystem", required = false)
        sourceSystem: String? = null,
    ): CompletableFuture<RecordMetadata>

    /**
     * @param payload Details about an account update.
     * @param key Kafka record key.
     * @param correlationId Identifier used to correlate related messages.
     * @param sourceSystem Optional name of the system that produced the message.
     * @return Future completed with Kafka record metadata after successful publication.
     */
    fun sendMyAccountUpdated(
        @Payload
        @Valid
        payload: MyAccountUpdatedPayload,

        @Header(name = KafkaHeaders.KEY, required = true)
        key: String,

        @Header(name = "correlationId", required = true)
        correlationId: String,
        @Header(name = "sourceSystem", required = false)
        sourceSystem: String? = null,
    ): CompletableFuture<RecordMetadata>

    /**
     * @param payload Details about a closed account.
     * @param key Kafka record key.
     * @param correlationId Identifier used to correlate related messages.
     * @param sourceSystem Optional name of the system that produced the message.
     * @return Future completed with Kafka record metadata after successful publication.
     */
    fun sendMyAccountClosed(
        @Payload
        @Valid
        payload: MyAccountClosedPayload,

        @Header(name = KafkaHeaders.KEY, required = true)
        key: String,

        @Header(name = "correlationId", required = true)
        correlationId: String,
        @Header(name = "sourceSystem", required = false)
        sourceSystem: String? = null,
    ): CompletableFuture<RecordMetadata>
}
