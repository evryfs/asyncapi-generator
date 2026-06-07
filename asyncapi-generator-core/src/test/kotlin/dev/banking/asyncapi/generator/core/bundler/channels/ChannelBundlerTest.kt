package dev.banking.asyncapi.generator.core.bundler.channels

import dev.banking.asyncapi.generator.core.bundler.BundlingContext
import dev.banking.asyncapi.generator.core.model.channels.Channel
import dev.banking.asyncapi.generator.core.model.channels.ChannelInterface
import dev.banking.asyncapi.generator.core.model.messages.Message
import dev.banking.asyncapi.generator.core.model.messages.MessageInterface
import dev.banking.asyncapi.generator.core.model.references.Reference
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ChannelBundlerTest {

    private val bundler = ChannelBundler()

    @Test
    fun `bundle bundles and inlines an unvisited channel reference`() {
        val messageReference = Reference("#/components/messages/userUpdated", model = Message(name = "userUpdated"))
        val channel = Channel(
            address = "users.updated",
            messages = mapOf("userUpdated" to MessageInterface.MessageReference(messageReference)),
        )
        val channelReference = Reference("#/channels/userUpdated", model = channel)
        val channelInterface = ChannelInterface.ChannelReference(channelReference)

        val bundled = bundler.bundle(channelInterface, BundlingContext.empty())

        assertThat(bundled).isSameAs(channelInterface)
        assertThat(channelReference.inline).isTrue()
        assertThat(channelReference.model).isInstanceOf(Channel::class.java)
        assertThat(messageReference.inline).isTrue()
    }

    @Test
    fun `bundle keeps a visited channel reference unchanged`() {
        val channel = Channel(address = "users.updated")
        val channelReference = Reference("#/channels/userUpdated", model = channel)
        val channelInterface = ChannelInterface.ChannelReference(channelReference)

        val bundled = bundler.bundle(channelInterface, BundlingContext.empty().enter(channelReference))

        assertThat(bundled).isSameAs(channelInterface)
        assertThat(channelReference.inline).isFalse()
        assertThat(channelReference.model).isSameAs(channel)
    }
}
