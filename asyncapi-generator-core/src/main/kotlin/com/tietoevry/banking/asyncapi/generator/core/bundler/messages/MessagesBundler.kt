package com.tietoevry.banking.asyncapi.generator.core.bundler.messages

import com.tietoevry.banking.asyncapi.generator.core.bundler.bindings.BindingBundler
import com.tietoevry.banking.asyncapi.generator.core.bundler.correlations.CorrelationIdBundler
import com.tietoevry.banking.asyncapi.generator.core.bundler.externaldocs.ExternalDocsBundler
import com.tietoevry.banking.asyncapi.generator.core.bundler.schemas.SchemaBundler
import com.tietoevry.banking.asyncapi.generator.core.bundler.tags.TagBundler
import com.tietoevry.banking.asyncapi.generator.core.model.messages.Message
import com.tietoevry.banking.asyncapi.generator.core.model.messages.MessageInterface
import com.tietoevry.banking.asyncapi.generator.core.model.tags.TagInterface

class MessagesBundler {

    private val schemaBundler: SchemaBundler = SchemaBundler()
    private val correlationIdBundler: CorrelationIdBundler = CorrelationIdBundler()
    private val tagBundler: TagBundler = TagBundler()
    private val externalDocsBundler: ExternalDocsBundler = ExternalDocsBundler()
    private val bindingBundler = BindingBundler()
    private val messageTraitBundler = MessageTraitBundler()

    fun bundleMap(messages: Map<String, MessageInterface>?, visited: Set<String>): Map<String, MessageInterface>? =
        messages?.mapValues { (_, message) ->
            bundleMessageInterface(message, visited)
        }

    private fun bundleMessageInterface(message: MessageInterface, visited: Set<String>): MessageInterface =
        when (message) {
            is MessageInterface.MessageInline -> {
                MessageInterface.MessageInline(
                    bundleMessage(message.message, visited)
                )
            }
            is MessageInterface.MessageReference -> {
                val ref = message.reference.ref
                if (visited.contains(ref)) {
                    message
                } else {
                    val model = message.reference.requireModel<Message>()
                    val newVisited = visited + ref
                    val bundled = bundleMessage(model, newVisited)
                    message.reference.model = bundled
                    message.reference.inline()
                    message

                }
            }
        }

    private fun bundleMessage(message: Message, visited: Set<String>): Message {
        val bundledHeaders = schemaBundler.bundleMap(message.headers, visited)
        val bundledPayload = schemaBundler.bundle(message.payload, visited)
        val bundledCorrelationId = message.correlationId?.let { correlationIdBundler.bundle(it, visited) }
        val bundledTags: List<TagInterface>? = tagBundler.bundleList(message.tags, visited)
        val bundledExternalDocs = message.externalDocs?.let { externalDocsBundler.bundle(it, visited) }
        val bundledBindings = bindingBundler.bundleMap(message.bindings, visited)
        val bundledTraits = messageTraitBundler.bundleList(message.traits, visited)
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
