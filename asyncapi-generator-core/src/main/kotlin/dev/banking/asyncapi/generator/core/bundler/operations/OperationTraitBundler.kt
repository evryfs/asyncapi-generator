package dev.banking.asyncapi.generator.core.bundler.operations

import dev.banking.asyncapi.generator.core.bundler.BundlingScope
import dev.banking.asyncapi.generator.core.bundler.BundlingWalk
import dev.banking.asyncapi.generator.core.model.operations.OperationTrait

/**
 * Bundles the members of an operation trait.
 *
 * @param walk bundles the members of the trait
 */
internal class OperationTraitBundler(
    private val walk: BundlingWalk,
) {
    /** Returns [trait] with its members bundled. */
    fun bundle(
        trait: OperationTrait,
        scope: BundlingScope,
    ): OperationTrait =
        trait.copy(
            tags = walk.members(trait.tags, walk.tag, scope),
            externalDocs = walk.member(trait.externalDocs, walk.externalDoc, scope),
            security = walk.members(trait.security, walk.securityScheme, scope),
            bindings = walk.members(trait.bindings, walk.binding, scope),
        )
}
