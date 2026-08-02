package dev.banking.asyncapi.generator.core.validator.security

import dev.banking.asyncapi.generator.core.constants.RegexPatterns.URL
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.security.SecurityScheme
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SECURITY_BEARER_FORMAT_EMPTY
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SECURITY_IN_VALUE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SECURITY_NAME_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SECURITY_OAUTH_FLOWS_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SECURITY_OPEN_ID_URL_FORMAT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SECURITY_OPEN_ID_URL_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SECURITY_SCHEME_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SECURITY_TYPE_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SECURITY_TYPE_VALUE
import dev.banking.asyncapi.generator.core.validator.util.ValidationCollector
import dev.banking.asyncapi.generator.core.validator.util.ValidatorUtility.sanitizeString

class SecuritySchemeValidator(
    val asyncApiContext: AsyncApiContext,
) {

    fun validate(node: SecurityScheme, contextString: String, results: ValidationCollector) {
        if (!results.visit(node)) return
        validateType(node, contextString, results)
        validateName(node, contextString, results)
        validateInField(node, contextString, results)
        validateScheme(node, contextString, results)
        validateBearerFormat(node, contextString, results)
        validateFlows(node, contextString, results)
        validateOpenIdConnectUrl(node, contextString, results)
    }

    private fun validateType(node: SecurityScheme, contextString: String, results: ValidationCollector) {
        val validTypes = setOf(
            "userPassword",
            "apiKey",
            "X509",
            "symmetricEncryption",
            "asymmetricEncryption",
            "httpApiKey",
            "http",
            "oauth2",
            "openIdConnect",
            "plain",
            "scramSha256",
            "scramSha512",
            "gssapi"
        )
        val type = node.type.let(::sanitizeString)
        if (type.isBlank()) {
            results.error(
                SECURITY_TYPE_REQUIRED,
                "$contextString 'type' field in SecurityScheme is required.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::type),
                doc = "https://www.asyncapi.com/docs/reference/specification/v3.0.0#securitySchemeObject",
            )
        } else if (type !in validTypes) {
            results.error(
                SECURITY_TYPE_VALUE,
                "$contextString invalid type '$type'. Expected one of: ${validTypes.joinToString(", ")}",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::type),
                doc = "https://www.asyncapi.com/docs/reference/specification/v3.0.0#securitySchemeObject",
            )
        }
    }

    private fun validateName(node: SecurityScheme, contextString: String, results: ValidationCollector) {
        val type = node.type.let(::sanitizeString)
        val name = node.name?.let(::sanitizeString)
        if (type == "httpApiKey" && name.isNullOrBlank()) {
            results.error(
                SECURITY_NAME_REQUIRED,
                "$contextString of type 'httpApiKey' requires non-empty 'name'.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::name),
                doc = "https://www.asyncapi.com/docs/reference/specification/v3.0.0#securitySchemeObject",
            )
        }
    }

    private fun validateInField(node: SecurityScheme, contextString: String, results: ValidationCollector) {
        val type = node.type.let(::sanitizeString)
        val inField = node.inField?.let(::sanitizeString) ?: return
        val validInValues = when (type) {
            "apiKey" -> setOf("user", "password")
            "httpApiKey" -> setOf("query", "header", "cookie")
            else -> null
        } ?: return
        if (inField !in validInValues) {
            results.error(
                SECURITY_IN_VALUE,
                "$contextString invalid 'in' value '$inField' for type '$type'. Expected one of: ${validInValues.joinToString(", ")}",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::inField),
                doc = "https://www.asyncapi.com/docs/reference/specification/v3.0.0#securitySchemeObject",
            )
        }
    }

    private fun validateScheme(node: SecurityScheme, contextString: String, results: ValidationCollector) {
        val type = node.type.let(::sanitizeString)
        val scheme = node.scheme?.let(::sanitizeString)
        if (type == "http" && scheme.isNullOrBlank()) {
            results.error(
                SECURITY_SCHEME_REQUIRED,
                "$contextString of type 'http' requires non-empty 'scheme'.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::scheme),
                doc = "https://www.asyncapi.com/docs/reference/specification/v3.0.0#securitySchemeObject",
            )
        }
    }

    private fun validateBearerFormat(node: SecurityScheme, contextString: String, results: ValidationCollector) {
        val type = node.type.let(::sanitizeString)
        val bearerFormat = node.bearerFormat?.let(::sanitizeString)
        if (type == "http" && node.scheme == "bearer" && bearerFormat.isNullOrBlank()) {
            results.warn(
                SECURITY_BEARER_FORMAT_EMPTY,
                "$contextString of type 'http' with scheme 'bearer' has an empty 'bearerFormat'.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::bearerFormat),
            )
        }
    }

    private fun validateFlows(node: SecurityScheme, contextString: String, results: ValidationCollector) {
        val type = node.type.let(::sanitizeString)
        val flows = node.flows
        if (type == "oauth2") {
            if (flows == null) {
                results.error(
                    SECURITY_OAUTH_FLOWS_REQUIRED,
                    "$contextString of type 'oauth2' requires at least one OAuth2 flow (implicit, password, " +
                        "clientCredentials, or authorizationCode).",
                    sourceLocation = asyncApiContext.getSourceLocation(node, node::flows),
                    doc = "https://www.asyncapi.com/docs/reference/specification/v3.0.0#securitySchemeObject",
                )
                return
            }
            if (
                flows.implicit == null &&
                flows.password == null &&
                flows.clientCredentials == null &&
                flows.authorizationCode == null
            ) {
                results.error(
                    SECURITY_OAUTH_FLOWS_REQUIRED,
                    "$contextString of type 'oauth2' requires at least one OAuth2 flow.",
                    sourceLocation = asyncApiContext.getSourceLocation(node, node::flows),
                    doc = "https://www.asyncapi.com/docs/reference/specification/v3.0.0#securitySchemeObject",
                )
            }
        }
    }

    private fun validateOpenIdConnectUrl(node: SecurityScheme, contextString: String, results: ValidationCollector) {
        val type = node.type.let(::sanitizeString)
        val url = node.openIdConnectUrl?.let(::sanitizeString)

        if (type == "openIdConnect") {
            if (url.isNullOrBlank()) {
                results.error(
                    SECURITY_OPEN_ID_URL_REQUIRED,
                    "$contextString of type 'openIdConnect' must provide a valid absolute 'openIdConnectUrl'.",
                    sourceLocation = asyncApiContext.getSourceLocation(node, node::openIdConnectUrl),
                    doc = "https://www.asyncapi.com/docs/reference/specification/v3.0.0#securitySchemeObject",
                )
            } else {
                if (!URL.matches(url)) {
                    results.error(
                        SECURITY_OPEN_ID_URL_FORMAT,
                        "$contextString of type 'openIdConnect' must provide a valid absolute 'openIdConnectUrl'. " +
                            "Got '$url'.",
                        sourceLocation = asyncApiContext.getSourceLocation(node, node::openIdConnectUrl),
                        doc = "https://www.asyncapi.com/docs/reference/specification/v3.0.0#securitySchemeObject",
                    )
                }
            }
        }
    }
}
