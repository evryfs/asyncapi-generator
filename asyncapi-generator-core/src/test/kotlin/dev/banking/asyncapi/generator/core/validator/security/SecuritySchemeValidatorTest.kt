package dev.banking.asyncapi.generator.core.validator.security

import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiValidateException
import dev.banking.asyncapi.generator.core.model.validator.ValidationSeverity.ERROR
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SECURITY_IN_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SECURITY_OAUTH_AUTHORIZATION_URL_FORMAT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SECURITY_OAUTH_AUTHORIZATION_URL_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SECURITY_OAUTH_AVAILABLE_SCOPES_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SECURITY_OAUTH_REFRESH_URL_FORMAT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SECURITY_OAUTH_SCOPE_AVAILABLE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SECURITY_OAUTH_TOKEN_URL_FORMAT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SECURITY_OAUTH_TOKEN_URL_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SECURITY_OPEN_ID_URL_FORMAT
import dev.banking.asyncapi.generator.core.validator.AbstractValidatorTest
import dev.banking.asyncapi.generator.core.validator.AsyncApiValidator
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

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
    fun `optional bearer format and empty OAuth scopes do not produce warnings`() {
        val document = parse("validator/security/asyncapi_validator_security_warnings.yaml")
        val results = asyncApiValidator.validate(document)
        val exception = assertFailsWith<AsyncApiValidateException.ValidateError> {
            throwErrors(results)
        }
        assertEquals(1, exception.errors.size, "Expected 1 error (missing name).")
        assertEquals(0, results.warnings.size)
        assertFinding(
            results,
            severity = ERROR,
            messageContains = "requires non-empty 'name'",
            sourceFile = "asyncapi_validator_security_warnings.yaml",
            path = "asyncapi_validator_security_warnings.root.components.securitySchemes.MissingNameHttpApiKey",
            line = 24,
        )
    }

    @Test
    fun `valid security scheme variants pass validation`() {
        val results = validate("validator/security/asyncapi_validator_security_valid.yaml")

        assertNoFindings(results)
    }

    @Test
    fun `validates required OAuth flow fields and absolute URLs`() {
        val results = validate("validator/security/asyncapi_validator_security_oauth_invalid.yaml")

        assertEquals(9, results.errors.size)
        assertRule(
            results,
            SECURITY_IN_REQUIRED,
            path = "asyncapi_validator_security_oauth_invalid.root.components.securitySchemes.MissingApiKeyLocation",
            line = 7,
        )
        assertRule(
            results,
            SECURITY_OAUTH_AUTHORIZATION_URL_REQUIRED,
            path = "asyncapi_validator_security_oauth_invalid.root.components.securitySchemes.InvalidOAuthFlows.flows.implicit",
            line = 12,
        )
        assertRule(
            results,
            SECURITY_OAUTH_TOKEN_URL_FORMAT,
            path = "asyncapi_validator_security_oauth_invalid.root.components.securitySchemes.InvalidOAuthFlows.flows.password.tokenUrl",
            line = 15,
        )
        assertRule(
            results,
            SECURITY_OAUTH_REFRESH_URL_FORMAT,
            path = "asyncapi_validator_security_oauth_invalid.root.components.securitySchemes.InvalidOAuthFlows.flows.password.refreshUrl",
            line = 16,
        )
        assertRule(
            results,
            SECURITY_OAUTH_TOKEN_URL_REQUIRED,
            path = "asyncapi_validator_security_oauth_invalid.root.components.securitySchemes.InvalidOAuthFlows.flows.clientCredentials",
            line = 18,
        )
        assertRule(
            results,
            SECURITY_OAUTH_AUTHORIZATION_URL_FORMAT,
            path = "asyncapi_validator_security_oauth_invalid.root.components.securitySchemes.InvalidOAuthFlows.flows.authorizationCode.authorizationUrl",
            line = 21,
        )
        assertRule(
            results,
            SECURITY_OAUTH_AVAILABLE_SCOPES_REQUIRED,
            path = "asyncapi_validator_security_oauth_invalid.root.components.securitySchemes.InvalidOAuthFlows.flows.authorizationCode",
            line = 20,
        )
        assertRule(
            results,
            SECURITY_OAUTH_SCOPE_AVAILABLE,
            path = "asyncapi_validator_security_oauth_invalid.root.components.securitySchemes.InvalidOAuthFlows.scopes",
            line = 23,
        )
        assertRule(
            results,
            SECURITY_OPEN_ID_URL_FORMAT,
            path = "asyncapi_validator_security_oauth_invalid.root.components.securitySchemes.InvalidOpenIdUrl.openIdConnectUrl",
            line = 27,
        )
    }
}
