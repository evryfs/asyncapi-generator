package dev.banking.asyncapi.generator.core.validator.security

import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiValidateException
import dev.banking.asyncapi.generator.core.model.security.OAuthFlow
import dev.banking.asyncapi.generator.core.model.security.OAuthFlows
import dev.banking.asyncapi.generator.core.model.security.SecurityScheme
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SECURITY_IN_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SECURITY_IN_VALUE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SECURITY_NAME_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SECURITY_OAUTH_AUTHORIZATION_URL_FORMAT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SECURITY_OAUTH_AUTHORIZATION_URL_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SECURITY_OAUTH_AVAILABLE_SCOPES_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SECURITY_OAUTH_FLOWS_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SECURITY_OAUTH_REFRESH_URL_FORMAT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SECURITY_OAUTH_SCOPE_AVAILABLE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SECURITY_OAUTH_TOKEN_URL_FORMAT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SECURITY_OAUTH_TOKEN_URL_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SECURITY_OPEN_ID_URL_FORMAT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SECURITY_OPEN_ID_URL_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SECURITY_SCHEME_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SECURITY_TYPE_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SECURITY_TYPE_VALUE
import dev.banking.asyncapi.generator.core.validator.AbstractValidatorTest
import dev.banking.asyncapi.generator.core.validator.AsyncApiValidator
import dev.banking.asyncapi.generator.core.validator.util.ValidationCollector
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
        assertEquals(7, exception.errors.size, "Expected 7 validation errors.")
        assertRule(
            results,
            SECURITY_TYPE_REQUIRED,
            sourceFile = "asyncapi_validator_security_invalid.yaml",
            path = "asyncapi_validator_security_invalid.root.components.securitySchemes.EmptyType.type",
            line = 9,
        )
        assertRule(
            results,
            SECURITY_SCHEME_REQUIRED,
            sourceFile = "asyncapi_validator_security_invalid.yaml",
            path = "asyncapi_validator_security_invalid.root.components.securitySchemes.InvalidHttp",
            line = 12,
        )
        assertRule(
            results,
            SECURITY_OAUTH_FLOWS_REQUIRED,
            sourceFile = "asyncapi_validator_security_invalid.yaml",
            path = "asyncapi_validator_security_invalid.root.components.securitySchemes.InvalidOAuth2",
            line = 17,
        )
        assertRule(
            results,
            SECURITY_OPEN_ID_URL_REQUIRED,
            sourceFile = "asyncapi_validator_security_invalid.yaml",
            path = "asyncapi_validator_security_invalid.root.components.securitySchemes.InvalidOpenID",
            line = 22,
        )
        assertRule(
            results,
            SECURITY_IN_VALUE,
            sourceFile = "asyncapi_validator_security_invalid.yaml",
            path = "asyncapi_validator_security_invalid.root.components.securitySchemes.InvalidApiKey",
            line = 27,
        )
        assertRule(
            results,
            SECURITY_IN_VALUE,
            sourceFile = "asyncapi_validator_security_invalid.yaml",
            path = "asyncapi_validator_security_invalid.root.components.securitySchemes.InvalidHttpApiKey",
            line = 32,
        )
        assertRule(
            results,
            SECURITY_TYPE_VALUE,
            sourceFile = "asyncapi_validator_security_invalid.yaml",
            path = "asyncapi_validator_security_invalid.root.components.securitySchemes.UnknownType.type",
            line = 39,
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
        assertRule(
            results,
            SECURITY_NAME_REQUIRED,
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

        assertEquals(8, results.errors.size)
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
            SECURITY_OAUTH_SCOPE_AVAILABLE,
            path = "asyncapi_validator_security_oauth_invalid.root.components.securitySchemes.InvalidOAuthFlows.scopes",
            line = 24,
        )
        assertRule(
            results,
            SECURITY_OPEN_ID_URL_FORMAT,
            path = "asyncapi_validator_security_oauth_invalid.root.components.securitySchemes.InvalidOpenIdUrl.openIdConnectUrl",
            line = 28,
        )
    }

    @Test
    fun `validator defensively rejects programmatic OAuth flows without available scopes`() {
        val scheme = SecurityScheme(
            type = "oauth2",
            flows = OAuthFlows(
                authorizationCode = OAuthFlow(
                    authorizationUrl = "https://example.com/authorize",
                    tokenUrl = "https://example.com/token",
                    availableScopes = null,
                ),
            ),
        )
        val collector = ValidationCollector()

        OAuthFlowsValidator(asyncApiContext).validate(scheme, "Security Scheme", collector)
        val results = collector.report()

        assertEquals(1, results.errors.size)
        assertRule(results, SECURITY_OAUTH_AVAILABLE_SCOPES_REQUIRED)
    }
}
