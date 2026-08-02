package dev.banking.asyncapi.generator.core.validator.references

import dev.banking.asyncapi.generator.core.model.channels.ChannelInterface
import dev.banking.asyncapi.generator.core.model.messages.MessageInterface
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.CORRELATION_LOCATION_FORMAT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.OPERATION_REPLY_ADDRESS_FORMAT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.REFERENCE_CATEGORY_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.REFERENCE_TARGET_CATEGORY
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.TAG_NAME_REQUIRED
import dev.banking.asyncapi.generator.core.validator.AbstractValidatorTest
import dev.banking.asyncapi.generator.core.validator.AsyncApiValidator
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ReferenceIntegrityValidatorTest : AbstractValidatorTest() {

    @Test
    fun `validates a multiply referenced target once after following reference chains`() {
        val report = validate("validator/references/asyncapi_validator_reference_chain.yaml")

        assertEquals(1, report.findings.size)
        assertRule(
            report,
            rule = TAG_NAME_REQUIRED,
            sourceFile = "asyncapi_validator_reference_chain.yaml",
            path = "asyncapi_validator_reference_chain.root.components.messages.actual.tags[0].name",
            line = 21,
        )
    }

    @Test
    fun `terminates reference cycles without duplicating findings or rejecting resolvable edges`() {
        val report = validate("validator/references/asyncapi_validator_reference_cycle.yaml")

        assertNoFindings(report)
    }

    @Test
    fun `reports an external target category mismatch at the originating reference`() {
        val report = validate("validator/references/asyncapi_validator_external_target_mismatch.yaml")

        assertEquals(1, report.errors.size)
        assertRule(
            report,
            rule = REFERENCE_TARGET_CATEGORY,
            sourceFile = "asyncapi_validator_external_target_mismatch.yaml",
            path = "asyncapi_validator_external_target_mismatch.root.components.messages.invalidMessage",
            line = 7,
        )
    }

    @Test
    fun `reports malformed reference metadata instead of throwing an implementation exception`() {
        val document = parse("validator/references/asyncapi_validator_reference_chain.yaml")
        val channel = (document.channels?.getValue("events") as ChannelInterface.ChannelInline).channel
        val reference =
            (channel.messages?.getValue("first") as MessageInterface.MessageReference).reference
        reference.referenceCategoryKey = null

        val report = AsyncApiValidator(asyncApiContext).validate(document)

        assertRule(
            report,
            rule = REFERENCE_CATEGORY_REQUIRED,
            sourceFile = "asyncapi_validator_reference_chain.yaml",
            path = "asyncapi_validator_reference_chain.root.channels.events.messages.first",
            line = 9,
        )
    }

    @Test
    fun `validates nested message trait and reply fields through their ordinary validators`() {
        val report = validate("validator/references/asyncapi_validator_nested_traversal.yaml")

        assertEquals(3, report.findings.size)
        assertRule(
            report,
            rule = CORRELATION_LOCATION_FORMAT,
            sourceFile = "asyncapi_validator_nested_traversal.yaml",
            path = "asyncapi_validator_nested_traversal.root.components.messages.event.correlationId.location",
            line = 26,
        )
        assertRule(
            report,
            rule = TAG_NAME_REQUIRED,
            sourceFile = "asyncapi_validator_nested_traversal.yaml",
            path = "asyncapi_validator_nested_traversal.root.components.operationTraits.shared.tags[0].name",
            line = 30,
        )
        assertRule(
            report,
            rule = OPERATION_REPLY_ADDRESS_FORMAT,
            sourceFile = "asyncapi_validator_nested_traversal.yaml",
            path = "asyncapi_validator_nested_traversal.root.components.replyAddresses.shared.location",
            line = 37,
        )
    }
}
