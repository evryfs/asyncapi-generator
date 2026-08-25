package dev.banking.asyncapi.generator.core.bundler.operations

import dev.banking.asyncapi.generator.core.bundler.BundlingContext
import dev.banking.asyncapi.generator.core.bundler.ReferenceBundler
import dev.banking.asyncapi.generator.core.bundler.bindings.BindingBundler
import dev.banking.asyncapi.generator.core.bundler.externaldocs.ExternalDocsBundler
import dev.banking.asyncapi.generator.core.bundler.security.SecuritySchemeBundler
import dev.banking.asyncapi.generator.core.bundler.tags.TagBundler
import dev.banking.asyncapi.generator.core.model.operations.OperationTrait
import dev.banking.asyncapi.generator.core.model.operations.OperationTraitInterface

/**
 * Bundles operation trait objects and references.
 */
internal class OperationTraitBundler {

    private val tagBundler = TagBundler()
    private val externalDocsBundler = ExternalDocsBundler()
    private val securitySchemeBundler = SecuritySchemeBundler()
    private val bindingBundler = BindingBundler()
    fun bundleMap(
        traits: Map<String, OperationTraitInterface>?,
        context: BundlingContext,
    ): Map<String, OperationTraitInterface>? =
        traits?.mapValues { (_, trait) -> bundle(trait, context) }
    fun bundleList(
        traits: List<OperationTraitInterface>?,
        context: BundlingContext,
    ): List<OperationTraitInterface>? =
        traits?.map { trait -> bundle(trait, context) }
    fun bundle(traitInterface: OperationTraitInterface, context: BundlingContext): OperationTraitInterface =
        when (traitInterface) {
            is OperationTraitInterface.OperationTraitInline ->
                OperationTraitInterface.OperationTraitInline(
                    bundleTrait(traitInterface.operationTrait, context)
                )
            is OperationTraitInterface.OperationTraitReference -> {
                ReferenceBundler.bundleReferencedModel<OperationTrait>(
                    reference = traitInterface.reference,
                    context = context,
                ) { trait, nextContext ->
                    bundleTrait(trait, nextContext)
                }
                traitInterface
            }
        }

    private fun bundleTrait(trait: OperationTrait, context: BundlingContext): OperationTrait {
        val bundledTags = tagBundler.bundleList(trait.tags, context)
        val bundledExternalDocs = trait.externalDocs?.let { externalDocsBundler.bundle(it, context) }
        val bundledSecurity = securitySchemeBundler.bundleList(trait.security, context)
        val bundledBindings = bindingBundler.bundleMap(trait.bindings, context)
        return trait.copy(
            tags = bundledTags,
            externalDocs = bundledExternalDocs,
            security = bundledSecurity,
            bindings = bundledBindings
        )
    }
}
