package dev.banking.asyncapi.generator.core.validator.security

import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SECURITY_IN_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SECURITY_IN_VALUE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SECURITY_NAME_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SECURITY_OAUTH_AUTHORIZATION_URL_FORMAT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SECURITY_OAUTH_AUTHORIZATION_URL_REQUIRED
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
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SecuritySchemeValidatorTest : AbstractValidatorTest() {

    private val asyncApiValidator = AsyncApiValidator(asyncApiContext)

    @Test
    fun `invalid security schemes trigger validation errors`() {
        val document = parse("validator/security/asyncapi_validator_security_invalid.yaml")
        val results = asyncApiValidator.validate(document)

        assertEquals(7, results.errors.size)
        val typeRequired = results.findings.single { it.code == SECURITY_TYPE_REQUIRED.code }
        assertEquals(SECURITY_TYPE_REQUIRED.severity, typeRequired.severity)
        assertEquals(SECURITY_TYPE_REQUIRED.concern, typeRequired.concern)
        assertEquals("asyncapi_validator_security_invalid.yaml", typeRequired.sourceLocation?.file?.name)
        assertEquals(
            "asyncapi_validator_security_invalid.root.components.securitySchemes.EmptyType.type",
            typeRequired.path,
        )
        assertEquals(9, typeRequired.line)

        val schemeRequired = results.findings.single { it.code == SECURITY_SCHEME_REQUIRED.code }
        assertEquals(SECURITY_SCHEME_REQUIRED.severity, schemeRequired.severity)
        assertEquals(SECURITY_SCHEME_REQUIRED.concern, schemeRequired.concern)
        assertEquals("asyncapi_validator_security_invalid.yaml", schemeRequired.sourceLocation?.file?.name)
        assertEquals("asyncapi_validator_security_invalid.root.components.securitySchemes.InvalidHttp", schemeRequired.path)
        assertEquals(12, schemeRequired.line)

        val flowsRequired = results.findings.single { it.code == SECURITY_OAUTH_FLOWS_REQUIRED.code }
        assertEquals(SECURITY_OAUTH_FLOWS_REQUIRED.severity, flowsRequired.severity)
        assertEquals(SECURITY_OAUTH_FLOWS_REQUIRED.concern, flowsRequired.concern)
        assertEquals("asyncapi_validator_security_invalid.yaml", flowsRequired.sourceLocation?.file?.name)
        assertEquals("asyncapi_validator_security_invalid.root.components.securitySchemes.InvalidOAuth2", flowsRequired.path)
        assertEquals(17, flowsRequired.line)

        val openIdRequired = results.findings.single {
            it.code == SECURITY_OPEN_ID_URL_REQUIRED.code &&
                it.path ==
                "asyncapi_validator_security_invalid.root.components.securitySchemes.InvalidOpenID"
        }
        assertEquals(SECURITY_OPEN_ID_URL_REQUIRED.code, openIdRequired.code)
        assertEquals(SECURITY_OPEN_ID_URL_REQUIRED.severity, openIdRequired.severity)
        assertEquals(SECURITY_OPEN_ID_URL_REQUIRED.concern, openIdRequired.concern)
        assertEquals("asyncapi_validator_security_invalid.yaml", openIdRequired.sourceLocation?.file?.name)
        assertEquals("asyncapi_validator_security_invalid.root.components.securitySchemes.InvalidOpenID", openIdRequired.path)
        assertEquals(22, openIdRequired.line)

        val inValue = results.findings.single {
            it.code == SECURITY_IN_VALUE.code &&
                it.path ==
                "asyncapi_validator_security_invalid.root.components.securitySchemes.InvalidApiKey"
        }
        assertEquals(SECURITY_IN_VALUE.severity, inValue.severity)
        assertEquals(SECURITY_IN_VALUE.concern, inValue.concern)
        assertEquals("asyncapi_validator_security_invalid.yaml", inValue.sourceLocation?.file?.name)
        assertEquals("asyncapi_validator_security_invalid.root.components.securitySchemes.InvalidApiKey", inValue.path)
        assertEquals(27, inValue.line)

        val inValueHttp = results.findings.single {
            it.code == SECURITY_IN_VALUE.code &&
                it.path ==
                "asyncapi_validator_security_invalid.root.components.securitySchemes.InvalidHttpApiKey"
        }
        assertEquals(SECURITY_IN_VALUE.severity, inValueHttp.severity)
        assertEquals(SECURITY_IN_VALUE.concern, inValueHttp.concern)
        assertEquals("asyncapi_validator_security_invalid.yaml", inValueHttp.sourceLocation?.file?.name)
        assertEquals(
            "asyncapi_validator_security_invalid.root.components.securitySchemes.InvalidHttpApiKey",
            inValueHttp.path,
        )
        assertEquals(32, inValueHttp.line)

        val unknownType = results.findings.single { it.code == SECURITY_TYPE_VALUE.code }
        assertEquals(SECURITY_TYPE_VALUE.severity, unknownType.severity)
        assertEquals(SECURITY_TYPE_VALUE.concern, unknownType.concern)
        assertEquals("asyncapi_validator_security_invalid.yaml", unknownType.sourceLocation?.file?.name)
        assertEquals("asyncapi_validator_security_invalid.root.components.securitySchemes.UnknownType.type", unknownType.path)
        assertEquals(39, unknownType.line)
    }

    @Test
    fun `optional bearer format and empty OAuth scopes do not produce additional findings`() {
        val document = parse("validator/security/asyncapi_validator_security_warnings.yaml")
        val results = asyncApiValidator.validate(document)

        assertEquals(1, results.errors.size)
        assertEquals(0, results.warnings.size)
        val missingName = results.findings.single()
        assertEquals(SECURITY_NAME_REQUIRED.code, missingName.code)
        assertEquals(SECURITY_NAME_REQUIRED.severity, missingName.severity)
        assertEquals(SECURITY_NAME_REQUIRED.concern, missingName.concern)
        assertEquals("asyncapi_validator_security_warnings.yaml", missingName.sourceLocation?.file?.name)
        assertEquals(
            "asyncapi_validator_security_warnings.root.components.securitySchemes.MissingNameHttpApiKey",
            missingName.path,
        )
        assertEquals(24, missingName.line)
    }

    @Test
    fun `valid security scheme variants pass validation`() {
        val results = validate("validator/security/asyncapi_validator_security_valid.yaml")

        assertEquals(emptyList(), results.findings)
    }

    @Test
    fun `validates required OAuth flow fields and absolute URLs`() {
        val results = validate("validator/security/asyncapi_validator_security_oauth_invalid.yaml")

        assertEquals(8, results.errors.size)
        val inRequired = results.findings.single { it.code == SECURITY_IN_REQUIRED.code }
        assertEquals(SECURITY_IN_REQUIRED.severity, inRequired.severity)
        assertEquals(SECURITY_IN_REQUIRED.concern, inRequired.concern)
        assertEquals("asyncapi_validator_security_oauth_invalid.yaml", inRequired.sourceLocation?.file?.name)
        assertEquals("asyncapi_validator_security_oauth_invalid.root.components.securitySchemes.MissingApiKeyLocation", inRequired.path)
        assertEquals(7, inRequired.line)

        val authUrlRequired =
            results.findings.single { it.code == SECURITY_OAUTH_AUTHORIZATION_URL_REQUIRED.code }
        assertEquals(SECURITY_OAUTH_AUTHORIZATION_URL_REQUIRED.severity, authUrlRequired.severity)
        assertEquals(SECURITY_OAUTH_AUTHORIZATION_URL_REQUIRED.concern, authUrlRequired.concern)
        assertEquals(
            "asyncapi_validator_security_oauth_invalid.yaml",
            authUrlRequired.sourceLocation?.file?.name,
        )
        assertEquals(
            "asyncapi_validator_security_oauth_invalid.root.components.securitySchemes.InvalidOAuthFlows.flows.implicit",
            authUrlRequired.path,
        )
        assertEquals(12, authUrlRequired.line)

        val tokenUrlFormat = results.findings.single { it.code == SECURITY_OAUTH_TOKEN_URL_FORMAT.code }
        assertEquals(SECURITY_OAUTH_TOKEN_URL_FORMAT.severity, tokenUrlFormat.severity)
        assertEquals(SECURITY_OAUTH_TOKEN_URL_FORMAT.concern, tokenUrlFormat.concern)
        assertEquals("asyncapi_validator_security_oauth_invalid.yaml", tokenUrlFormat.sourceLocation?.file?.name)
        assertEquals(
            "asyncapi_validator_security_oauth_invalid.root.components.securitySchemes.InvalidOAuthFlows.flows.password.tokenUrl",
            tokenUrlFormat.path,
        )
        assertEquals(15, tokenUrlFormat.line)

        val refreshUrlFormat =
            results.findings.single { it.code == SECURITY_OAUTH_REFRESH_URL_FORMAT.code }
        assertEquals(SECURITY_OAUTH_REFRESH_URL_FORMAT.severity, refreshUrlFormat.severity)
        assertEquals(SECURITY_OAUTH_REFRESH_URL_FORMAT.concern, refreshUrlFormat.concern)
        assertEquals("asyncapi_validator_security_oauth_invalid.yaml", refreshUrlFormat.sourceLocation?.file?.name)
        assertEquals(
            "asyncapi_validator_security_oauth_invalid.root.components.securitySchemes.InvalidOAuthFlows.flows.password.refreshUrl",
            refreshUrlFormat.path,
        )
        assertEquals(16, refreshUrlFormat.line)

        val tokenUrlRequired = results.findings.single { it.code == SECURITY_OAUTH_TOKEN_URL_REQUIRED.code }
        assertEquals(SECURITY_OAUTH_TOKEN_URL_REQUIRED.severity, tokenUrlRequired.severity)
        assertEquals(SECURITY_OAUTH_TOKEN_URL_REQUIRED.concern, tokenUrlRequired.concern)
        assertEquals("asyncapi_validator_security_oauth_invalid.yaml", tokenUrlRequired.sourceLocation?.file?.name)
        assertEquals(
            "asyncapi_validator_security_oauth_invalid.root.components.securitySchemes.InvalidOAuthFlows.flows.clientCredentials",
            tokenUrlRequired.path,
        )
        assertEquals(18, tokenUrlRequired.line)

        val authUrlFormat =
            results.findings.single { it.code == SECURITY_OAUTH_AUTHORIZATION_URL_FORMAT.code }
        assertEquals(SECURITY_OAUTH_AUTHORIZATION_URL_FORMAT.severity, authUrlFormat.severity)
        assertEquals(SECURITY_OAUTH_AUTHORIZATION_URL_FORMAT.concern, authUrlFormat.concern)
        assertEquals("asyncapi_validator_security_oauth_invalid.yaml", authUrlFormat.sourceLocation?.file?.name)
        assertEquals(
            "asyncapi_validator_security_oauth_invalid.root.components.securitySchemes.InvalidOAuthFlows.flows.authorizationCode.authorizationUrl",
            authUrlFormat.path,
        )
        assertEquals(21, authUrlFormat.line)

        val scopeAvailable = results.findings.single { it.code == SECURITY_OAUTH_SCOPE_AVAILABLE.code }
        assertEquals(SECURITY_OAUTH_SCOPE_AVAILABLE.severity, scopeAvailable.severity)
        assertEquals(SECURITY_OAUTH_SCOPE_AVAILABLE.concern, scopeAvailable.concern)
        assertEquals("asyncapi_validator_security_oauth_invalid.yaml", scopeAvailable.sourceLocation?.file?.name)
        assertEquals(
            "asyncapi_validator_security_oauth_invalid.root.components.securitySchemes.InvalidOAuthFlows.scopes",
            scopeAvailable.path,
        )
        assertEquals(24, scopeAvailable.line)

        val openIdFormat = results.findings.single { it.code == SECURITY_OPEN_ID_URL_FORMAT.code }
        assertEquals(SECURITY_OPEN_ID_URL_FORMAT.severity, openIdFormat.severity)
        assertEquals(SECURITY_OPEN_ID_URL_FORMAT.concern, openIdFormat.concern)
        assertEquals("asyncapi_validator_security_oauth_invalid.yaml", openIdFormat.sourceLocation?.file?.name)
        assertEquals(
            "asyncapi_validator_security_oauth_invalid.root.components.securitySchemes.InvalidOpenIdUrl.openIdConnectUrl",
            openIdFormat.path,
        )
        assertEquals(28, openIdFormat.line)
    }

}
