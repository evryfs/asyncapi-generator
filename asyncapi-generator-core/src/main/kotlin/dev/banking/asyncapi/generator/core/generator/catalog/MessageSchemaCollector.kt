package dev.banking.asyncapi.generator.core.generator.catalog

import dev.banking.asyncapi.generator.core.generator.analyzer.MessagePayloadResolver
import dev.banking.asyncapi.generator.core.generator.analyzer.ResolvedMessagePayload
import dev.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument
import dev.banking.asyncapi.generator.core.model.channels.ChannelInterface
import dev.banking.asyncapi.generator.core.model.components.Component
import dev.banking.asyncapi.generator.core.model.messages.Message
import dev.banking.asyncapi.generator.core.model.messages.MessageInterface
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.references.parseReference
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

/**
 * Declares the payload schema of every message in `components.messages` and in
 * the channels, then hands each message to [KafkaKeySchemaCollector].
 */
internal object MessageSchemaCollector {
    /** Declares the payloads and Kafka keys of the messages of [asyncApiDocument]. */
    fun collect(
        asyncApiDocument: AsyncApiDocument,
        declarations: SchemaDeclarations,
    ) {
        collectMessages(asyncApiDocument, asyncApiDocument.components).forEach { collectedMessage ->
            val message = MessagePayloadResolver.resolveMessage(collectedMessage.message) ?: return@forEach
            declarePayload(collectedMessage, message, declarations)
            KafkaKeySchemaCollector.collect(collectedMessage, message, declarations)
        }
    }

    private fun declarePayload(
        collectedMessage: CollectedMessage,
        message: Message,
        declarations: SchemaDeclarations,
    ) {
        val identity = payloadDeclarationIdentity(collectedMessage, message)
        val origin = payloadOrigin(collectedMessage.origin, message)
        when (val payload = MessagePayloadResolver.resolvePayload(message, collectedMessage.messageKey)) {
            is ResolvedMessagePayload.AsyncApi ->
                declarations.declareSchema(payload.typeName, payload.schema, identity, origin)
            is ResolvedMessagePayload.MultiFormat ->
                declarations.declareMultiFormat(payload.typeName, payload.schema, identity, origin)
            is ResolvedMessagePayload.Boolean -> declarations.declareBoolean(payload.typeName, payload.value, origin)
            null -> Unit
        }
    }

    private fun collectMessages(
        asyncApiDocument: AsyncApiDocument,
        component: Component?,
    ): List<CollectedMessage> {
        val componentMessages = component?.messages.orEmpty()
        val fromComponents =
            componentMessages.map { (messageKey, messageInterface) ->
                CollectedMessage(
                    messageKey = messageKey,
                    message = messageInterface,
                    origin = "components.messages['$messageKey']",
                    payloadIdentity = componentMessagePayloadIdentity(messageKey, componentMessages),
                )
            }
        val fromChannels =
            asyncApiDocument.channels.orEmpty().flatMap { (channelKey, channelInterface) ->
                channelMessages(channelKey, channelInterface, componentMessages)
            }
        return fromComponents + fromChannels
    }

    private fun channelMessages(
        channelKey: String,
        channelInterface: ChannelInterface,
        componentMessages: Map<String, MessageInterface>,
    ): List<CollectedMessage> {
        val externalChannel =
            channelInterface is ChannelInterface.ChannelReference &&
                channelInterface.reference.ref
                    .parseReference()
                    .isExternal
        val channel = channelInterface.resolvedOrNull() ?: return emptyList()
        return channel.messages.orEmpty().map { (messageKey, messageInterface) ->
            CollectedMessage(
                messageKey = messageKey,
                message = messageInterface,
                origin = "channels['$channelKey'].messages['$messageKey']",
                payloadIdentity = messagePayloadIdentity(messageInterface, externalChannel, componentMessages),
            )
        }
    }

    /**
     * A message found in `components.messages` or in a channel.
     *
     * @param messageKey the key of the message in its map.
     * @param message the message or a reference to it.
     * @param origin where the message was found, for collision messages.
     * @param payloadIdentity the identity of its payload when the message is a reference.
     */
    data class CollectedMessage(
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
        val messageReference =
            (componentMessages[messageName] as? MessageInterface.MessageReference)
                ?.reference
                ?.takeIf { messageName !in visitedNames }
        val referencedComponentName = messageReference?.localComponentName("messages")
        return when {
            messageReference == null -> ContractDeclarationIdentity.ComponentMessagePayload(messageName)
            referencedComponentName == null ->
                ContractDeclarationIdentity.ReferencedMessagePayload(messageReference.sourceId, messageReference.ref)
            else ->
                componentMessagePayloadIdentity(
                    messageName = referencedComponentName,
                    componentMessages = componentMessages,
                    visitedNames = visitedNames + messageName,
                )
        }
    }

    private fun payloadDeclarationIdentity(
        collectedMessage: CollectedMessage,
        message: Message,
    ): ContractDeclarationIdentity? {
        val payloadReference =
            (message.payload as? SchemaInterface.SchemaReference)?.reference
                ?: return collectedMessage.payloadIdentity
        return referencedSchemaIdentity(payloadReference, collectedMessage)
    }

    /**
     * The identity of a schema a message references: its component name when the
     * message is read from the root document, otherwise the reference target.
     */
    fun referencedSchemaIdentity(
        reference: Reference,
        collectedMessage: CollectedMessage,
    ): ContractDeclarationIdentity {
        val componentSchemaName = reference.localComponentName("schemas")
        return if (
            componentSchemaName != null &&
            collectedMessage.payloadIdentity !is ContractDeclarationIdentity.ReferencedMessagePayload
        ) {
            ContractDeclarationIdentity.ComponentSchema(componentSchemaName)
        } else {
            ContractDeclarationIdentity.ReferenceTarget(reference.sourceId, reference.ref)
        }
    }

    /** The component name when this reference points at `#/components/<category>/<name>` in the same document. */
    private fun Reference.localComponentName(category: String): String? =
        runCatching { ref.parseReference() }
            .getOrNull()
            ?.takeUnless { it.isExternal }
            ?.pointerSegments()
            ?.takeIf { it.size == COMPONENT_POINTER_SEGMENTS && it[0] == "components" && it[1] == category }
            ?.get(2)
            ?.takeIf(String::isNotBlank)

    private const val COMPONENT_POINTER_SEGMENTS = 3

    private fun payloadOrigin(
        messageOrigin: String,
        message: Message,
    ): String =
        when (val payload = message.payload) {
            is SchemaInterface.SchemaReference ->
                "$messageOrigin.payload (reference '${payload.reference.ref}')"
            else -> "$messageOrigin.payload"
        }
}
