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
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.SchemaNameCollision
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

object AsyncApiSchemaLoader {

    fun load(asyncApiDocument: AsyncApiDocument): LoadedSchemas {
        val collectedSchemas = mutableMapOf<String, Schema>()
        val allComponentSchemas = mutableMapOf<String, Schema>()
        val collectedMultiFormatSchemas = mutableMapOf<String, MultiFormatSchema>()
        val originalNamesByGenerated = mutableMapOf<String, MutableList<String>>()
        val componentNode = resolveComponent(asyncApiDocument.components)
        val usageIndex = collectSchemaUsage(asyncApiDocument)
        componentNode?.schemas?.forEach { (name, schemaInterface) ->
            val schemaName = MapperUtil.toPascalCase(name)
            originalNamesByGenerated.getOrPut(schemaName) { mutableListOf() }.add(name)
            detectSchemaNameCollision(originalNamesByGenerated)
            when (schemaInterface) {
                is SchemaInterface.SchemaInline -> {
                    allComponentSchemas[schemaName] = schemaInterface.schema
                    if (!usageIndex.isHeaderOnly(schemaName)) {
                        collectedSchemas[schemaName] = schemaInterface.schema
                    }
                }
                is SchemaInterface.MultiFormatSchemaInline ->
                    collectedMultiFormatSchemas[schemaName] = schemaInterface.multiFormatSchema
                is SchemaInterface.SchemaReference ->
                    when (val model = schemaInterface.reference.model) {
                        is Schema -> {
                            allComponentSchemas[schemaName] = model
                            if (!usageIndex.isHeaderOnly(schemaName)) {
                                collectedSchemas[schemaName] = model
                            }
                        }
                        is MultiFormatSchema -> collectedMultiFormatSchemas[schemaName] = model
                        else -> Unit
                    }
                is SchemaInterface.BooleanSchema -> Unit
            }
        }

        collectMessages(asyncApiDocument, componentNode).forEach { (messageKey, messageInterface) ->
            val message = MessagePayloadResolver.resolveMessage(messageInterface) ?: return@forEach
            when (val payload = MessagePayloadResolver.resolvePayload(message, messageKey)) {
                is ResolvedMessagePayload.AsyncApi ->
                    collectedSchemas.putIfAbsent(payload.typeName, payload.schema)
                is ResolvedMessagePayload.MultiFormat ->
                    collectedMultiFormatSchemas.putIfAbsent(payload.typeName, payload.schema)
                null -> Unit
            }
            collectKafkaKeySchema(
                messageKey = messageKey,
                message = message,
                collectedSchemas = collectedSchemas,
            )
        }
        return LoadedSchemas(
            schemas = collectedSchemas,
            allComponentSchemas = allComponentSchemas,
            multiFormatSchemas = collectedMultiFormatSchemas,
        )
    }

    private fun resolveComponent(componentInterface: ComponentInterface?): Component? =
        when (componentInterface) {
            is ComponentInterface.ComponentInline -> componentInterface.component
            is ComponentInterface.ComponentReference -> componentInterface.reference.model as? Component
            null -> null
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
        message: Message,
        collectedSchemas: MutableMap<String, Schema>,
    ) {
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

    private fun detectSchemaNameCollision(originalNamesByGenerated: Map<String, List<String>>) {
        for ((generatedName, originalNames) in originalNamesByGenerated) {
            if (originalNames.size > 1) {
                throw SchemaNameCollision(
                    originalNames = originalNames.distinct(),
                    generatedName = generatedName,
                )
            }
        }
    }
}
