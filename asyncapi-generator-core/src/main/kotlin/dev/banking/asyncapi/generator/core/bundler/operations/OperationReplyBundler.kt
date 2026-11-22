package dev.banking.asyncapi.generator.core.bundler.operations

import dev.banking.asyncapi.generator.core.bundler.BundlingScope
import dev.banking.asyncapi.generator.core.bundler.BundlingWalk
import dev.banking.asyncapi.generator.core.model.operations.OperationReply

/**
 * Bundles the members of an operation reply.
 *
 * The reply's `channel` and `messages` point into root channels, which are bundled themselves; like an
 * operation's channel and messages they stay references. Only the address is bundled.
 *
 * @param walk bundles the members of the reply
 */
internal class OperationReplyBundler(
    private val walk: BundlingWalk,
) {
    /** Returns [reply] with its members bundled. */
    fun bundle(
        reply: OperationReply,
        scope: BundlingScope,
    ): OperationReply = reply.copy(address = walk.member(reply.address, walk.replyAddress, scope))
}
