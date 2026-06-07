package dev.banking.asyncapi.generator.core.bundler.channels

import dev.banking.asyncapi.generator.core.bundler.BundlingContext
import dev.banking.asyncapi.generator.core.bundler.ReferenceBundler
import dev.banking.asyncapi.generator.core.bundler.bindings.BindingBundler
import dev.banking.asyncapi.generator.core.bundler.externaldocs.ExternalDocsBundler
import dev.banking.asyncapi.generator.core.bundler.messages.MessagesBundler
import dev.banking.asyncapi.generator.core.bundler.parameters.ParameterBundler
import dev.banking.asyncapi.generator.core.bundler.tags.TagBundler
import dev.banking.asyncapi.generator.core.model.channels.Channel
import dev.banking.asyncapi.generator.core.model.channels.ChannelInterface

/**
 * Bundles channel objects and references.
 *
 * Expected behavior is covered by:
 * - `ChannelBundlerTest`
 */
class ChannelBundler {

    private val messagesBundler: MessagesBundler = MessagesBundler()
    private val parameterBundler: ParameterBundler = ParameterBundler()
    private val tagBundler: TagBundler = TagBundler()
    private val externalDocsBundler: ExternalDocsBundler = ExternalDocsBundler()
    private val bindingBundler = BindingBundler()

    fun bundleMap(channels: Map<String, ChannelInterface>?, visited: Set<String>): Map<String, ChannelInterface>? =
        bundleMap(channels, BundlingContext.from(visited))

    fun bundleMap(channels: Map<String, ChannelInterface>?, context: BundlingContext): Map<String, ChannelInterface>? {
        if (channels == null) return null
        return channels.mapValues { (_, channelInterface) ->
            bundle(channelInterface, context)
        }
    }

    fun bundle(channelInterface: ChannelInterface, visited: Set<String>): ChannelInterface =
        bundle(channelInterface, BundlingContext.from(visited))

    fun bundle(channelInterface: ChannelInterface, context: BundlingContext): ChannelInterface =
        when (channelInterface) {
            is ChannelInterface.ChannelInline ->
                ChannelInterface.ChannelInline(
                    bundleChannel(channelInterface.channel, context)
                )
            is ChannelInterface.ChannelReference -> {
                ReferenceBundler.bundleReferencedModel<Channel>(
                    reference = channelInterface.reference,
                    context = context,
                ) { channel, nextContext ->
                    bundleChannel(channel, nextContext)
                }
                channelInterface
            }
        }

    fun bundleChannel(channel: Channel, visited: Set<String>): Channel =
        bundleChannel(channel, BundlingContext.from(visited))

    fun bundleChannel(channel: Channel, context: BundlingContext): Channel {
        val bundledMessages = messagesBundler.bundleMap(channel.messages, context)
        val bundledParameters = parameterBundler.bundleMap(channel.parameters, context)
        val bundledTags = tagBundler.bundleList(channel.tags, context)
        val bundledExternalDocs = channel.externalDocs?.let { externalDocsBundler.bundle(it, context)}
        val bundledBindings = bindingBundler.bundleMap(channel.bindings, context)
        return channel.copy(
            messages = bundledMessages,
            parameters = bundledParameters,
            tags = bundledTags,
            externalDocs = bundledExternalDocs,
            bindings = bundledBindings
        )
    }
}
