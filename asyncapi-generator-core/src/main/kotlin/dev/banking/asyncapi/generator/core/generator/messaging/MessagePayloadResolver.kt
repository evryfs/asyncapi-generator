package dev.banking.asyncapi.generator.core.generator.messaging

import dev.banking.asyncapi.generator.core.generator.schema.generatedSchemaName
import dev.banking.asyncapi.generator.core.model.messages.Message
import dev.banking.asyncapi.generator.core.model.messages.MessageInterface
import dev.banking.asyncapi.generator.core.model.schemas.MultiFormatSchema
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

/**
 * Resolves the message and payload identity shared by model and client generation.
 */
internal object MessagePayloadResolver {
    fun resolveMessage(messageInterface: MessageInterface): Message? = messageInterface.resolvedOrNull()

    /**
     * The payload of [message], the message with key [messageId] in its channel or components; `null` when it
     * has none or its reference does not lead to a schema.
     *
     * An inline payload is named after the message with a `Payload` suffix, a referenced one after its schema.
     */
    fun resolvePayload(
        message: Message,
        messageId: String,
    ): ResolvedMessagePayload? {
        val payload = message.payload ?: return null
        val typeName =
            when (payload) {
                is SchemaInterface.SchemaReference -> payload.reference.generatedSchemaName()
                else -> inlinePayloadTypeName(message, messageId)
            }
        return when (
            val model =
                when (payload) {
                    is SchemaInterface.SchemaInline -> payload.schema
                    is SchemaInterface.SchemaReference -> payload.reference.model
                    is SchemaInterface.MultiFormatSchemaInline -> payload.multiFormatSchema
                    is SchemaInterface.BooleanSchema -> payload
                }
        ) {
            is Schema -> ResolvedMessagePayload.AsyncApi(typeName = typeName, schema = model)
            is MultiFormatSchema -> ResolvedMessagePayload.MultiFormat(typeName = typeName, schema = model)
            is SchemaInterface.BooleanSchema -> ResolvedMessagePayload.Boolean(typeName = typeName, value = model.value)
            else -> null
        }
    }

    private fun inlinePayloadTypeName(
        message: Message,
        messageId: String,
    ): String =
        MessageNameResolver.resolve(message, messageId).let { messageName ->
            if (messageName.endsWith("Payload")) messageName else "${messageName}Payload"
        }
}

/** What a message payload resolves to. */
internal sealed interface ResolvedMessagePayload {
    /** Name of the generated payload type. */
    val typeName: String

    /**
     * A payload described by an AsyncAPI schema.
     *
     * @param typeName Name of the generated payload type.
     * @param schema The payload schema.
     */
    data class AsyncApi(
        override val typeName: String,
        val schema: Schema,
    ) : MessagePayload

    /**
     * A payload described in another schema format, such as native Avro or Protobuf.
     *
     * @param typeName Name of the generated payload type.
     * @param schema The payload schema and its format.
     */
    data class MultiFormat(
        override val typeName: String,
        val schema: MultiFormatSchema,
    ) : MessagePayload

    /**
     * A payload given as a boolean schema.
     *
     * @param typeName Name of the generated payload type.
     * @param value The boolean schema.
     */
    data class Boolean(
        override val typeName: String,
        val value: kotlin.Boolean,
    ) : ResolvedMessagePayload
}

/** A payload clients can be generated for: an AsyncAPI schema or a native schema. */
internal sealed interface MessagePayload : ResolvedMessagePayload
