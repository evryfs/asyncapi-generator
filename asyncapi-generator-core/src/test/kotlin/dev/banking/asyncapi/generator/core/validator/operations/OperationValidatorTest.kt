package dev.banking.asyncapi.generator.core.validator.operations

import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiValidateException
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.OPERATION_CHANNEL_TARGET
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.OPERATION_CHANNEL_REFERENCE_SCOPE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.OPERATION_MESSAGE_REFERENCE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.OPERATION_REPLY_ADDRESS_FORMAT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.OPERATION_REPLY_CHANNEL_ADDRESS
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.OPERATION_REPLY_CHANNEL_REFERENCE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.OPERATION_REPLY_CHANNEL_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.OPERATION_REPLY_MESSAGE_REFERENCE
import dev.banking.asyncapi.generator.core.model.validator.ValidationSeverity.ERROR
import dev.banking.asyncapi.generator.core.validator.AbstractValidatorTest
import dev.banking.asyncapi.generator.core.validator.AsyncApiValidator
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OperationValidatorTest : AbstractValidatorTest() {

    private val asyncApiValidator = AsyncApiValidator(asyncApiContext)

    @Test
    fun `accepts root and component operations that reference messages through their channels`() {
        val results = validate("validator/operations/asyncapi_validator_operation_boundaries_valid.yaml")

        assertNoFindings(results)
    }

    @Test
    fun `enforces operation and reply channel ownership and message subsets`() {
        val results = validate("validator/operations/asyncapi_validator_operation_boundaries_invalid.yaml")

        assertEquals(7, results.errors.size)
        assertRule(
            results,
            OPERATION_CHANNEL_REFERENCE_SCOPE,
            path = "asyncapi_validator_operation_boundaries_invalid.root.operations.InvalidRootChannelScope.channel",
            line = 33,
        )
        assertRule(
            results,
            OPERATION_MESSAGE_REFERENCE,
            path = "asyncapi_validator_operation_boundaries_invalid.root.operations.InvalidMessageSubset.messages[0]",
            line = 40,
        )
        assertRule(
            results,
            OPERATION_REPLY_CHANNEL_REFERENCE,
            path = "asyncapi_validator_operation_boundaries_invalid.root.operations.InvalidReplyChannelScope.reply.channel",
            line = 46,
        )
        assertRule(
            results,
            OPERATION_REPLY_MESSAGE_REFERENCE,
            path = "asyncapi_validator_operation_boundaries_invalid.root.operations.InvalidReplyMessageSubset.reply.messages[0]",
            line = 56,
        )
        assertRule(
            results,
            OPERATION_REPLY_CHANNEL_ADDRESS,
            path = "asyncapi_validator_operation_boundaries_invalid.root.operations.InvalidReplyChannelAddress.reply.channel",
            line = 64,
        )
        assertRule(
            results,
            OPERATION_REPLY_CHANNEL_REQUIRED,
            path = "asyncapi_validator_operation_boundaries_invalid.root.operations.MissingReplyChannel.reply.messages",
            line = 71,
        )
        assertRule(
            results,
            OPERATION_REPLY_ADDRESS_FORMAT,
            path = "asyncapi_validator_operation_boundaries_invalid.root.operations.InvalidReplyAddress.reply.address.location",
            line = 79,
        )
    }

    @Test
    fun `validation fails for operation with invalid action`() {
        val document = parse("validator/operations/asyncapi_validator_operations_invalid_action.yaml")
        val validationResults = asyncApiValidator.validate(document)
        val exception = assertFailsWith<AsyncApiValidateException.ValidateError> {
            throwErrors(validationResults)
        }
        assertEquals(1, exception.errors.size, "Expected 1 error for invalid action.")
        assertFinding(
            validationResults,
            severity = ERROR,
            messageContains = "has invalid action ' send '",
            sourceFile = "asyncapi_validator_operations_invalid_action.yaml",
            path = "asyncapi_validator_operations_invalid_action.root.operations.testOperation.action",
            line = 18,
        )
    }

    @Test
    fun `validation fails for operation with broken channel reference`() {
        val document = parse("validator/operations/asyncapi_validator_operations_broken_channel_ref.yaml")
        val validationResults = asyncApiValidator.validate(document)
        val exception = assertFailsWith<AsyncApiValidateException.ValidateError> {
            throwErrors(validationResults)
        }
        assertEquals(1, exception.errors.size, "Expected 1 error for broken channel reference.")
        assertFinding(
            validationResults,
            severity = ERROR,
            messageContains = "reference '#/channels/nonExistentChannel' could not be resolved",
            sourceFile = "asyncapi_validator_operations_broken_channel_ref.yaml",
            path = "asyncapi_validator_operations_broken_channel_ref.root.operations.testOperation.channel",
            line = 9,
        )
    }

    @Test
    fun `validation fails for operation channel reference type mismatch`() {
        val document = parse("validator/operations/asyncapi_validator_operations_channel_ref_type_mismatch.yaml")
        val validationResults = asyncApiValidator.validate(document)
        val exception = assertFailsWith<AsyncApiValidateException.ValidateError> {
            throwErrors(validationResults)
        }
        assertEquals(1, exception.errors.size, "Expected 1 error: channel type mismatch.")
        assertRule(
            validationResults,
            rule = OPERATION_CHANNEL_TARGET,
            sourceFile = "asyncapi_validator_operations_channel_ref_type_mismatch.yaml",
            path = "asyncapi_validator_operations_channel_ref_type_mismatch.root.operations.testOperation.channel",
            line = 18,
        )
    }
}
