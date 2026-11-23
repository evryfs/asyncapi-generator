package dev.banking.asyncapi.generator.core.bundler.operations

import dev.banking.asyncapi.generator.core.bundler.BundlingScope
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
        scope: BundlingScope,
    ): Map<String, OperationTraitInterface>? = traits?.mapValues { (_, trait) -> bundle(trait, scope) }

    fun bundleList(
        traits: List<OperationTraitInterface>?,
        scope: BundlingScope,
    ): List<OperationTraitInterface>? = traits?.map { trait -> bundle(trait, scope) }

    fun bundle(
        traitInterface: OperationTraitInterface,
        scope: BundlingScope,
    ): OperationTraitInterface =
        when (traitInterface) {
            is OperationTraitInterface.OperationTraitInline ->
                OperationTraitInterface.OperationTraitInline(
                    bundleTrait(traitInterface.operationTrait, scope),
                )
            is OperationTraitInterface.OperationTraitReference -> {
                traitInterface.copy(
                    reference =
                        ReferenceBundler.bundleReferencedModel<OperationTrait>(
                            reference = traitInterface.reference,
                            scope = scope,
                        ) { trait, nextScope ->
                            bundleTrait(trait, nextScope)
                        },
                )
            }
        }

    private fun bundleTrait(
        trait: OperationTrait,
        scope: BundlingScope,
    ): OperationTrait {
        val bundledTags = tagBundler.bundleList(trait.tags, scope)
        val bundledExternalDocs = trait.externalDocs?.let { externalDocsBundler.bundle(it, scope) }
        val bundledSecurity = securitySchemeBundler.bundleList(trait.security, scope)
        val bundledBindings = bindingBundler.bundleMap(trait.bindings, scope)
        return trait.copy(
            tags = bundledTags,
            externalDocs = bundledExternalDocs,
            security = bundledSecurity,
            bindings = bundledBindings,
        )
    }
}
