package com.tietoevry.banking.asyncapi.generator.core.bundler.operations

import com.tietoevry.banking.asyncapi.generator.core.bundler.bindings.BindingBundler
import com.tietoevry.banking.asyncapi.generator.core.bundler.externaldocs.ExternalDocsBundler
import com.tietoevry.banking.asyncapi.generator.core.bundler.security.SecuritySchemeBundler
import com.tietoevry.banking.asyncapi.generator.core.bundler.tags.TagBundler
import com.tietoevry.banking.asyncapi.generator.core.model.operations.OperationTrait
import com.tietoevry.banking.asyncapi.generator.core.model.operations.OperationTraitInterface

class OperationTraitBundler {

    private val tagBundler = TagBundler()
    private val externalDocsBundler = ExternalDocsBundler()
    private val securitySchemeBundler = SecuritySchemeBundler()
    private val bindingBundler = BindingBundler()

    fun bundleMap(
        traits: Map<String, OperationTraitInterface>?,
        visited: Set<String>
    ): Map<String, OperationTraitInterface>? =
        traits?.mapValues { (_, trait) -> bundle(trait, visited) }

    fun bundleList(
        traits: List<OperationTraitInterface>?,
        visited: Set<String>
    ): List<OperationTraitInterface>? =
        traits?.map { trait -> bundle(trait, visited) }

    fun bundle(traitInterface: OperationTraitInterface, visited: Set<String>): OperationTraitInterface {
        return when (traitInterface) {
            is OperationTraitInterface.OperationTraitInline ->
                OperationTraitInterface.OperationTraitInline(
                    bundleTrait(traitInterface.operationTrait, visited)
                )
            is OperationTraitInterface.OperationTraitReference -> {
                val ref = traitInterface.reference.ref
                if (visited.contains(ref)) {
                    traitInterface
                } else {
                    val model = traitInterface.reference.requireModel<OperationTrait>()
                    val newVisited = visited + ref
                    val bundled = bundleTrait(model, newVisited)
                    traitInterface.reference.model = bundled
                    traitInterface.reference.inline()
                    traitInterface
                }
            }
        }
    }

    private fun bundleTrait(trait: OperationTrait, visited: Set<String>): OperationTrait {
        val bundledTags = tagBundler.bundleList(trait.tags, visited)
        val bundledExternalDocs = trait.externalDocs?.let { externalDocsBundler.bundle(it, visited) }
        val bundledSecurity = securitySchemeBundler.bundleMap(trait.security, visited)
        val bundledBindings = bindingBundler.bundleMap(trait.bindings, visited)
        return trait.copy(
            tags = bundledTags,
            externalDocs = bundledExternalDocs,
            security = bundledSecurity,
            bindings = bundledBindings
        )
    }
}
