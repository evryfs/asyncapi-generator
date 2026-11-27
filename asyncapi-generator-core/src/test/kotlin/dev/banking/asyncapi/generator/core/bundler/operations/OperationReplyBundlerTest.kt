package dev.banking.asyncapi.generator.core.bundler.operations

import dev.banking.asyncapi.generator.core.bundler.BundlingRun
import dev.banking.asyncapi.generator.core.bundler.BundlingScope
import dev.banking.asyncapi.generator.core.bundler.BundlingWalk
import dev.banking.asyncapi.generator.core.model.channels.Channel
import dev.banking.asyncapi.generator.core.model.messages.Message
import dev.banking.asyncapi.generator.core.model.operations.OperationReply
import dev.banking.asyncapi.generator.core.model.operations.OperationReplyAddress
import dev.banking.asyncapi.generator.core.model.operations.OperationReplyAddressInterface
import dev.banking.asyncapi.generator.core.model.operations.OperationReplyInterface
import dev.banking.asyncapi.generator.core.model.references.Reference
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

internal class OperationReplyBundlerTest {
    private val walk = BundlingWalk()

    @Test
    fun `bundle bundles and inlines an unvisited operation reply reference`() {
        val addressReference =
            Reference(
                "#/components/replyAddresses/success",
                model = OperationReplyAddress(location = "\$message.header#/replyTo"),
            )
        val channelReference = Reference("#/channels/reply", model = Channel())
        val messageReference = Reference("#/components/messages/reply", model = Message())
        val reply =
            OperationReply(
                address = OperationReplyAddressInterface.OperationReplyAddressReference(addressReference),
                channel = channelReference,
                messages = listOf(messageReference),
            )
        val replyReference = Reference("#/components/replies/success", model = reply)
        val replyInterface = OperationReplyInterface.OperationReplyReference(replyReference)

        val bundled =
            assertIs<OperationReplyInterface.OperationReplyReference>(
                walk.member(replyInterface, walk.reply, BundlingRun().rootScope()),
            )

        assertTrue(bundled.reference.inline)
        val bundledReply = assertIs<OperationReply>(bundled.reference.model)
        assertTrue(
            assertIs<OperationReplyAddressInterface.OperationReplyAddressReference>(
                bundledReply.address,
            ).reference.inline,
        )
        assertFalse(bundledReply.channel!!.inline, "the reply channel stays a reference")
        assertFalse(bundledReply.messages!!.single().inline, "reply messages stay references")
        assertFalse(replyReference.inline, "the input reference is not changed")
        assertFalse(channelReference.inline, "the input reference is not changed")
        assertFalse(messageReference.inline, "the input reference is not changed")
    }
}
