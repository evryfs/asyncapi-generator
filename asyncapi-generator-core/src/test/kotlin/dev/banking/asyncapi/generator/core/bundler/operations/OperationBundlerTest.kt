package dev.banking.asyncapi.generator.core.bundler.operations

import dev.banking.asyncapi.generator.core.bundler.BundlingContext
import dev.banking.asyncapi.generator.core.model.operations.Operation
import dev.banking.asyncapi.generator.core.model.operations.OperationInterface
import dev.banking.asyncapi.generator.core.model.operations.OperationReply
import dev.banking.asyncapi.generator.core.model.operations.OperationReplyInterface
import dev.banking.asyncapi.generator.core.model.operations.OperationTrait
import dev.banking.asyncapi.generator.core.model.operations.OperationTraitInterface
import dev.banking.asyncapi.generator.core.model.references.Reference
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OperationBundlerTest {

    private val bundler = OperationBundler()

    @Test
    fun `bundle bundles and inlines an unvisited operation reference`() {
        val traitReference = Reference("#/components/operationTraits/audit", model = OperationTrait(title = "Audit"))
        val replyChannelReference = Reference("#/channels/reply")
        val replyReference = Reference(
            ref = "#/components/replies/success",
            model = OperationReply(channel = replyChannelReference),
        )
        val operation = Operation(
            action = "send",
            traits = listOf(OperationTraitInterface.OperationTraitReference(traitReference)),
            reply = OperationReplyInterface.OperationReplyReference(replyReference),
        )
        val operationReference = Reference("#/operations/sendUserUpdated", model = operation)
        val operationInterface = OperationInterface.OperationReference(operationReference)

        val bundled = bundler.bundle(operationInterface, BundlingContext.empty())

        assertThat(bundled).isSameAs(operationInterface)
        assertThat(operationReference.inline).isTrue()
        assertThat(operationReference.model).isInstanceOf(Operation::class.java)
        assertThat(traitReference.inline).isTrue()
        assertThat(replyReference.inline).isTrue()
        assertThat(replyChannelReference.inline).isTrue()
    }

}
