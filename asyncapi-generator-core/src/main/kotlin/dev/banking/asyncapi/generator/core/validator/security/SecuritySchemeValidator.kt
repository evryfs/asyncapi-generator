package dev.banking.asyncapi.generator.core.validator.security

import dev.banking.asyncapi.generator.core.constants.RegexPatterns.URL
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.security.SecurityScheme
import dev.banking.asyncapi.generator.core.validator.util.ValidationResults
import dev.banking.asyncapi.generator.core.validator.util.ValidatorUtility.sanitizeString

class SecuritySchemeValidator(
    val asyncApiContext: AsyncApiContext,
) {

    fun validate(node: SecurityScheme, contextString: String, results: ValidationResults) {
        validateType(node, contextString, results)
        validateName(node, contextString, results)
        validateInField(node, contextString, results)
        validateScheme(node, contextString, results)
        validateBearerFormat(node, contextString, results)
        validateFlows(node, contextString, results)
        validateOpenIdConnectUrl(node, contextString, results)
    }

    private fun validateType(node: SecurityScheme, contextString: String, results: ValidationResults) {
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
                "$contextString 'type' field in SecurityScheme is required.",
                asyncApiContext.getLine(node, node::type),
                "https://www.asyncapi.com/docs/reference/specification/v3.0.0#securitySchemeObject",
            )
        } else if (type !in validTypes) {
            results.error(
                "$contextString invalid type '$type'. Expected one of: ${validTypes.joinToString(", ")}",
                asyncApiContext.getLine(node, node::type),
                "https://www.asyncapi.com/docs/reference/specification/v3.0.0#securitySchemeObject",
            )
        }
    }

    private fun validateName(node: SecurityScheme, contextString: String, results: ValidationResults) {
        val type = node.type.let(::sanitizeString)
        val name = node.name?.let(::sanitizeString)
        if (type == "httpApiKey" && name.isNullOrBlank()) {
            results.error(
                "$contextString of type 'httpApiKey' requires non-empty 'name'.",
                asyncApiContext.getLine(node, node::name),
                "https://www.asyncapi.com/docs/reference/specification/v3.0.0#securitySchemeObject",
            )
        }
    }

    private fun validateInField(node: SecurityScheme, contextString: String, results: ValidationResults) {
        val type = node.type.let(::sanitizeString)
        val inField = node.inField?.let(::sanitizeString) ?: return
        val validInValues = when (type) {
            "apiKey" -> setOf("user", "password")
            "httpApiKey" -> setOf("query", "header", "cookie")
            else -> null
        } ?: return
        if (inField !in validInValues) {
            results.error(
                "$contextString invalid 'in' value '$inField' for type '$type'. Expected one of: ${validInValues.joinToString(", ")}",
                asyncApiContext.getLine(node, node::inField),
                "https://www.asyncapi.com/docs/reference/specification/v3.0.0#securitySchemeObject",
            )
        }
    }

    private fun validateScheme(node: SecurityScheme, contextString: String, results: ValidationResults) {
        val type = node.type.let(::sanitizeString)
        val scheme = node.scheme?.let(::sanitizeString)
        if (type == "http" && scheme.isNullOrBlank()) {
            results.error(
                "$contextString of type 'http' requires non-empty 'scheme'.",
                asyncApiContext.getLine(node, node::scheme),
                "https://www.asyncapi.com/docs/reference/specification/v3.0.0#securitySchemeObject",
            )
        }
    }

    private fun validateBearerFormat(node: SecurityScheme, contextString: String, results: ValidationResults) {
        val type = node.type.let(::sanitizeString)
        val bearerFormat = node.bearerFormat?.let(::sanitizeString)
        if (type == "http" && node.scheme == "bearer" && bearerFormat.isNullOrBlank()) {
            results.warn(
                "$contextString of type 'http' with scheme 'bearer' has an empty 'bearerFormat'.",
                asyncApiContext.getLine(node, node::bearerFormat)
            )
        }
    }

    private fun validateFlows(node: SecurityScheme, contextString: String, results: ValidationResults) {
        val type = node.type.let(::sanitizeString)
        val flows = node.flows
        if (type == "oauth2") {
            if (flows == null) {
                results.error(
                    "$contextString of type 'oauth2' requires at least one OAuth2 flow (implicit, password, " +
                        "clientCredentials, or authorizationCode).",
                    asyncApiContext.getLine(node, node::flows),
                    "https://www.asyncapi.com/docs/reference/specification/v3.0.0#securitySchemeObject",
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
                    "$contextString of type 'oauth2' requires at least one OAuth2 flow.",
                    asyncApiContext.getLine(node, node::flows),
                    "https://www.asyncapi.com/docs/reference/specification/v3.0.0#securitySchemeObject",
                )
            }
        }
    }

    private fun validateOpenIdConnectUrl(node: SecurityScheme, contextString: String, results: ValidationResults) {
        val type = node.type.let(::sanitizeString)
        val url = node.openIdConnectUrl?.let(::sanitizeString)

        if (type == "openIdConnect") {
            if (url.isNullOrBlank()) {
                results.error(
                    "$contextString of type 'openIdConnect' must provide a valid absolute 'openIdConnectUrl'.",
                    asyncApiContext.getLine(node, node::openIdConnectUrl),
                    "https://www.asyncapi.com/docs/reference/specification/v3.0.0#securitySchemeObject",
                )
            } else {
                if (!URL.matches(url)) {
                    results.error(
                        "$contextString of type 'openIdConnect' must provide a valid absolute 'openIdConnectUrl'. " +
                            "Got '$url'.",
                        asyncApiContext.getLine(node, node::openIdConnectUrl),
                        "https://www.asyncapi.com/docs/reference/specification/v3.0.0#securitySchemeObject",
                    )
                }
            }
        }
    }
}
