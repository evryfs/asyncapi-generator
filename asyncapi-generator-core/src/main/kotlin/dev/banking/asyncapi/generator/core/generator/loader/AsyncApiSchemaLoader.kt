package dev.banking.asyncapi.generator.core.generator.loader

import dev.banking.asyncapi.generator.core.generator.util.MapperUtil
import dev.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument
import dev.banking.asyncapi.generator.core.model.channels.Channel
import dev.banking.asyncapi.generator.core.model.channels.ChannelInterface
import dev.banking.asyncapi.generator.core.model.components.ComponentInterface
import dev.banking.asyncapi.generator.core.model.messages.Message
import dev.banking.asyncapi.generator.core.model.messages.MessageInterface
import dev.banking.asyncapi.generator.core.model.messages.MessageTrait
import dev.banking.asyncapi.generator.core.model.messages.MessageTraitInterface
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

object AsyncApiSchemaLoader {

    fun load(asyncApiDocument: AsyncApiDocument): Map<String, Schema> {
        val collectedSchemas = mutableMapOf<String, Schema>()
        val componentNode = (asyncApiDocument.components as? ComponentInterface.ComponentInline)?.component
        val usageIndex = collectSchemaUsage(asyncApiDocument)
        componentNode?.schemas?.forEach { (name, schemaInterface) ->
            if (schemaInterface is SchemaInterface.SchemaInline) {
                val schemaName = MapperUtil.toPascalCase(name)
                if (!usageIndex.isHeaderOnly(schemaName)) {
                    collectedSchemas[schemaName] = schemaInterface.schema
                }
            }
        }

        // keep existing inline payload promotion from components.messages
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

        // keep existing inline payload promotion from channels
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

    private data class SchemaUsageIndex(
        val payloadSchemaNames: Set<String>,
        val headerSchemaNames: Set<String>,
    ) {
        fun isHeaderOnly(schemaName: String): Boolean =
            schemaName in headerSchemaNames && schemaName !in payloadSchemaNames
    }

    private fun collectSchemaUsage(asyncApiDocument: AsyncApiDocument): SchemaUsageIndex {
        val payloadNames = mutableSetOf<String>()
        val headerNames = mutableSetOf<String>()
        asyncApiDocument.channels?.values?.forEach { channelInterface ->
            val channel =
                when (channelInterface) {
                    is ChannelInterface.ChannelInline -> channelInterface.channel
                    is ChannelInterface.ChannelReference -> channelInterface.reference.model as? Channel
                } ?: return@forEach
            channel.messages?.values?.forEach { messageInterface ->
                val message =
                    when (messageInterface) {
                        is MessageInterface.MessageInline -> messageInterface.message
                        is MessageInterface.MessageReference -> messageInterface.reference.model as? Message
                    } ?: return@forEach
                message.payload?.let { collectFromSchemaInterface(it, payloadNames, mutableSetOf()) }
                message.headers?.let { collectFromSchemaInterface(it, headerNames, mutableSetOf()) }
                message.traits?.forEach { traitInterface ->
                    val trait =
                        when (traitInterface) {
                            is MessageTraitInterface.InlineMessageTrait -> traitInterface.trait
                            is MessageTraitInterface.ReferenceMessageTrait -> traitInterface.reference.model as? MessageTrait
                        } ?: return@forEach
                    trait.headers?.let { collectFromSchemaInterface(it, headerNames, mutableSetOf()) }
                }
            }
        }
        return SchemaUsageIndex(
            payloadSchemaNames = payloadNames,
            headerSchemaNames = headerNames,
        )
    }

    private fun collectFromSchemaInterface(
        schemaInterface: SchemaInterface,
        sink: MutableSet<String>,
        visitedRefs: MutableSet<String>,
    ) {
        when (schemaInterface) {
            is SchemaInterface.SchemaInline -> collectFromSchema(schemaInterface.schema, sink, visitedRefs)
            is SchemaInterface.SchemaReference -> {
                collectFromReference(schemaInterface.reference, sink, visitedRefs)
                (schemaInterface.reference.model as? Schema)?.let { collectFromSchema(it, sink, visitedRefs) }
            }
            else -> Unit
        }
    }

    private fun collectFromReference(
        reference: Reference,
        sink: MutableSet<String>,
        visitedRefs: MutableSet<String>,
    ) {
        val rawName = reference.ref.substringAfterLast('/')
        val schemaName = MapperUtil.toPascalCase(rawName)
        if (schemaName.isBlank() || !visitedRefs.add(schemaName)) return
        sink.add(schemaName)
    }

    private fun collectFromSchema(
        schema: Schema,
        sink: MutableSet<String>,
        visitedRefs: MutableSet<String>,
    ) {
        schema.properties?.values?.forEach { collectFromSchemaInterface(it, sink, visitedRefs) }
        schema.items?.let { collectFromSchemaInterface(it, sink, visitedRefs) }
        schema.additionalProperties?.let { collectFromSchemaInterface(it, sink, visitedRefs) }
        schema.oneOf?.forEach { collectFromSchemaInterface(it, sink, visitedRefs) }
        schema.anyOf?.forEach { collectFromSchemaInterface(it, sink, visitedRefs) }
        schema.allOf?.forEach { collectFromSchemaInterface(it, sink, visitedRefs) }
        schema.not?.let { collectFromSchemaInterface(it, sink, visitedRefs) }
    }
}
