package dev.banking.asyncapi.generator.core.bundler.messages

import dev.banking.asyncapi.generator.core.bundler.BundlingScope
import dev.banking.asyncapi.generator.core.bundler.ReferenceBundler
import dev.banking.asyncapi.generator.core.bundler.bindings.BindingBundler
import dev.banking.asyncapi.generator.core.bundler.correlations.CorrelationIdBundler
import dev.banking.asyncapi.generator.core.bundler.externaldocs.ExternalDocsBundler
import dev.banking.asyncapi.generator.core.bundler.schemas.SchemaBundler
import dev.banking.asyncapi.generator.core.bundler.tags.TagBundler
import dev.banking.asyncapi.generator.core.model.messages.Message
import dev.banking.asyncapi.generator.core.model.messages.MessageInterface
import dev.banking.asyncapi.generator.core.model.tags.TagInterface

/**
 * Bundles message objects and references.
 */
internal class MessagesBundler {
    private val schemaBundler = SchemaBundler()
    private val correlationIdBundler = CorrelationIdBundler()
    private val tagBundler = TagBundler()
    private val externalDocsBundler = ExternalDocsBundler()
    private val bindingBundler = BindingBundler()
    private val messageTraitBundler = MessageTraitBundler()

    fun bundleMap(
        messages: Map<String, MessageInterface>?,
        scope: BundlingScope,
    ): Map<String, MessageInterface>? =
        messages?.mapValues { (_, message) ->
            bundle(message, scope)
        }

    fun bundle(
        message: MessageInterface,
        scope: BundlingScope,
    ): MessageInterface =
        when (message) {
            is MessageInterface.MessageInline -> {
                MessageInterface.MessageInline(
                    bundleMessage(message.message, scope),
                )
            }
            is MessageInterface.MessageReference -> {
                message.copy(
                    reference =
                        ReferenceBundler.bundleReferencedModel<Message>(
                            reference = message.reference,
                            scope = scope,
                        ) { messageModel, nextScope ->
                            bundleMessage(messageModel, nextScope)
                        },
                )
            }
        }

    private fun bundleMessage(
        message: Message,
        scope: BundlingScope,
    ): Message {
        val bundledHeaders = message.headers?.let { schemaBundler.bundle(it, scope) }
        val bundledPayload = message.payload?.let { schemaBundler.bundle(it, scope) }
        val bundledCorrelationId = message.correlationId?.let { correlationIdBundler.bundle(it, scope) }
        val bundledTags: List<TagInterface>? = tagBundler.bundleList(message.tags, scope)
        val bundledExternalDocs = message.externalDocs?.let { externalDocsBundler.bundle(it, scope) }
        val bundledBindings = bindingBundler.bundleMap(message.bindings, scope)
        val bundledTraits = messageTraitBundler.bundleList(message.traits, scope)
        val bundledExamples = message.examples
        return message.copy(
            headers = bundledHeaders,
            payload = bundledPayload,
            correlationId = bundledCorrelationId,
            tags = bundledTags,
            externalDocs = bundledExternalDocs,
            bindings = bundledBindings,
            examples = bundledExamples,
            traits = bundledTraits,
        )
    }
}
