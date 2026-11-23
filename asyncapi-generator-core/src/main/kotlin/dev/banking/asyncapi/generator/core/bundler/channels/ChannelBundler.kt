package dev.banking.asyncapi.generator.core.bundler.channels

import dev.banking.asyncapi.generator.core.bundler.BundlingScope
import dev.banking.asyncapi.generator.core.bundler.BundlingWalk
import dev.banking.asyncapi.generator.core.model.channels.Channel

/**
 * Bundles the members of a channel.
 *
 * @param walk bundles the members of the channel
 */
internal class ChannelBundler(
    private val walk: BundlingWalk,
) {
    /** Returns [channel] with its members bundled. */
    fun bundle(
        channel: Channel,
        scope: BundlingScope,
    ): Channel =
        channel.copy(
            messages = walk.members(channel.messages, walk.message, scope),
            parameters = walk.members(channel.parameters, walk.parameter, scope),
            tags = walk.members(channel.tags, walk.tag, scope),
            externalDocs = walk.member(channel.externalDocs, walk.externalDoc, scope),
            bindings = walk.members(channel.bindings, walk.binding, scope),
        )
}
