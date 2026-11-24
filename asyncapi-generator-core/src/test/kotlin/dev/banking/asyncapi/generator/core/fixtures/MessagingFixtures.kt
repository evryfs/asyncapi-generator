package dev.banking.asyncapi.generator.core.fixtures

import dev.banking.asyncapi.generator.core.generator.messaging.AnalyzedMessage
import dev.banking.asyncapi.generator.core.generator.messaging.AnalyzedMessageHeaders
import dev.banking.asyncapi.generator.core.generator.messaging.ResolvedMessagePayload
import dev.banking.asyncapi.generator.core.generator.messaging.kafka.KafkaKey
import dev.banking.asyncapi.generator.core.model.schemas.MultiFormatSchema
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

/** An analyzed message with an AsyncAPI schema payload, or no payload when [schema] is `null`. */
internal fun analyzedMessage(
    messageName: String,
    payloadTypeName: String?,
    schema: Schema?,
    keySchema: SchemaInterface? = null,
    headers: AnalyzedMessageHeaders? = null,
    messageId: String = messageName,
): AnalyzedMessage =
    AnalyzedMessage(
        messageId = messageId,
        messageName = messageName,
        payload = schema?.let { ResolvedMessagePayload.AsyncApi(requireNotNull(payloadTypeName), it) },
        kafkaKey = keySchema?.let { KafkaKey.of(messageName, it) },
        headers = headers,
    )

/** An analyzed message with a payload in a native schema format. */
internal fun nativeMessage(
    messageName: String,
    payloadName: String,
    schema: MultiFormatSchema,
    keySchema: SchemaInterface? = null,
    headers: AnalyzedMessageHeaders? = null,
    messageId: String = messageName,
): AnalyzedMessage =
    AnalyzedMessage(
        messageId = messageId,
        messageName = messageName,
        payload = ResolvedMessagePayload.MultiFormat(payloadName, schema),
        kafkaKey = keySchema?.let { KafkaKey.of(messageName, it) },
        headers = headers,
    )
