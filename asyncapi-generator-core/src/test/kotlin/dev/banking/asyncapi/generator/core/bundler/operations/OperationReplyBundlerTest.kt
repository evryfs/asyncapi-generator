package dev.banking.asyncapi.generator.core.bundler.operations

import dev.banking.asyncapi.generator.core.bundler.BundlingContext
import dev.banking.asyncapi.generator.core.model.operations.OperationReply
import dev.banking.asyncapi.generator.core.model.operations.OperationReplyAddressInterface
import dev.banking.asyncapi.generator.core.model.operations.OperationReplyInterface
import dev.banking.asyncapi.generator.core.model.references.Reference
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

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

        assertThat(bundled).isSameAs(replyInterface)
        assertThat(replyReference.inline).isTrue()
        assertThat(replyReference.model).isInstanceOf(OperationReply::class.java)
        assertThat(addressReference.inline).isTrue()
        assertThat(channelReference.inline).isTrue()
        assertThat(messageReference.inline).isTrue()
    }

}
