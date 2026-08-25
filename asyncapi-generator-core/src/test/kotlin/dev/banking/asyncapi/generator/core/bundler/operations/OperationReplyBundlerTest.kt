package dev.banking.asyncapi.generator.core.bundler.operations

import dev.banking.asyncapi.generator.core.bundler.BundlingContext
import dev.banking.asyncapi.generator.core.model.operations.OperationReply
import dev.banking.asyncapi.generator.core.model.operations.OperationReplyAddressInterface
import dev.banking.asyncapi.generator.core.model.operations.OperationReplyInterface
import dev.banking.asyncapi.generator.core.model.references.Reference
import org.junit.jupiter.api.Test
import kotlin.test.assertIs
import kotlin.test.assertSame
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

        val bundled = bundler.bundle(replyInterface, BundlingContext.empty())

        assertSame(replyInterface, bundled)
        assertTrue(replyReference.inline)
        assertIs<OperationReply>(replyReference.model)
        assertTrue(addressReference.inline)
        assertTrue(channelReference.inline)
        assertTrue(messageReference.inline)
    }
}
