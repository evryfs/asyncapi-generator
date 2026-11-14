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
import kotlin.test.assertFalse
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

        val bundled = assertIs<OperationInterface.OperationReference>(
            bundler.bundle(operationInterface, BundlingContext.empty()),
        )

        assertTrue(bundled.reference.inline)
        val bundledOperation = assertIs<Operation>(bundled.reference.model)
        assertTrue(assertIs<OperationTraitInterface.OperationTraitReference>(bundledOperation.traits!!.single()).reference.inline)
        val bundledReply = assertIs<OperationReplyInterface.OperationReplyReference>(bundledOperation.reply)
        assertTrue(bundledReply.reference.inline)
        assertTrue(assertIs<OperationReply>(bundledReply.reference.model).channel!!.inline)
        assertFalse(operationReference.inline, "the input reference is not changed")
        assertFalse(replyChannelReference.inline, "the input reference is not changed")
    }
}
