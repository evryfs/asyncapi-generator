package dev.banking.asyncapi.generator.core.validator.operations

import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiValidateException
import dev.banking.asyncapi.generator.core.validator.AbstractValidatorTest
import dev.banking.asyncapi.generator.core.validator.AsyncApiValidator
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OperationValidatorTest : AbstractValidatorTest() {

    private val asyncApiValidator = AsyncApiValidator(asyncApiContext)

    @Test
    fun `validation fails for operation with invalid action`() {
        val document = parse("src/test/resources/validator/operations/asyncapi_validator_operations_invalid_action.yaml")
        val validationResults = asyncApiValidator.validate(document)
        val exception = assertFailsWith<AsyncApiValidateException.ValidateError> {
            validationResults.throwErrors()
        }
        assertEquals(1, exception.errors.size, "Expected 1 error for invalid action.")
    }

    @Test
    fun `validation fails for operation with broken channel reference`() {
        val document = parse("src/test/resources/validator/operations/asyncapi_validator_operations_broken_channel_ref.yaml")
        val validationResults = asyncApiValidator.validate(document)
        val exception = assertFailsWith<AsyncApiValidateException.ValidateError> {
            validationResults.throwErrors()
        }
        assertEquals(1, exception.errors.size, "Expected 1 error for broken channel reference.")
    }

    @Test
    fun `validation fails for operation channel reference type mismatch`() {
        val document = parse("src/test/resources/validator/operations/asyncapi_validator_operations_channel_ref_type_mismatch.yaml")
        val validationResults = asyncApiValidator.validate(document)
        val exception = assertFailsWith<AsyncApiValidateException.ValidateError> {
            validationResults.throwErrors()
        }
        assertEquals(1, exception.errors.size, "Expected 1 error: channel type mismatch.")
    }
}
