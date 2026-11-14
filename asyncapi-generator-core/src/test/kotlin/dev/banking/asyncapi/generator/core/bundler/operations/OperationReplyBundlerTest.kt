package dev.banking.asyncapi.generator.core.bundler.operations

import dev.banking.asyncapi.generator.core.bundler.BundlingContext
import dev.banking.asyncapi.generator.core.model.operations.OperationReply
import dev.banking.asyncapi.generator.core.model.operations.OperationReplyAddressInterface
import dev.banking.asyncapi.generator.core.model.operations.OperationReplyInterface
import dev.banking.asyncapi.generator.core.model.references.Reference
import org.junit.jupiter.api.Test
import kotlin.test.assertIs
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OperationReplyBundlerTest {

    private val bundler = OperationReplyBundler()

    @Test
    fun `bundle bundles and inlines an unvisited operation reply reference`() {
        val addressReference = Reference("#/components/replyAddresses/success")
        val channelReference = Reference("#/channels/reply")
        val messageReference = Reference("#/components/messages/reply")
        val reply = OperationReply(
            address = OperationReplyAddressInterface.OperationReplyAddressReference(addressReference),
            channel = channelReference,
            messages = listOf(messageReference),
        )
        val replyReference = Reference("#/components/replies/success", model = reply)
        val replyInterface = OperationReplyInterface.OperationReplyReference(replyReference)

        val bundled = assertIs<OperationReplyInterface.OperationReplyReference>(
            bundler.bundle(replyInterface, BundlingContext.empty()),
        )

        assertTrue(bundled.reference.inline)
        val bundledReply = assertIs<OperationReply>(bundled.reference.model)
        assertTrue(
            assertIs<OperationReplyAddressInterface.OperationReplyAddressReference>(bundledReply.address).reference.inline,
        )
        assertTrue(bundledReply.channel!!.inline)
        assertTrue(bundledReply.messages!!.single().inline)
        assertFalse(replyReference.inline, "the input reference is not changed")
        assertFalse(channelReference.inline, "the input reference is not changed")
        assertFalse(messageReference.inline, "the input reference is not changed")
    }
}
