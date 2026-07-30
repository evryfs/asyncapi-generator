package dev.banking.asyncapi.generator.core.generator.analyzer

import dev.banking.asyncapi.generator.core.generator.util.MapperUtil
import dev.banking.asyncapi.generator.core.model.messages.Message
import dev.banking.asyncapi.generator.core.model.messages.MessageInterface
import dev.banking.asyncapi.generator.core.model.schemas.MultiFormatSchema
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

/**
 * Resolves the message and payload identity shared by model and client generation.
 */
internal object MessagePayloadResolver {
    fun resolveMessage(messageInterface: MessageInterface): Message? =
        when (messageInterface) {
            is MessageInterface.MessageInline -> messageInterface.message
            is MessageInterface.MessageReference -> messageInterface.reference.model as? Message
        }

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
                val typeName = MapperUtil.toPascalCase(payload.reference.ref.substringAfterLast('/'))
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
                    else -> null
                }
            }
            is SchemaInterface.MultiFormatSchemaInline ->
                ResolvedMessagePayload.MultiFormat(
                    typeName = inlinePayloadTypeName,
                    schema = payload.multiFormatSchema,
                )
            else -> null
        }
    }
}

internal sealed interface ResolvedMessagePayload {
    val typeName: String

    data class AsyncApi(
        override val typeName: String,
        val schema: Schema,
    ) : ResolvedMessagePayload

    data class MultiFormat(
        override val typeName: String,
        val schema: MultiFormatSchema,
    ) : ResolvedMessagePayload
}
