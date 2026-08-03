package dev.banking.asyncapi.generator.core.validator.parameters

import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiValidateException
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.PARAMETER_DEFAULT_ENUM
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.PARAMETER_ENUM_UNIQUE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.PARAMETER_EXAMPLES_ENUM
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.PARAMETER_LOCATION_FORMAT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.PARAMETER_NAME_FORMAT
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
        assertEquals(4, errors.size, "Expected 4 validation errors.")

        assertTrue(results.hasWarnings(), "Expected warnings for non-critical issues.")
        val warnings = results.warnings.map { it.message }
        assertEquals(2, warnings.size, "Expected 2 validation warnings.")
        assertEquals(6, results.findings.size)

        assertRule(
            results,
            rule = PARAMETER_DEFAULT_ENUM,
            sourceFile = "asyncapi_validator_parameter_invalid.yaml",
            path = "asyncapi_validator_parameter_invalid.root.components.parameters.DefaultNotInEnum.default",
            line = 11,
        )
        assertRule(
            results,
            rule = PARAMETER_LOCATION_FORMAT,
            sourceFile = "asyncapi_validator_parameter_invalid.yaml",
            path = "asyncapi_validator_parameter_invalid.root.components.parameters.InvalidLocation.location",
            line = 38,
        )
        assertRule(
            results,
            rule = PARAMETER_ENUM_UNIQUE,
            sourceFile = "asyncapi_validator_parameter_invalid.yaml",
            path = "asyncapi_validator_parameter_invalid.root.components.parameters.DuplicateEnum.enum",
            line = 16,
        )
        assertRule(
            results,
            rule = PARAMETER_EXAMPLES_ENUM,
            sourceFile = "asyncapi_validator_parameter_invalid.yaml",
            path = "asyncapi_validator_parameter_invalid.root.components.parameters.ExampleNotInEnum.examples",
            line = 24,
        )
        assertRule(
            results,
            rule = PARAMETER_LOCATION_FORMAT,
            sourceFile = "asyncapi_validator_parameter_invalid.yaml",
            path = "asyncapi_validator_parameter_invalid.root.components.parameters.InvalidContextLocation.location",
            line = 46,
        )
        assertRule(
            results,
            rule = PARAMETER_LOCATION_FORMAT,
            sourceFile = "asyncapi_validator_parameter_invalid.yaml",
            path = "asyncapi_validator_parameter_invalid.root.components.parameters.InvalidJsonPointerEscape.location",
            line = 49,
        )
    }

    @Test
    fun `valid runtime expressions accept whole values and escaped JSON Pointer tokens`() {
        val results = validate("validator/parameters/asyncapi_validator_runtime_expressions_valid.yaml")

        assertNoFindings(results)
    }

    @Test
    fun `parameter names use the specification key format`() {
        val results = validate("validator/parameters/asyncapi_validator_parameter_name_invalid.yaml")

        assertEquals(1, results.errors.size)
        assertRule(
            results,
            rule = PARAMETER_NAME_FORMAT,
            sourceFile = "asyncapi_validator_parameter_name_invalid.yaml",
            path = "asyncapi_validator_parameter_name_invalid.root.channels.events.parameters[\"invalid.name\"]",
            line = 9,
        )
    }
}
