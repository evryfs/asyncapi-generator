package dev.banking.asyncapi.generator.core.bundler.channels

import dev.banking.asyncapi.generator.core.bundler.BundlingContext
import dev.banking.asyncapi.generator.core.model.channels.Channel
import dev.banking.asyncapi.generator.core.model.channels.ChannelInterface
import dev.banking.asyncapi.generator.core.model.messages.Message
import dev.banking.asyncapi.generator.core.model.messages.MessageInterface
import dev.banking.asyncapi.generator.core.model.references.Reference
import org.junit.jupiter.api.Test
import kotlin.test.assertIs
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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

        val bundled = assertIs<ChannelInterface.ChannelReference>(
            bundler.bundle(channelInterface, BundlingContext.empty()),
        )

        assertTrue(bundled.reference.inline)
        val bundledChannel = assertIs<Channel>(bundled.reference.model)
        val bundledMessage = assertIs<MessageInterface.MessageReference>(bundledChannel.messages!!["userUpdated"])
        assertTrue(bundledMessage.reference.inline)
        assertFalse(channelReference.inline, "the input reference is not changed")
        assertFalse(messageReference.inline, "the input reference is not changed")
    }
}
