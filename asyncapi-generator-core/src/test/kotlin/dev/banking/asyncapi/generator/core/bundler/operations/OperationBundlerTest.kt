package dev.banking.asyncapi.generator.core.bundler.operations

import dev.banking.asyncapi.generator.core.bundler.BundlingContext
import dev.banking.asyncapi.generator.core.model.operations.Operation
import dev.banking.asyncapi.generator.core.model.operations.OperationInterface
import dev.banking.asyncapi.generator.core.model.operations.OperationReply
import dev.banking.asyncapi.generator.core.model.operations.OperationReplyInterface
import dev.banking.asyncapi.generator.core.model.operations.OperationTrait
import dev.banking.asyncapi.generator.core.model.operations.OperationTraitInterface
import dev.banking.asyncapi.generator.core.model.references.Reference
import org.junit.jupiter.api.Test
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

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

        assertSame(operationInterface, bundled)
        assertTrue(operationReference.inline)
        assertIs<Operation>(operationReference.model)
        assertTrue(traitReference.inline)
        assertTrue(replyReference.inline)
        assertTrue(replyChannelReference.inline)
    }
}
