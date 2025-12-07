package dev.banking.asyncapi.generator.core.bundler.messages

import dev.banking.asyncapi.generator.core.bundler.bindings.BindingBundler
import dev.banking.asyncapi.generator.core.bundler.correlations.CorrelationIdBundler
import dev.banking.asyncapi.generator.core.bundler.externaldocs.ExternalDocsBundler
import dev.banking.asyncapi.generator.core.bundler.schemas.SchemaBundler
import dev.banking.asyncapi.generator.core.bundler.tags.TagBundler
import dev.banking.asyncapi.generator.core.model.messages.MessageTrait
import dev.banking.asyncapi.generator.core.model.messages.MessageTraitInterface

class MessageTraitBundler {

    private val schemaBundler = SchemaBundler()
    private val correlationIdBundler = CorrelationIdBundler()
    private val tagBundler = TagBundler()
    private val externalDocsBundler = ExternalDocsBundler()
    private val bindingBundler = BindingBundler()

    fun bundleMap(
        traits: Map<String, MessageTraitInterface>?,
        visited: Set<String>
    ): Map<String, MessageTraitInterface>? =
        traits?.mapValues { (_, trait) -> bundle(trait, visited) }

    fun bundleList(
        traits: List<MessageTraitInterface>?,
        visited: Set<String>
    ): List<MessageTraitInterface>? =
        traits?.map { trait -> bundle(trait, visited) }

    fun bundle(traitInterface: MessageTraitInterface, visited: Set<String>): MessageTraitInterface {
        return when (traitInterface) {
            is MessageTraitInterface.InlineMessageTrait ->
                MessageTraitInterface.InlineMessageTrait(
                    bundleTrait(traitInterface.trait, visited)
                )
            is MessageTraitInterface.ReferenceMessageTrait -> {
                val ref = traitInterface.reference.ref
                if (visited.contains(ref)) {
                    traitInterface
                } else {
                    val model = traitInterface.reference.requireModel<MessageTrait>()
                    val newVisited = visited + ref
                    val bundled = bundleTrait(model, newVisited)
                    traitInterface.reference.model = bundled
                    traitInterface.reference.inline()
                    traitInterface
                }
            }
        }
    }

    private fun bundleTrait(trait: MessageTrait, visited: Set<String>): MessageTrait {
        val bundledHeaders = schemaBundler.bundleMap(trait.headers, visited)
        val bundledCorrelationId = trait.correlationId?.let { correlationIdBundler.bundle(it, visited) }
        val bundledTags = tagBundler.bundleList(trait.tags, visited)
        val bundledExternalDocs = trait.externalDocs?.let { externalDocsBundler.bundle(it, visited) }
        val bundledBindings = bindingBundler.bundleMap(trait.bindings, visited)
        return trait.copy(
            headers = bundledHeaders,
            correlationId = bundledCorrelationId,
            tags = bundledTags,
            externalDocs = bundledExternalDocs,
            bindings = bundledBindings
        )
    }
}
