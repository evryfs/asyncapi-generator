package dev.banking.asyncapi.generator.core.validator.parameters

import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiValidateException
import dev.banking.asyncapi.generator.core.model.validator.ValidationSeverity.ERROR
import dev.banking.asyncapi.generator.core.model.validator.ValidationSeverity.WARNING
import dev.banking.asyncapi.generator.core.validator.AbstractValidatorTest
import dev.banking.asyncapi.generator.core.validator.AsyncApiValidator
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ParameterValidatorTest : AbstractValidatorTest() {

    private val asyncApiValidator = AsyncApiValidator(asyncApiContext)

    @Test
    fun `invalid parameters trigger errors and warnings`() {
        val document = parse("validator/parameters/asyncapi_validator_parameter_invalid.yaml")
        val results = asyncApiValidator.validate(document)
        val errorException = assertFailsWith<AsyncApiValidateException.ValidateError> {
            throwErrors(results)
        }
        val errors = errorException.errors.map { it.message }
        assertEquals(2, errors.size, "Expected 2 validation errors.")

        assertTrue(results.hasWarnings(), "Expected warnings for non-critical issues.")
        val warnings = results.warnings.map { it.message }
        assertEquals(2, warnings.size, "Expected 2 validation warnings.")
        assertEquals(4, results.findings.size)

        assertFinding(
            results,
            severity = ERROR,
            messageContains = "'default' value ('C') is not included",
            sourceFile = "asyncapi_validator_parameter_invalid.yaml",
            path = "asyncapi_validator_parameter_invalid.root.components.parameters.DefaultNotInEnum.default",
            line = 11,
        )
        assertFinding(
            results,
            severity = ERROR,
            messageContains = "invalid 'location' expression",
            sourceFile = "asyncapi_validator_parameter_invalid.yaml",
            path = "asyncapi_validator_parameter_invalid.root.components.parameters.InvalidLocation.location",
            line = 38,
        )
        assertFinding(
            results,
            severity = WARNING,
            messageContains = "'enum' contains duplicate values",
            sourceFile = "asyncapi_validator_parameter_invalid.yaml",
            path = "asyncapi_validator_parameter_invalid.root.components.parameters.DuplicateEnum.enum",
            line = 16,
        )
        assertFinding(
            results,
            severity = WARNING,
            messageContains = "'examples' are not part of the defined enum values",
            sourceFile = "asyncapi_validator_parameter_invalid.yaml",
            path = "asyncapi_validator_parameter_invalid.root.components.parameters.ExampleNotInEnum.examples",
            line = 24,
        )
    }
}
