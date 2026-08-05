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
        val missingTagName = report.findings.single { it.code == TAG_NAME_REQUIRED.code }
        assertEquals(TAG_NAME_REQUIRED.severity, missingTagName.severity)
        assertEquals(TAG_NAME_REQUIRED.concern, missingTagName.concern)
        assertEquals("asyncapi_validator_reference_chain.yaml", missingTagName.sourceLocation?.file?.name)
        assertEquals(
            "asyncapi_validator_reference_chain.root.components.messages.actual.tags[0].name",
            missingTagName.path,
        )
        assertEquals(21, missingTagName.line)
    }

    @Test
    fun `terminates reference cycles without duplicating findings or rejecting resolvable edges`() {
        val report = validate("validator/references/asyncapi_validator_reference_cycle.yaml")

        assertEquals(emptyList(), report.findings)
    }

    @Test
    fun `reports an external target category mismatch at the originating reference`() {
        val report = validate("validator/references/asyncapi_validator_external_target_mismatch.yaml")

        assertEquals(1, report.errors.size)
        val mismatch = report.findings.single { it.code == REFERENCE_TARGET_CATEGORY.code }
        assertEquals(REFERENCE_TARGET_CATEGORY.severity, mismatch.severity)
        assertEquals(REFERENCE_TARGET_CATEGORY.concern, mismatch.concern)
        assertEquals("asyncapi_validator_external_target_mismatch.yaml", mismatch.sourceLocation?.file?.name)
        assertEquals(
            "asyncapi_validator_external_target_mismatch.root.components.messages.invalidMessage",
            mismatch.path,
        )
        assertEquals(7, mismatch.line)
    }

    @Test
    fun `reports malformed reference metadata instead of throwing an implementation exception`() {
        val document = parse("validator/references/asyncapi_validator_reference_chain.yaml")
        val channel = (document.channels?.getValue("events") as ChannelInterface.ChannelInline).channel
        val reference =
            (channel.messages?.getValue("first") as MessageInterface.MessageReference).reference
        reference.referenceCategoryKey = null

        val report = AsyncApiValidator(asyncApiContext).validate(document)

        val missingCategory = report.findings.single { it.code == REFERENCE_CATEGORY_REQUIRED.code }
        assertEquals(REFERENCE_CATEGORY_REQUIRED.severity, missingCategory.severity)
        assertEquals(REFERENCE_CATEGORY_REQUIRED.concern, missingCategory.concern)
        assertEquals("asyncapi_validator_reference_chain.yaml", missingCategory.sourceLocation?.file?.name)
        assertEquals(
            "asyncapi_validator_reference_chain.root.channels.events.messages.first",
            missingCategory.path,
        )
        assertEquals(9, missingCategory.line)
    }

    @Test
    fun `validates nested message trait and reply fields through their ordinary validators`() {
        val report = validate("validator/references/asyncapi_validator_nested_traversal.yaml")

        assertEquals(3, report.findings.size)

        val location = report.findings.single { it.code == CORRELATION_LOCATION_FORMAT.code }
        assertEquals(CORRELATION_LOCATION_FORMAT.severity, location.severity)
        assertEquals(CORRELATION_LOCATION_FORMAT.concern, location.concern)
        assertEquals("asyncapi_validator_nested_traversal.yaml", location.sourceLocation?.file?.name)
        assertEquals(
            "asyncapi_validator_nested_traversal.root.components.messages.event.correlationId.location",
            location.path,
        )
        assertEquals(26, location.line)

        val missingTagName = report.findings.single { it.code == TAG_NAME_REQUIRED.code }
        assertEquals(TAG_NAME_REQUIRED.severity, missingTagName.severity)
        assertEquals(TAG_NAME_REQUIRED.concern, missingTagName.concern)
        assertEquals("asyncapi_validator_nested_traversal.yaml", missingTagName.sourceLocation?.file?.name)
        assertEquals(
            "asyncapi_validator_nested_traversal.root.components.operationTraits.shared.tags[0].name",
            missingTagName.path,
        )
        assertEquals(30, missingTagName.line)

        val replyAddress = report.findings.single { it.code == OPERATION_REPLY_ADDRESS_FORMAT.code }
        assertEquals(OPERATION_REPLY_ADDRESS_FORMAT.severity, replyAddress.severity)
        assertEquals(OPERATION_REPLY_ADDRESS_FORMAT.concern, replyAddress.concern)
        assertEquals("asyncapi_validator_nested_traversal.yaml", replyAddress.sourceLocation?.file?.name)
        assertEquals(
            "asyncapi_validator_nested_traversal.root.components.replyAddresses.shared.location",
            replyAddress.path,
        )
        assertEquals(37, replyAddress.line)
    }
}
