package dev.banking.asyncapi.generator.core.generator.loader

import dev.banking.asyncapi.generator.core.generator.analyzer.MessageNameResolver
import dev.banking.asyncapi.generator.core.generator.analyzer.MessagePayloadResolver
import dev.banking.asyncapi.generator.core.generator.analyzer.ResolvedMessagePayload
import dev.banking.asyncapi.generator.core.generator.kafka.KafkaKeySchemaResolver
import dev.banking.asyncapi.generator.core.generator.kafka.kafkaKeySchema
import dev.banking.asyncapi.generator.core.generator.schema.SchemaDeclarationCatalog
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil
import dev.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument
import dev.banking.asyncapi.generator.core.model.channels.Channel
import dev.banking.asyncapi.generator.core.model.channels.ChannelInterface
import dev.banking.asyncapi.generator.core.model.components.Component
import dev.banking.asyncapi.generator.core.model.components.ComponentInterface
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.SchemaNameCollision
import dev.banking.asyncapi.generator.core.model.messages.Message
import dev.banking.asyncapi.generator.core.model.messages.MessageInterface
import dev.banking.asyncapi.generator.core.model.messages.MessageTrait
import dev.banking.asyncapi.generator.core.model.messages.MessageTraitInterface
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.references.parseReference
import dev.banking.asyncapi.generator.core.model.schemas.MultiFormatSchema
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

object AsyncApiSchemaLoader {

    fun load(asyncApiDocument: AsyncApiDocument): LoadedSchemas {
        val collectedSchemas = mutableMapOf<String, Schema>()
        val contractDeclarations = ContractDeclarationRegistry()
        val componentNode = resolveComponent(asyncApiDocument.components)
        val usageIndex = collectSchemaUsage(asyncApiDocument)
        componentNode?.schemas?.forEach { (name, schemaInterface) ->
            val schemaName = MapperUtil.toPascalCase(name)
            val origin = "component schema components.schemas['$name']"
            when (schemaInterface) {
                is SchemaInterface.SchemaInline -> {
                    contractDeclarations.register(
                        generatedName = schemaName,
                        declaration =
                            ContractDeclaration.AsyncApi(
                                schema = schemaInterface.schema,
                                identity = ContractDeclarationIdentity.ComponentSchema(name),
                            ),
                        origin = origin,
                    )
                    if (!usageIndex.isHeaderOnly(schemaName)) {
                        collectedSchemas[schemaName] = schemaInterface.schema
                    }
                }
                is SchemaInterface.MultiFormatSchemaInline -> {
                    contractDeclarations.register(
                        generatedName = schemaName,
                        declaration =
                            ContractDeclaration.MultiFormat(
                                schema = schemaInterface.multiFormatSchema,
                                identity = ContractDeclarationIdentity.ComponentSchema(name),
                            ),
                        origin = origin,
                    )
                }
                is SchemaInterface.SchemaReference ->
                    when (val model = schemaInterface.reference.model) {
                        is Schema -> {
                            contractDeclarations.register(
                                generatedName = schemaName,
                                declaration =
                                    ContractDeclaration.AsyncApi(
                                        schema = model,
                                        identity = ContractDeclarationIdentity.ComponentSchema(name),
                                    ),
                                origin = origin,
                            )
                            if (!usageIndex.isHeaderOnly(schemaName)) {
                                collectedSchemas[schemaName] = model
                            }
                        }
                        is MultiFormatSchema ->
                            contractDeclarations.register(
                                generatedName = schemaName,
                                declaration =
                                    ContractDeclaration.MultiFormat(
                                        schema = model,
                                        identity = ContractDeclarationIdentity.ComponentSchema(name),
                                    ),
                                origin = origin,
                            )
                        is SchemaInterface.BooleanSchema ->
                            contractDeclarations.register(
                                generatedName = schemaName,
                                declaration = ContractDeclaration.BooleanSchema(model.value),
                                origin = origin,
                            )
                        else -> Unit
                    }
                is SchemaInterface.BooleanSchema ->
                    contractDeclarations.register(
                        generatedName = schemaName,
                        declaration = ContractDeclaration.BooleanSchema(schemaInterface.value),
                        origin = origin,
                    )
            }
        }

        collectMessages(asyncApiDocument, componentNode).forEach { collectedMessage ->
            val message = MessagePayloadResolver.resolveMessage(collectedMessage.message) ?: return@forEach
            val declarationIdentity = payloadDeclarationIdentity(collectedMessage, message)
            when (val payload = MessagePayloadResolver.resolvePayload(message, collectedMessage.messageKey)) {
                is ResolvedMessagePayload.AsyncApi -> {
                    contractDeclarations.register(
                        generatedName = payload.typeName,
                        declaration = ContractDeclaration.AsyncApi(payload.schema, declarationIdentity),
                        origin = payloadOrigin(collectedMessage.origin, message),
                    )
                    collectedSchemas.putIfAbsent(payload.typeName, payload.schema)
                }
                is ResolvedMessagePayload.MultiFormat -> {
                    contractDeclarations.register(
                        generatedName = payload.typeName,
                        declaration = ContractDeclaration.MultiFormat(payload.schema, declarationIdentity),
                        origin = payloadOrigin(collectedMessage.origin, message),
                    )
                }
                is ResolvedMessagePayload.Boolean -> {
                    contractDeclarations.register(
                        generatedName = payload.typeName,
                        declaration = ContractDeclaration.BooleanSchema(payload.value),
                        origin = payloadOrigin(collectedMessage.origin, message),
                    )
                }
                null -> Unit
            }
            collectKafkaKeySchema(
                messageKey = collectedMessage.messageKey,
                message = message,
                collectedSchemas = collectedSchemas,
            )
        }
        return LoadedSchemas(
            schemas = collectedSchemas,
            schemaDeclarations =
                SchemaDeclarationCatalog(
                    asyncApiSchemas = contractDeclarations.asyncApiSchemas,
                    multiFormatSchemas = contractDeclarations.multiFormatSchemas,
                    booleanSchemas = contractDeclarations.booleanSchemas,
                ),
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
    ): List<CollectedMessage> =
        buildList {
            val componentMessages = component?.messages.orEmpty()
            componentMessages.forEach { (messageKey, messageInterface) ->
                add(
                    CollectedMessage(
                        messageKey = messageKey,
                        message = messageInterface,
                        origin = "components.messages['$messageKey']",
                        payloadIdentity = componentMessagePayloadIdentity(messageKey, componentMessages),
                    ),
                )
            }
            asyncApiDocument.channels?.forEach { (channelKey, channelInterface) ->
                val externalChannel =
                    channelInterface is ChannelInterface.ChannelReference &&
                        channelInterface.reference.ref.parseReference().isExternal
                val channel =
                    when (channelInterface) {
                        is ChannelInterface.ChannelInline -> channelInterface.channel
                        is ChannelInterface.ChannelReference -> channelInterface.reference.model as? Channel
                    } ?: return@forEach
                channel.messages?.forEach { (messageKey, messageInterface) ->
                    add(
                        CollectedMessage(
                            messageKey = messageKey,
                            message = messageInterface,
                            origin = "channels['$channelKey'].messages['$messageKey']",
                            payloadIdentity =
                                messagePayloadIdentity(
                                    messageInterface = messageInterface,
                                    externalContext = externalChannel,
                                    componentMessages = componentMessages,
                                ),
                        ),
                    )
                }
            }
        }

    private data class CollectedMessage(
        val messageKey: String,
        val message: MessageInterface,
        val origin: String,
        val payloadIdentity: ContractDeclarationIdentity?,
    )

    private fun messagePayloadIdentity(
        messageInterface: MessageInterface,
        externalContext: Boolean,
        componentMessages: Map<String, MessageInterface>,
    ): ContractDeclarationIdentity? =
        when (messageInterface) {
            is MessageInterface.MessageInline -> null
            is MessageInterface.MessageReference -> {
                val reference = messageInterface.reference
                val componentMessageName = reference.localComponentName("messages")
                if (!externalContext && componentMessageName != null) {
                    componentMessagePayloadIdentity(componentMessageName, componentMessages)
                } else {
                    ContractDeclarationIdentity.ReferencedMessagePayload(reference.sourceId, reference.ref)
                }
            }
        }

    private fun componentMessagePayloadIdentity(
        messageName: String,
        componentMessages: Map<String, MessageInterface>,
        visitedNames: Set<String> = emptySet(),
    ): ContractDeclarationIdentity {
        val immediateIdentity = ContractDeclarationIdentity.ComponentMessagePayload(messageName)
        if (messageName in visitedNames) return immediateIdentity
        val messageReference =
            (componentMessages[messageName] as? MessageInterface.MessageReference)?.reference
                ?: return immediateIdentity
        val referencedComponentName = messageReference.localComponentName("messages")
            ?: return ContractDeclarationIdentity.ReferencedMessagePayload(
                messageReference.sourceId,
                messageReference.ref,
            )
        return componentMessagePayloadIdentity(
            messageName = referencedComponentName,
            componentMessages = componentMessages,
            visitedNames = visitedNames + messageName,
        )
    }

    private fun payloadDeclarationIdentity(
        collectedMessage: CollectedMessage,
        message: Message,
    ): ContractDeclarationIdentity? {
        val payloadReference = (message.payload as? SchemaInterface.SchemaReference)?.reference
            ?: return collectedMessage.payloadIdentity
        val componentSchemaName = payloadReference.localComponentName("schemas")
        return if (
            componentSchemaName != null &&
            collectedMessage.payloadIdentity !is ContractDeclarationIdentity.ReferencedMessagePayload
        ) {
            ContractDeclarationIdentity.ComponentSchema(componentSchemaName)
        } else {
            ContractDeclarationIdentity.ReferenceTarget(payloadReference.sourceId, payloadReference.ref)
        }
    }

    private fun Reference.localComponentName(category: String): String? {
        val parsedReference = runCatching { ref.parseReference() }.getOrNull() ?: return null
        if (parsedReference.isExternal) return null
        val segments = parsedReference.pointerSegments()
        return segments
            .takeIf { it.size == 3 && it[0] == "components" && it[1] == category }
            ?.get(2)
            ?.takeIf(String::isNotBlank)
    }

    private fun payloadOrigin(
        messageOrigin: String,
        message: Message,
    ): String =
        when (val payload = message.payload) {
            is SchemaInterface.SchemaReference ->
                "$messageOrigin.payload (reference '${payload.reference.ref}')"
            else -> "$messageOrigin.payload"
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

    private class ContractDeclarationRegistry {
        val asyncApiSchemas: MutableMap<String, Schema> = mutableMapOf()
        val multiFormatSchemas: MutableMap<String, MultiFormatSchema> = mutableMapOf()
        val booleanSchemas: MutableMap<String, Boolean> = mutableMapOf()
        private val owners = mutableMapOf<String, ContractDeclarationOwner>()

        fun register(
            generatedName: String,
            declaration: ContractDeclaration,
            origin: String,
        ) {
            val existingOwner = owners[generatedName]
            if (existingOwner != null) {
                if (!existingOwner.declaration.hasSameIdentity(declaration)) {
                    throw SchemaNameCollision(
                        generatedName = generatedName,
                        firstOrigin = existingOwner.origin,
                        conflictingOrigin = origin,
                    )
                }
                return
            }

            owners[generatedName] = ContractDeclarationOwner(declaration, origin)
            when (declaration) {
                is ContractDeclaration.AsyncApi -> asyncApiSchemas[generatedName] = declaration.schema
                is ContractDeclaration.MultiFormat -> multiFormatSchemas[generatedName] = declaration.schema
                is ContractDeclaration.BooleanSchema -> booleanSchemas[generatedName] = declaration.value
            }
        }
    }

    private data class ContractDeclarationOwner(
        val declaration: ContractDeclaration,
        val origin: String,
    )

    private sealed interface ContractDeclaration {
        fun hasSameIdentity(other: ContractDeclaration): Boolean

        class AsyncApi(
            val schema: Schema,
            val identity: ContractDeclarationIdentity?,
        ) : ContractDeclaration {
            override fun hasSameIdentity(other: ContractDeclaration): Boolean =
                other is AsyncApi &&
                    (identity != null && identity == other.identity || schema === other.schema)
        }

        class MultiFormat(
            val schema: MultiFormatSchema,
            val identity: ContractDeclarationIdentity?,
        ) : ContractDeclaration {
            override fun hasSameIdentity(other: ContractDeclaration): Boolean =
                other is MultiFormat &&
                    (identity != null && identity == other.identity || schema === other.schema)
        }

        class BooleanSchema(
            val value: Boolean,
        ) : ContractDeclaration {
            override fun hasSameIdentity(other: ContractDeclaration): Boolean =
                other is BooleanSchema && value == other.value
        }
    }

    private sealed interface ContractDeclarationIdentity {
        data class ComponentSchema(
            val name: String,
        ) : ContractDeclarationIdentity

        data class ComponentMessagePayload(
            val name: String,
        ) : ContractDeclarationIdentity

        data class ReferencedMessagePayload(
            val sourceId: String?,
            val ref: String,
        ) : ContractDeclarationIdentity

        data class ReferenceTarget(
            val sourceId: String?,
            val ref: String,
        ) : ContractDeclarationIdentity
    }
}
