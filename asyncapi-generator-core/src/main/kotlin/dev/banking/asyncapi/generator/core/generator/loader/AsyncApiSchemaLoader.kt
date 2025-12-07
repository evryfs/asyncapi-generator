package dev.banking.asyncapi.generator.core.generator.loader

import dev.banking.asyncapi.generator.core.generator.util.MapperUtil
import dev.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument
import dev.banking.asyncapi.generator.core.model.components.ComponentInterface
import dev.banking.asyncapi.generator.core.model.messages.MessageInterface
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

object AsyncApiSchemaLoader {

    fun load(asyncApiDocument: AsyncApiDocument): Map<String, Schema> {
        val collectedSchemas = mutableMapOf<String, Schema>()
        val componentNode = (asyncApiDocument.components as? ComponentInterface.ComponentInline)?.component ?: return emptyMap()

        // 1. Load explicit Schemas from components.schemas
        componentNode.schemas?.forEach { (name, schemaInterface) ->
            if (schemaInterface is SchemaInterface.SchemaInline) {
                collectedSchemas[name] = schemaInterface.schema
            }
        }

        // 2. Load implicit Schemas from components.messages (payloads)
        componentNode.messages?.forEach { (messageName, messageInterface) ->
            val message = (messageInterface as? MessageInterface.MessageInline)?.message ?: return@forEach

            // If payload is an Inline Schema, we promote it to a top-level schema
            // using the Message Name (or Title) as the Schema Name.
            if (message.payload is SchemaInterface.SchemaInline) {
                val inlinePayload = message.payload.schema

                // Determine a name for this schema
                val schemaName = MapperUtil.toPascalCase(messageName)

                // Only add if not already defined (explicit schemas take precedence)
                if (!collectedSchemas.containsKey(schemaName)) {
                    collectedSchemas[schemaName] = inlinePayload
                }
            }
        }

        return collectedSchemas
    }
}
