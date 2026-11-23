package dev.banking.asyncapi.generator.core.bundler.messages

import dev.banking.asyncapi.generator.core.bundler.BundlingScope
import dev.banking.asyncapi.generator.core.bundler.BundlingWalk
import dev.banking.asyncapi.generator.core.model.messages.Message

/**
 * Bundles the members of a message.
 *
 * @param walk bundles the members of the message
 */
internal class MessageBundler(
    private val walk: BundlingWalk,
) {
    /** Returns [message] with its members bundled. */
    fun bundle(
        message: Message,
        scope: BundlingScope,
    ): Message =
        message.copy(
            headers = message.headers?.let { walk.schemas.bundle(it, scope) },
            payload = message.payload?.let { walk.schemas.bundle(it, scope) },
            correlationId = walk.member(message.correlationId, walk.correlationId, scope),
            tags = walk.members(message.tags, walk.tag, scope),
            externalDocs = walk.member(message.externalDocs, walk.externalDoc, scope),
            bindings = walk.members(message.bindings, walk.binding, scope),
            traits = walk.members(message.traits, walk.messageTrait, scope),
        )
}
