package dev.banking.asyncapi.generator.core.validator.operations

import dev.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiValidateException
import dev.banking.asyncapi.generator.core.model.info.Info
import dev.banking.asyncapi.generator.core.model.operations.Operation
import dev.banking.asyncapi.generator.core.model.operations.OperationInterface
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.OPERATION_ACTION_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.OPERATION_ACTION_VALUE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.OPERATION_CHANNEL_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.OPERATION_CHANNEL_TARGET
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.OPERATION_CHANNEL_REFERENCE_SCOPE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.OPERATION_MESSAGE_REFERENCE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.OPERATION_REPLY_ADDRESS_FORMAT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.OPERATION_REPLY_ADDRESS_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.OPERATION_REPLY_CHANNEL_ADDRESS
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.OPERATION_REPLY_CHANNEL_REFERENCE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.OPERATION_REPLY_CHANNEL_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.OPERATION_REPLY_MESSAGE_REFERENCE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.OPERATION_REPLY_MESSAGES_EMPTY
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.REFERENCE_UNRESOLVED
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

        assertEquals(8, results.errors.size)
        assertEquals(1, results.warnings.size)
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
        assertRule(
            results,
            OPERATION_REPLY_MESSAGES_EMPTY,
            path = "asyncapi_validator_operation_boundaries_invalid.root.operations.EmptyReplyMessages.reply.messages",
            line = 85,
        )
        assertRule(
            results,
            OPERATION_REPLY_ADDRESS_REQUIRED,
            path = "asyncapi_validator_operation_boundaries_invalid.root.operations." +
                "MissingReplyAddressLocation.reply.address.location",
            line = 92,
        )
    }

    @Test
    fun `validation fails for operation with invalid action`() {
        val document = parse("validator/operations/asyncapi_validator_operations_invalid_action.yaml")
        val validationResults = asyncApiValidator.validate(document)
        val exception = assertFailsWith<AsyncApiValidateException.ValidateError> {
            throwErrors(validationResults)
        }
        assertEquals(2, exception.errors.size, "Expected invalid and empty action errors.")
        assertRule(
            validationResults,
            OPERATION_ACTION_VALUE,
            sourceFile = "asyncapi_validator_operations_invalid_action.yaml",
            path = "asyncapi_validator_operations_invalid_action.root.operations.testOperation.action",
            line = 18,
        )
        assertRule(
            validationResults,
            OPERATION_ACTION_REQUIRED,
            sourceFile = "asyncapi_validator_operations_invalid_action.yaml",
            path = "asyncapi_validator_operations_invalid_action.root.operations.emptyAction.action",
            line = 24,
        )
    }

    @Test
    fun `validator defensively rejects a programmatic operation without a channel`() {
        val document = AsyncApiDocument(
            asyncapi = "3.0.0",
            info = Info(title = "Programmatic document", version = "1.0.0"),
            operations = mapOf(
                "missingChannel" to OperationInterface.OperationInline(Operation(action = "send")),
            ),
        )

        val validationResults = asyncApiValidator.validate(document)

        assertEquals(1, validationResults.errors.size)
        assertRule(validationResults, OPERATION_CHANNEL_REQUIRED)
    }

    @Test
    fun `validation fails for operation with broken channel reference`() {
        val document = parse("validator/operations/asyncapi_validator_operations_broken_channel_ref.yaml")
        val validationResults = asyncApiValidator.validate(document)
        val exception = assertFailsWith<AsyncApiValidateException.ValidateError> {
            throwErrors(validationResults)
        }
        assertEquals(1, exception.errors.size, "Expected 1 error for broken channel reference.")
        assertRule(
            validationResults,
            REFERENCE_UNRESOLVED,
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
