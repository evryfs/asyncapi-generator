package dev.banking.asyncapi.generator.core.bundler.operations

import dev.banking.asyncapi.generator.core.bundler.BundlingScope
import dev.banking.asyncapi.generator.core.bundler.BundlingWalk
import dev.banking.asyncapi.generator.core.model.operations.Operation

/**
 * Bundles the members of an operation.
 *
 * An operation's `channel` and `messages` point into root channels, which are bundled themselves, so they
 * stay references.
 *
 * @param walk bundles the members of the operation
 */
internal class OperationBundler(
    private val walk: BundlingWalk,
) {
    /** Returns [operation] with its members bundled. */
    fun bundle(
        operation: Operation,
        scope: BundlingScope,
    ): Operation =
        operation.copy(
            bindings = walk.members(operation.bindings, walk.binding, scope),
            traits = walk.members(operation.traits, walk.operationTrait, scope),
            tags = walk.members(operation.tags, walk.tag, scope),
            externalDocs = walk.member(operation.externalDocs, walk.externalDoc, scope),
            reply = walk.member(operation.reply, walk.reply, scope),
            security = walk.members(operation.security, walk.securityScheme, scope),
        )
}
