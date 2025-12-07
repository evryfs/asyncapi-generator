package com.tietoevry.banking.asyncapi.generator.core.bundler.channels

import com.tietoevry.banking.asyncapi.generator.core.bundler.externaldocs.ExternalDocsBundler
import com.tietoevry.banking.asyncapi.generator.core.bundler.parameters.ParameterBundler
import com.tietoevry.banking.asyncapi.generator.core.bundler.bindings.BindingBundler
import com.tietoevry.banking.asyncapi.generator.core.bundler.messages.MessagesBundler
import com.tietoevry.banking.asyncapi.generator.core.bundler.tags.TagBundler
import com.tietoevry.banking.asyncapi.generator.core.model.channels.Channel
import com.tietoevry.banking.asyncapi.generator.core.model.channels.ChannelInterface

class ChannelBundler {

    private val messagesBundler: MessagesBundler = MessagesBundler()
    private val parameterBundler: ParameterBundler = ParameterBundler()
    private val tagBundler: TagBundler = TagBundler()
    private val externalDocsBundler: ExternalDocsBundler = ExternalDocsBundler()
    private val bindingBundler = BindingBundler()

    fun bundleMap(channels: Map<String, ChannelInterface>?, visited: Set<String>): Map<String, ChannelInterface>? {
        if (channels == null) return null
        return channels.mapValues { (_, channelInterface) ->
            when (channelInterface) {
                is ChannelInterface.ChannelInline ->
                    ChannelInterface.ChannelInline(
                        bundleChannel(channelInterface.channel, visited)
                    )
                is ChannelInterface.ChannelReference -> {
                    val ref = channelInterface.reference.ref
                    if (visited.contains(ref)) {
                        channelInterface
                    } else {
                        val channelModel = channelInterface.reference.requireModel<Channel>()
                        val newVisited = visited + ref
                        val bundled = bundleChannel(channelModel, newVisited)
                        channelInterface.reference.model = bundled
                        channelInterface.reference.inline()
                        channelInterface

                    }
                }
            }
        }
    }

    fun bundleChannel(channel: Channel, visited: Set<String>): Channel {
        val bundledMessages = messagesBundler.bundleMap(channel.messages, visited)
        val bundledParameters = parameterBundler.bundleMap(channel.parameters, visited)
        val bundledTags = tagBundler.bundleList(channel.tags, visited)
        val bundledExternalDocs = channel.externalDocs?.let { externalDocsBundler.bundle(it, visited)}
        val bundledBindings = bindingBundler.bundleMap(channel.bindings, visited)
        return channel.copy(
            messages = bundledMessages,
            parameters = bundledParameters,
            tags = bundledTags,
            externalDocs = bundledExternalDocs,
            bindings = bundledBindings
        )
    }
}
