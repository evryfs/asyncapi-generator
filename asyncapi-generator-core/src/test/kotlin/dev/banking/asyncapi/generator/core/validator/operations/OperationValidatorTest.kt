package dev.banking.asyncapi.generator.core.validator.operations

import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiValidateException
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.OPERATION_CHANNEL_TARGET
import dev.banking.asyncapi.generator.core.model.validator.ValidationSeverity.ERROR
import dev.banking.asyncapi.generator.core.validator.AbstractValidatorTest
import dev.banking.asyncapi.generator.core.validator.AsyncApiValidator
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OperationValidatorTest : AbstractValidatorTest() {

    private val asyncApiValidator = AsyncApiValidator(asyncApiContext)

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
            messageContains = "has invalid action 'invalidAction'",
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
