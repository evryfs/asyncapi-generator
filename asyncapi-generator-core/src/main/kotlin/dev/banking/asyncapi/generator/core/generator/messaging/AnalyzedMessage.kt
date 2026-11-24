package dev.banking.asyncapi.generator.core.generator.messaging

import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

/**
 * A message of a channel, as client generation uses it.
 *
 * @param messageId Key of the message in its channel.
 * @param messageName Source-code name of the message.
 * @param payload Payload of the message, or `null` when the message has none.
 * @param keySchema Schema of the Kafka record key, or `null` when the message declares none.
 * @param headers Headers of the message, or `null` when it declares none.
 */
internal data class AnalyzedMessage(
    val messageId: String,
    val messageName: String,
    val payload: MessagePayload?,
    val keySchema: SchemaInterface? = null,
    val headers: AnalyzedMessageHeaders? = null,
) {
    /** The payload schema, when the payload is an AsyncAPI schema. */
    val schema: Schema? get() = (payload as? ResolvedMessagePayload.AsyncApi)?.schema

    /** The native payload, when the payload uses a native schema format. */
    val nativePayload: ResolvedMessagePayload.MultiFormat? get() = payload as? ResolvedMessagePayload.MultiFormat
}
