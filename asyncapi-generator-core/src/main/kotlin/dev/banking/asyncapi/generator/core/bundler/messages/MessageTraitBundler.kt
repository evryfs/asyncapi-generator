package dev.banking.asyncapi.generator.core.bundler.messages

import dev.banking.asyncapi.generator.core.bundler.BundlingScope
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
 */
internal class MessageTraitBundler {
    private val schemaBundler = SchemaBundler()
    private val correlationIdBundler = CorrelationIdBundler()
    private val tagBundler = TagBundler()
    private val externalDocsBundler = ExternalDocsBundler()
    private val bindingBundler = BindingBundler()

    fun bundleMap(
        traits: Map<String, MessageTraitInterface>?,
        scope: BundlingScope,
    ): Map<String, MessageTraitInterface>? = traits?.mapValues { (_, trait) -> bundle(trait, scope) }

    fun bundleList(
        traits: List<MessageTraitInterface>?,
        scope: BundlingScope,
    ): List<MessageTraitInterface>? = traits?.map { trait -> bundle(trait, scope) }

    fun bundle(
        traitInterface: MessageTraitInterface,
        scope: BundlingScope,
    ): MessageTraitInterface =
        when (traitInterface) {
            is MessageTraitInterface.MessageTraitInline ->
                MessageTraitInterface.MessageTraitInline(
                    bundleTrait(traitInterface.messageTrait, scope),
                )
            is MessageTraitInterface.MessageTraitReference -> {
                traitInterface.copy(
                    reference =
                        ReferenceBundler.bundleReferencedModel<MessageTrait>(
                            reference = traitInterface.reference,
                            scope = scope,
                        ) { trait, nextScope ->
                            bundleTrait(trait, nextScope)
                        },
                )
            }
        }

    private fun bundleTrait(
        trait: MessageTrait,
        scope: BundlingScope,
    ): MessageTrait {
        val bundledHeaders = trait.headers?.let { schemaBundler.bundle(it, scope) }
        val bundledCorrelationId = trait.correlationId?.let { correlationIdBundler.bundle(it, scope) }
        val bundledTags = tagBundler.bundleList(trait.tags, scope)
        val bundledExternalDocs = trait.externalDocs?.let { externalDocsBundler.bundle(it, scope) }
        val bundledBindings = bindingBundler.bundleMap(trait.bindings, scope)
        return trait.copy(
            headers = bundledHeaders,
            correlationId = bundledCorrelationId,
            tags = bundledTags,
            externalDocs = bundledExternalDocs,
            bindings = bundledBindings,
        )
    }
}
