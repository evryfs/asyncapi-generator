package dev.banking.asyncapi.generator.core.bundler.messages

import dev.banking.asyncapi.generator.core.bundler.BundlingContext
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
 *
 * Expected behavior is covered by:
 * - `MessagesBundlerTest`
 */
class MessagesBundler {

    private val schemaBundler = SchemaBundler()
    private val correlationIdBundler = CorrelationIdBundler()
    private val tagBundler = TagBundler()
    private val externalDocsBundler = ExternalDocsBundler()
    private val bindingBundler = BindingBundler()
    private val messageTraitBundler = MessageTraitBundler()

    fun bundleMap(messages: Map<String, MessageInterface>?, visited: Set<String>): Map<String, MessageInterface>? =
        bundleMap(messages, BundlingContext.from(visited))

    fun bundleMap(messages: Map<String, MessageInterface>?, context: BundlingContext): Map<String, MessageInterface>? =
        messages?.mapValues { (_, message) ->
            bundle(message, context)
        }

    fun bundle(message: MessageInterface, visited: Set<String>): MessageInterface =
        bundle(message, BundlingContext.from(visited))

    fun bundle(message: MessageInterface, context: BundlingContext): MessageInterface =
        when (message) {
            is MessageInterface.MessageInline -> {
                MessageInterface.MessageInline(
                    bundleMessage(message.message, context)
                )
            }
            is MessageInterface.MessageReference -> {
                ReferenceBundler.bundleReferencedModel<Message>(
                    reference = message.reference,
                    context = context,
                ) { messageModel, nextContext ->
                    bundleMessage(messageModel, nextContext)
                }
                message
            }
        }

    private fun bundleMessage(message: Message, context: BundlingContext): Message {
        val bundledHeaders =  message.headers?.let { schemaBundler.bundle(it, context) }
        val bundledPayload = message.payload?.let { schemaBundler.bundle(it, context) }
        val bundledCorrelationId = message.correlationId?.let { correlationIdBundler.bundle(it, context) }
        val bundledTags: List<TagInterface>? = tagBundler.bundleList(message.tags, context)
        val bundledExternalDocs = message.externalDocs?.let { externalDocsBundler.bundle(it, context) }
        val bundledBindings = bindingBundler.bundleMap(message.bindings, context)
        val bundledTraits = messageTraitBundler.bundleList(message.traits, context)
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
