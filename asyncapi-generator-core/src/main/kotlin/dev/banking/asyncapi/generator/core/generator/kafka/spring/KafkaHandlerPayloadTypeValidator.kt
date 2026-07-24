package dev.banking.asyncapi.generator.core.generator.kafka.spring

import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.AmbiguousKafkaHandlerPayloadTypes

/**
 * Ensures that a multi-message Spring Kafka consumer has one dispatch type per handler.
 *
 * Expected behavior is covered by:
 * - `KafkaHandlerPayloadTypeValidatorTest`
 */
internal object KafkaHandlerPayloadTypeValidator {
    fun validate(
        channelName: String,
        payloads: List<KafkaPayload>,
    ) {
        val collisions =
            payloads
                .groupBy(KafkaPayload::payloadType)
                .filterValues { messages -> messages.size > 1 }
                .mapValues { (_, messages) -> messages.map(KafkaPayload::messageName) }

        if (collisions.isNotEmpty()) {
            throw AmbiguousKafkaHandlerPayloadTypes(
                channelName = channelName,
                collisions = collisions,
            )
        }
    }
}
