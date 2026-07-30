package dev.banking.asyncapi.generator.core.generator.loader

import dev.banking.asyncapi.generator.core.generator.analyzer.MessageNameResolver
import dev.banking.asyncapi.generator.core.generator.analyzer.MessagePayloadResolver
import dev.banking.asyncapi.generator.core.generator.analyzer.ResolvedMessagePayload
import dev.banking.asyncapi.generator.core.generator.kafka.KafkaKeySchemaResolver
import dev.banking.asyncapi.generator.core.generator.kafka.kafkaKeySchema
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil
import dev.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument
import dev.banking.asyncapi.generator.core.model.channels.Channel
import dev.banking.asyncapi.generator.core.model.channels.ChannelInterface
import dev.banking.asyncapi.generator.core.model.components.Component
import dev.banking.asyncapi.generator.core.model.components.ComponentInterface
import dev.banking.asyncapi.generator.core.model.messages.Message
import dev.banking.asyncapi.generator.core.model.messages.MessageInterface
import dev.banking.asyncapi.generator.core.model.messages.MessageTrait
import dev.banking.asyncapi.generator.core.model.messages.MessageTraitInterface
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.schemas.MultiFormatSchema
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

object AsyncApiSchemaLoader {

    fun load(asyncApiDocument: AsyncApiDocument): Map<String, Schema> {
        val collectedSchemas = mutableMapOf<String, Schema>()
        val componentNode = resolveComponent(asyncApiDocument.components)
        val usageIndex = collectSchemaUsage(asyncApiDocument)
        componentNode?.schemas?.forEach { (name, schemaInterface) ->
            val schema = resolveSchema(schemaInterface) ?: return@forEach
            val schemaName = MapperUtil.toPascalCase(name)
            if (!usageIndex.isHeaderOnly(schemaName)) {
                collectedSchemas[schemaName] = schema
            }
        }

        collectMessages(asyncApiDocument, componentNode).forEach { (messageKey, messageInterface) ->
            val message = MessagePayloadResolver.resolveMessage(messageInterface) ?: return@forEach
            when (val payload = MessagePayloadResolver.resolvePayload(message, messageKey)) {
                is ResolvedMessagePayload.AsyncApi ->
                    collectedSchemas.putIfAbsent(payload.typeName, payload.schema)
                is ResolvedMessagePayload.MultiFormat,
                null,
                -> Unit
            }
            collectKafkaKeySchema(
                messageKey = messageKey,
                messageInterface = messageInterface,
                collectedSchemas = collectedSchemas,
            )
        }
        return collectedSchemas
    }

    fun loadMultiFormatSchemas(asyncApiDocument: AsyncApiDocument): Map<String, MultiFormatSchema> {
        val collectedSchemas = mutableMapOf<String, MultiFormatSchema>()
        val componentNode = resolveComponent(asyncApiDocument.components)

        componentNode?.schemas?.forEach { (name, schemaInterface) ->
            val multiFormatSchema = resolveMultiFormatSchema(schemaInterface) ?: return@forEach
            collectedSchemas[MapperUtil.toPascalCase(name)] = multiFormatSchema
        }

        collectMessages(asyncApiDocument, componentNode).forEach { (messageKey, messageInterface) ->
            val message = MessagePayloadResolver.resolveMessage(messageInterface) ?: return@forEach
            when (val payload = MessagePayloadResolver.resolvePayload(message, messageKey)) {
                is ResolvedMessagePayload.MultiFormat ->
                    collectedSchemas.putIfAbsent(payload.typeName, payload.schema)
                is ResolvedMessagePayload.AsyncApi,
                null,
                -> Unit
            }
        }

        return collectedSchemas
    }

    private fun resolveComponent(componentInterface: ComponentInterface?): Component? =
        when (componentInterface) {
            is ComponentInterface.ComponentInline -> componentInterface.component
            is ComponentInterface.ComponentReference -> componentInterface.reference.model as? Component
            null -> null
        }

    private fun resolveSchema(schemaInterface: SchemaInterface): Schema? =
        when (schemaInterface) {
            is SchemaInterface.SchemaInline -> schemaInterface.schema
            is SchemaInterface.SchemaReference -> schemaInterface.reference.model as? Schema
            else -> null
        }

    private fun resolveMultiFormatSchema(schemaInterface: SchemaInterface): MultiFormatSchema? =
        when (schemaInterface) {
            is SchemaInterface.MultiFormatSchemaInline -> schemaInterface.multiFormatSchema
            is SchemaInterface.SchemaReference -> schemaInterface.reference.model as? MultiFormatSchema
            else -> null
        }

    private fun collectMessages(
        asyncApiDocument: AsyncApiDocument,
        component: Component?,
    ): List<Pair<String, MessageInterface>> =
        buildList {
            component?.messages?.forEach { (messageKey, messageInterface) ->
                add(messageKey to messageInterface)
            }
            asyncApiDocument.channels?.values?.forEach { channelInterface ->
                val channel =
                    when (channelInterface) {
                        is ChannelInterface.ChannelInline -> channelInterface.channel
                        is ChannelInterface.ChannelReference -> channelInterface.reference.model as? Channel
                    } ?: return@forEach
                channel.messages?.forEach { (messageKey, messageInterface) ->
                    add(messageKey to messageInterface)
                }
            }
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
                if (collectFromReference(schemaInterface.reference, sink, visitedRefs)) {
                    (schemaInterface.reference.model as? Schema)?.let {
                        collectFromSchema(it, sink, visitedRefs)
                    }
                }
            }
            else -> Unit
        }
    }

    private fun collectFromReference(
        reference: Reference,
        sink: MutableSet<String>,
        visitedRefs: MutableSet<String>,
    ): Boolean {
        val rawName = reference.ref.substringAfterLast('/')
        val schemaName = MapperUtil.toPascalCase(rawName)
        if (schemaName.isBlank()) return false
        val referenceIdentity = "${reference.sourceId.orEmpty()}:${reference.ref}"
        if (!visitedRefs.add(referenceIdentity)) return false
        sink.add(schemaName)
        return true
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

    private fun collectKafkaKeySchema(
        messageKey: String,
        messageInterface: MessageInterface,
        collectedSchemas: MutableMap<String, Schema>,
    ) {
        val message = MessagePayloadResolver.resolveMessage(messageInterface) ?: return
        val keySchema = message.kafkaKeySchema() ?: return
        val keyModel =
            KafkaKeySchemaResolver.resolveObjectModelOrNull(
                messageName = messageBaseName(message, messageKey),
                schema = keySchema,
            ) ?: return

        collectedSchemas.putIfAbsent(keyModel.name, keyModel.schema)
    }

    private fun messageBaseName(
        message: Message,
        messageKey: String,
    ): String = MessageNameResolver.resolve(message, messageKey)
}
