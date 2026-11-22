package dev.banking.asyncapi.generator.core.bundler.messages

import dev.banking.asyncapi.generator.core.bundler.BundlingScope
import dev.banking.asyncapi.generator.core.bundler.BundlingWalk
import dev.banking.asyncapi.generator.core.model.messages.MessageTrait

/**
 * Bundles the members of a message trait.
 *
 * @param walk bundles the members of the trait
 */
internal class MessageTraitBundler(
    private val walk: BundlingWalk,
) {
    /** Returns [trait] with its members bundled. */
    fun bundle(
        trait: MessageTrait,
        scope: BundlingScope,
    ): MessageTrait =
        trait.copy(
            headers = trait.headers?.let { walk.schemas.bundle(it, scope) },
            correlationId = walk.member(trait.correlationId, walk.correlationId, scope),
            tags = walk.members(trait.tags, walk.tag, scope),
            externalDocs = walk.member(trait.externalDocs, walk.externalDoc, scope),
            bindings = walk.members(trait.bindings, walk.binding, scope),
        )
}
