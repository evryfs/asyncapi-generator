package dev.banking.asyncapi.generator.core.validator.parameters

import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.PARAMETER_DEFAULT_ENUM
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.PARAMETER_ENUM_UNIQUE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.PARAMETER_EXAMPLES_ENUM
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.PARAMETER_LOCATION_FORMAT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.PARAMETER_NAME_FORMAT
import dev.banking.asyncapi.generator.core.validator.AbstractValidatorTest
import dev.banking.asyncapi.generator.core.validator.AsyncApiValidator
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ParameterValidatorTest : AbstractValidatorTest() {

    private val asyncApiValidator = AsyncApiValidator(asyncApiContext)

    @Test
    fun `invalid parameters trigger errors and warnings`() {
        val document = parse("validator/parameters/asyncapi_validator_parameter_invalid.yaml")
        val results = asyncApiValidator.validate(document)

        assertEquals(4, results.errors.size)
        assertEquals(2, results.warnings.size)
        assertEquals(6, results.findings.size)

        val defaultEnum = results.findings.single {
            it.code == PARAMETER_DEFAULT_ENUM.code &&
                it.path ==
                "asyncapi_validator_parameter_invalid.root.components.parameters.DefaultNotInEnum.default"
        }
        assertEquals(PARAMETER_DEFAULT_ENUM.severity, defaultEnum.severity)
        assertEquals(PARAMETER_DEFAULT_ENUM.concern, defaultEnum.concern)
        assertEquals("asyncapi_validator_parameter_invalid.yaml", defaultEnum.sourceLocation?.file?.name)
        assertEquals(
            "asyncapi_validator_parameter_invalid.root.components.parameters.DefaultNotInEnum.default",
            defaultEnum.path,
        )
        assertEquals(11, defaultEnum.line)

        val parameterLocation = results.findings.single {
            it.code == PARAMETER_LOCATION_FORMAT.code &&
                it.path ==
                "asyncapi_validator_parameter_invalid.root.components.parameters.InvalidLocation.location"
        }
        assertEquals(PARAMETER_LOCATION_FORMAT.severity, parameterLocation.severity)
        assertEquals(PARAMETER_LOCATION_FORMAT.concern, parameterLocation.concern)
        assertEquals("asyncapi_validator_parameter_invalid.yaml", parameterLocation.sourceLocation?.file?.name)
        assertEquals(38, parameterLocation.line)

        val enumUnique = results.findings.single {
            it.code == PARAMETER_ENUM_UNIQUE.code &&
                it.path ==
                "asyncapi_validator_parameter_invalid.root.components.parameters.DuplicateEnum.enum"
        }
        assertEquals(PARAMETER_ENUM_UNIQUE.severity, enumUnique.severity)
        assertEquals(PARAMETER_ENUM_UNIQUE.concern, enumUnique.concern)
        assertEquals("asyncapi_validator_parameter_invalid.yaml", enumUnique.sourceLocation?.file?.name)
        assertEquals(16, enumUnique.line)

        val examplesEnum = results.findings.single {
            it.code == PARAMETER_EXAMPLES_ENUM.code &&
                it.path ==
                "asyncapi_validator_parameter_invalid.root.components.parameters.ExampleNotInEnum.examples"
        }
        assertEquals(PARAMETER_EXAMPLES_ENUM.severity, examplesEnum.severity)
        assertEquals(PARAMETER_EXAMPLES_ENUM.concern, examplesEnum.concern)
        assertEquals("asyncapi_validator_parameter_invalid.yaml", examplesEnum.sourceLocation?.file?.name)
        assertEquals(24, examplesEnum.line)

        val invalidContextLocation = results.findings.single {
            it.code == PARAMETER_LOCATION_FORMAT.code &&
                it.path ==
                "asyncapi_validator_parameter_invalid.root.components.parameters.InvalidContextLocation.location"
        }
        assertEquals(PARAMETER_LOCATION_FORMAT.severity, invalidContextLocation.severity)
        assertEquals(PARAMETER_LOCATION_FORMAT.concern, invalidContextLocation.concern)
        assertEquals("asyncapi_validator_parameter_invalid.yaml", invalidContextLocation.sourceLocation?.file?.name)
        assertEquals(46, invalidContextLocation.line)

        val invalidJsonPointerEscape = results.findings.single {
            it.code == PARAMETER_LOCATION_FORMAT.code &&
                it.path ==
                "asyncapi_validator_parameter_invalid.root.components.parameters.InvalidJsonPointerEscape.location"
        }
        assertEquals(PARAMETER_LOCATION_FORMAT.severity, invalidJsonPointerEscape.severity)
        assertEquals(PARAMETER_LOCATION_FORMAT.concern, invalidJsonPointerEscape.concern)
        assertEquals("asyncapi_validator_parameter_invalid.yaml", invalidJsonPointerEscape.sourceLocation?.file?.name)
        assertEquals(49, invalidJsonPointerEscape.line)
    }

    @Test
    fun `valid runtime expressions accept whole values and escaped JSON Pointer tokens`() {
        val results = validate("validator/parameters/asyncapi_validator_runtime_expressions_valid.yaml")

        assertEquals(emptyList(), results.findings)
    }

    @Test
    fun `parameter names use the specification key format`() {
        val results = validate("validator/parameters/asyncapi_validator_parameter_name_invalid.yaml")

        assertEquals(1, results.errors.size)
        val invalidName = results.findings.single { it.code == PARAMETER_NAME_FORMAT.code }
        assertEquals(PARAMETER_NAME_FORMAT.severity, invalidName.severity)
        assertEquals(PARAMETER_NAME_FORMAT.concern, invalidName.concern)
        assertEquals("asyncapi_validator_parameter_name_invalid.yaml", invalidName.sourceLocation?.file?.name)
        assertEquals(
            "asyncapi_validator_parameter_name_invalid.root.channels.events.parameters[\"invalid.name\"]",
            invalidName.path,
        )
        assertEquals(9, invalidName.line)
    }
}
