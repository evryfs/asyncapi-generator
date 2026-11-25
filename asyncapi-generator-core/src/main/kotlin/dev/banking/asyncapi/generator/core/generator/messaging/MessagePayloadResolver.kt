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

    fun resolvePayload(
        message: Message,
        messageId: String,
    ): ResolvedMessagePayload? {
        val inlinePayloadTypeName =
            MessageNameResolver.resolve(message, messageId).let { messageName ->
                if (messageName.endsWith("Payload")) messageName else "${messageName}Payload"
            }

        return when (val payload = message.payload) {
            is SchemaInterface.SchemaInline ->
                ResolvedMessagePayload.AsyncApi(
                    typeName = inlinePayloadTypeName,
                    schema = payload.schema,
                )
            is SchemaInterface.SchemaReference -> {
                val typeName = payload.reference.generatedSchemaName()
                when (val model = payload.reference.model) {
                    is Schema ->
                        ResolvedMessagePayload.AsyncApi(
                            typeName = typeName,
                            schema = model,
                        )
                    is MultiFormatSchema ->
                        ResolvedMessagePayload.MultiFormat(
                            typeName = typeName,
                            schema = model,
                        )
                    is SchemaInterface.BooleanSchema ->
                        ResolvedMessagePayload.Boolean(
                            typeName = typeName,
                            value = model.value,
                        )
                    else -> null
                }
            }
            is SchemaInterface.MultiFormatSchemaInline ->
                ResolvedMessagePayload.MultiFormat(
                    typeName = inlinePayloadTypeName,
                    schema = payload.multiFormatSchema,
                )
            is SchemaInterface.BooleanSchema ->
                ResolvedMessagePayload.Boolean(
                    typeName = inlinePayloadTypeName,
                    value = payload.value,
                )
            null -> null
        }
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
