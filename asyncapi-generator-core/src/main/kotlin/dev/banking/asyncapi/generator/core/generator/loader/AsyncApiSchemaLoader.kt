package dev.banking.asyncapi.generator.core.generator.loader

import dev.banking.asyncapi.generator.core.generator.util.MapperUtil
import dev.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument
import dev.banking.asyncapi.generator.core.model.channels.ChannelInterface
import dev.banking.asyncapi.generator.core.model.components.ComponentInterface
import dev.banking.asyncapi.generator.core.model.messages.MessageInterface
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

object AsyncApiSchemaLoader {
    fun load(asyncApiDocument: AsyncApiDocument): Map<String, Schema> {
        val collectedSchemas = mutableMapOf<String, Schema>()
        val componentNode =
            (asyncApiDocument.components as? ComponentInterface.ComponentInline)?.component

        componentNode?.schemas?.forEach { (name, schemaInterface) ->
            if (schemaInterface is SchemaInterface.SchemaInline) {
                val schemaName = MapperUtil.toPascalCase(name)
                collectedSchemas[schemaName] = schemaInterface.schema
            }
        }

        componentNode?.messages?.forEach { (messageKey, messageInterface) ->
            val message = (messageInterface as? MessageInterface.MessageInline)?.message ?: return@forEach

            if (message.payload is SchemaInterface.SchemaInline) {
                val inlinePayload = message.payload.schema
                val baseName = MapperUtil.toPascalCase(message.name ?: message.title ?: messageKey)
                val schemaName = if (baseName.endsWith("Payload")) baseName else "${baseName}Payload"
                if (!collectedSchemas.containsKey(schemaName)) {
                    collectedSchemas[schemaName] = inlinePayload
                }
            }
        }

        asyncApiDocument.channels?.forEach { (_, channelInterface) ->
            val channel = (channelInterface as? ChannelInterface.ChannelInline)?.channel ?: return@forEach
            channel.messages?.forEach { (messageKey, messageInterface) ->
                val message = (messageInterface as? MessageInterface.MessageInline)?.message ?: return@forEach
                val inlinePayload = message.payload as? SchemaInterface.SchemaInline ?: return@forEach

                val baseName = MapperUtil.toPascalCase(message.name ?: message.title ?: messageKey)
                val schemaName = if (baseName.endsWith("Payload")) baseName else "${baseName}Payload"
                if (!collectedSchemas.containsKey(schemaName)) {
                    collectedSchemas[schemaName] = inlinePayload.schema
                }
            }
        }

        return collectedSchemas
    }
}
