package dev.banking.asyncapi.generator.core.bundler.channels

import dev.banking.asyncapi.generator.core.bundler.BundlingScope
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
 */
internal class ChannelBundler {
    private val messagesBundler: MessagesBundler = MessagesBundler()
    private val parameterBundler: ParameterBundler = ParameterBundler()
    private val tagBundler: TagBundler = TagBundler()
    private val externalDocsBundler: ExternalDocsBundler = ExternalDocsBundler()
    private val bindingBundler = BindingBundler()

    fun bundleMap(
        channels: Map<String, ChannelInterface>?,
        scope: BundlingScope,
    ): Map<String, ChannelInterface>? {
        if (channels == null) return null
        return channels.mapValues { (_, channelInterface) ->
            bundle(channelInterface, scope)
        }
    }

    fun bundle(
        channelInterface: ChannelInterface,
        scope: BundlingScope,
    ): ChannelInterface =
        when (channelInterface) {
            is ChannelInterface.ChannelInline ->
                ChannelInterface.ChannelInline(
                    bundleChannel(channelInterface.channel, scope),
                )
            is ChannelInterface.ChannelReference -> {
                channelInterface.copy(
                    reference =
                        ReferenceBundler.bundleReferencedModel<Channel>(
                            reference = channelInterface.reference,
                            scope = scope,
                        ) { channel, nextScope ->
                            bundleChannel(channel, nextScope)
                        },
                )
            }
        }

    fun bundleChannel(
        channel: Channel,
        scope: BundlingScope,
    ): Channel {
        val bundledMessages = messagesBundler.bundleMap(channel.messages, scope)
        val bundledParameters = parameterBundler.bundleMap(channel.parameters, scope)
        val bundledTags = tagBundler.bundleList(channel.tags, scope)
        val bundledExternalDocs = channel.externalDocs?.let { externalDocsBundler.bundle(it, scope) }
        val bundledBindings = bindingBundler.bundleMap(channel.bindings, scope)
        return channel.copy(
            messages = bundledMessages,
            parameters = bundledParameters,
            tags = bundledTags,
            externalDocs = bundledExternalDocs,
            bindings = bundledBindings,
        )
    }
}
