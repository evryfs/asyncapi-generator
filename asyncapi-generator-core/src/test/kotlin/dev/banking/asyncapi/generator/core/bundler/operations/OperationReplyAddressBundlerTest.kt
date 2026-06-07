package dev.banking.asyncapi.generator.core.bundler.operations

import dev.banking.asyncapi.generator.core.bundler.BundlingContext
import dev.banking.asyncapi.generator.core.model.operations.OperationReplyAddressInterface
import dev.banking.asyncapi.generator.core.model.references.Reference
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OperationReplyAddressBundlerTest {

    private val bundler = OperationReplyAddressBundler()

    @Test
    fun `bundle marks an unvisited operation reply address reference as inline`() {
        val reference = Reference("#/components/replyAddresses/success")
        val replyAddress = OperationReplyAddressInterface.OperationReplyAddressReference(reference)

        val bundled = bundler.bundle(replyAddress, BundlingContext.empty())

        assertThat(bundled).isSameAs(replyAddress)
        assertThat(reference.inline).isTrue()
    }

    @Test
    fun `bundleMap keeps a visited operation reply address reference unchanged`() {
        val reference = Reference("#/components/replyAddresses/success")
        val replyAddress = OperationReplyAddressInterface.OperationReplyAddressReference(reference)

        val bundled = bundler.bundleMap(mapOf("success" to replyAddress), BundlingContext.empty().enter(reference))

        assertThat(bundled).containsEntry("success", replyAddress)
        assertThat(reference.inline).isFalse()
    }
}
