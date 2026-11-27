package dev.banking.asyncapi.generator.core.bundler.operations

import dev.banking.asyncapi.generator.core.bundler.BundlingRun
import dev.banking.asyncapi.generator.core.bundler.BundlingScope
import dev.banking.asyncapi.generator.core.bundler.BundlingWalk
import dev.banking.asyncapi.generator.core.model.operations.Operation
import dev.banking.asyncapi.generator.core.model.operations.OperationInterface
import dev.banking.asyncapi.generator.core.model.operations.OperationReply
import dev.banking.asyncapi.generator.core.model.operations.OperationReplyInterface
import dev.banking.asyncapi.generator.core.model.operations.OperationTrait
import dev.banking.asyncapi.generator.core.model.operations.OperationTraitInterface
import dev.banking.asyncapi.generator.core.model.references.Reference
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

internal class OperationBundlerTest {
    private val walk = BundlingWalk()

    @Test
    fun `bundle bundles and inlines an unvisited operation reference`() {
        val traitReference = Reference("#/components/operationTraits/audit", model = OperationTrait(title = "Audit"))
        val replyChannelReference = Reference("#/channels/reply")
        val replyReference =
            Reference(
                ref = "#/components/replies/success",
                model = OperationReply(channel = replyChannelReference),
            )
        val operation =
            Operation(
                action = "send",
                traits = listOf(OperationTraitInterface.OperationTraitReference(traitReference)),
                reply = OperationReplyInterface.OperationReplyReference(replyReference),
            )
        val operationReference = Reference("#/operations/sendUserUpdated", model = operation)
        val operationInterface = OperationInterface.OperationReference(operationReference)

        val bundled =
            assertIs<OperationInterface.OperationReference>(
                walk.member(operationInterface, walk.operation, BundlingRun().rootScope()),
            )

        assertTrue(bundled.reference.inline)
        val bundledOperation = assertIs<Operation>(bundled.reference.model)
        assertTrue(
            assertIs<OperationTraitInterface.OperationTraitReference>(
                bundledOperation.traits!!.single(),
            ).reference.inline,
        )
        val bundledReply = assertIs<OperationReplyInterface.OperationReplyReference>(bundledOperation.reply)
        assertTrue(bundledReply.reference.inline)
        assertFalse(
            assertIs<OperationReply>(bundledReply.reference.model).channel!!.inline,
            "the reply channel stays a reference",
        )
        assertFalse(operationReference.inline, "the input reference is not changed")
        assertFalse(replyChannelReference.inline, "the input reference is not changed")
    }
}
