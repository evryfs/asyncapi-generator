package dev.banking.asyncapi.generator.core.bundler.messages

import dev.banking.asyncapi.generator.core.bundler.BundlingContext
import dev.banking.asyncapi.generator.core.bundler.ReferenceBundler
import dev.banking.asyncapi.generator.core.bundler.bindings.BindingBundler
import dev.banking.asyncapi.generator.core.bundler.correlations.CorrelationIdBundler
import dev.banking.asyncapi.generator.core.bundler.externaldocs.ExternalDocsBundler
import dev.banking.asyncapi.generator.core.bundler.schemas.SchemaBundler
import dev.banking.asyncapi.generator.core.bundler.tags.TagBundler
import dev.banking.asyncapi.generator.core.model.messages.MessageTrait
import dev.banking.asyncapi.generator.core.model.messages.MessageTraitInterface

/**
 * Bundles message trait objects and references.
 *
 * Expected behavior is covered by:
 * - `MessageTraitBundlerTest`
 */
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
        bundleMap(traits, BundlingContext.from(visited))

    fun bundleMap(
        traits: Map<String, MessageTraitInterface>?,
        context: BundlingContext,
    ): Map<String, MessageTraitInterface>? =
        traits?.mapValues { (_, trait) -> bundle(trait, context) }

    fun bundleList(
        traits: List<MessageTraitInterface>?,
        visited: Set<String>
    ): List<MessageTraitInterface>? =
        bundleList(traits, BundlingContext.from(visited))

    fun bundleList(
        traits: List<MessageTraitInterface>?,
        context: BundlingContext,
    ): List<MessageTraitInterface>? =
        traits?.map { trait -> bundle(trait, context) }

    fun bundle(traitInterface: MessageTraitInterface, visited: Set<String>): MessageTraitInterface =
        bundle(traitInterface, BundlingContext.from(visited))

    fun bundle(traitInterface: MessageTraitInterface, context: BundlingContext): MessageTraitInterface =
        when (traitInterface) {
            is MessageTraitInterface.InlineMessageTrait ->
                MessageTraitInterface.InlineMessageTrait(
                    bundleTrait(traitInterface.trait, context)
                )
            is MessageTraitInterface.ReferenceMessageTrait -> {
                ReferenceBundler.bundleReferencedModel<MessageTrait>(
                    reference = traitInterface.reference,
                    context = context,
                ) { trait, nextContext ->
                    bundleTrait(trait, nextContext)
                }
                traitInterface
            }
        }

    private fun bundleTrait(trait: MessageTrait, context: BundlingContext): MessageTrait {
        val bundledHeaders =  trait.headers?.let { schemaBundler.bundle(it, context) }
        val bundledCorrelationId = trait.correlationId?.let { correlationIdBundler.bundle(it, context) }
        val bundledTags = tagBundler.bundleList(trait.tags, context)
        val bundledExternalDocs = trait.externalDocs?.let { externalDocsBundler.bundle(it, context) }
        val bundledBindings = bindingBundler.bundleMap(trait.bindings, context)
        return trait.copy(
            headers = bundledHeaders,
            correlationId = bundledCorrelationId,
            tags = bundledTags,
            externalDocs = bundledExternalDocs,
            bindings = bundledBindings
        )
    }
}
