package dev.banking.asyncapi.generator.core.validator.security

import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiValidateException
import dev.banking.asyncapi.generator.core.model.validator.ValidationSeverity.ERROR
import dev.banking.asyncapi.generator.core.model.validator.ValidationSeverity.WARNING
import dev.banking.asyncapi.generator.core.validator.AbstractValidatorTest
import dev.banking.asyncapi.generator.core.validator.AsyncApiValidator
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SecuritySchemeValidatorTest : AbstractValidatorTest() {

    private val asyncApiValidator = AsyncApiValidator(asyncApiContext)

    @Test
    fun `invalid security schemes trigger validation errors`() {
        val document = parse("validator/security/asyncapi_validator_security_invalid.yaml")
        val results = asyncApiValidator.validate(document)
        val exception = assertFailsWith<AsyncApiValidateException.ValidateError> { throwErrors(results) }
        assertEquals(6, exception.errors.size, "Expected 6 validation errors.")
        assertFinding(
            results,
            severity = ERROR,
            messageContains = "of type 'http' requires non-empty 'scheme'",
            sourceFile = "asyncapi_validator_security_invalid.yaml",
            path = "asyncapi_validator_security_invalid.root.components.securitySchemes.InvalidHttp",
            line = 9,
        )
        assertFinding(
            results,
            severity = ERROR,
            messageContains = "invalid 'in' value 'header'",
            sourceFile = "asyncapi_validator_security_invalid.yaml",
            path = "asyncapi_validator_security_invalid.root.components.securitySchemes.InvalidApiKey",
            line = 24,
        )
        assertFinding(
            results,
            severity = ERROR,
            messageContains = "invalid type 'alien_technology'",
            sourceFile = "asyncapi_validator_security_invalid.yaml",
            path = "asyncapi_validator_security_invalid.root.components.securitySchemes.UnknownType.type",
            line = 36,
        )
    }

    @Test
    fun `security schemes with missing optional fields trigger warnings`() {
        val document = parse("validator/security/asyncapi_validator_security_warnings.yaml")
        val results = asyncApiValidator.validate(document)
        val exception = assertFailsWith<AsyncApiValidateException.ValidateError> {
            throwErrors(results)
        }
        assertEquals(1, exception.errors.size, "Expected 1 error (missing name).")
        assertTrue(results.hasWarnings(), "Should have warnings.")
        assertFinding(
            results,
            severity = ERROR,
            messageContains = "requires non-empty 'name'",
            sourceFile = "asyncapi_validator_security_warnings.yaml",
            path = "asyncapi_validator_security_warnings.root.components.securitySchemes.MissingNameHttpApiKey",
            line = 24,
        )
        assertFinding(
            results,
            severity = WARNING,
            messageContains = "has an empty 'bearerFormat'",
            sourceFile = "asyncapi_validator_security_warnings.yaml",
            path = "asyncapi_validator_security_warnings.root.components.securitySchemes.MissingBearerFormat",
            line = 8,
        )
    }
}
