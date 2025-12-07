package dev.banking.asyncapi.generator.core.validator.security

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.security.SecurityScheme
import dev.banking.asyncapi.generator.core.validator.util.ValidationResults
import dev.banking.asyncapi.generator.core.validator.util.ValidatorUtility.sanitizeString

class SecuritySchemeValidator(
    val asyncApiContext: AsyncApiContext,
) {

    fun validate(node: SecurityScheme, securitySchemeName: String, results: ValidationResults) {
        validateType(node, securitySchemeName, results)
        validateName(node, securitySchemeName, results)
        validateInField(node, securitySchemeName, results)
        validateScheme(node, securitySchemeName, results)
        validateBearerFormat(node, securitySchemeName, results)
        validateFlows(node, securitySchemeName, results)
        validateOpenIdConnectUrl(node, securitySchemeName, results)
        validateScopes(node, securitySchemeName, results)
    }

    private fun validateType(node: SecurityScheme, securitySchemeName: String, results: ValidationResults) {
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
                "Security Scheme '$securitySchemeName' 'type' field in SecurityScheme is required.",
                asyncApiContext.getLine(node, node::type)
            )
        } else if (type !in validTypes) {
            results.error(
                "Security Scheme '$securitySchemeName' invalid SecurityScheme type '$type'. Expected one of: ${validTypes.joinToString(", ")}",
                asyncApiContext.getLine(node, node::type)
            )
        }
    }

    private fun validateName(node: SecurityScheme, securitySchemeName: String, results: ValidationResults) {
        val type = node.type.let(::sanitizeString)
        val name = node.name?.let(::sanitizeString)
        if (type == "httpApiKey" && name.isNullOrBlank()) {
            results.error(
                "Security Scheme '$securitySchemeName' of type 'httpApiKey' requires non-empty 'name'.",
                asyncApiContext.getLine(node, node::name)
            )
        }
    }

    private fun validateInField(node: SecurityScheme, securitySchemeName: String, results: ValidationResults) {
        val type = node.type.let(::sanitizeString)
        val inField = node.inField?.let(::sanitizeString) ?: return
        val validInValues = when (type) {
            "apiKey" -> setOf("user", "password")
            "httpApiKey" -> setOf("query", "header", "cookie")
            else -> null
        } ?: return
        if (inField !in validInValues) {
            results.error(
                "Security Scheme '$securitySchemeName' invalid 'in' value '$inField' for SecurityScheme type '$type'. " +
                    "Expected one of: ${validInValues.joinToString(", ")}",
                asyncApiContext.getLine(node, node::inField)
            )
        }
    }

    private fun validateScheme(node: SecurityScheme, securitySchemeName: String, results: ValidationResults) {
        val type = node.type.let(::sanitizeString)
        val scheme = node.scheme?.let(::sanitizeString)
        if (type == "http" && scheme.isNullOrBlank()) {
            results.error(
                "Security Scheme '$securitySchemeName' of type 'http' requires non-empty 'scheme'.",
                asyncApiContext.getLine(node, node::scheme)
            )
        }
    }

    private fun validateBearerFormat(node: SecurityScheme, securitySchemeName: String, results: ValidationResults) {
        val type = node.type.let(::sanitizeString)
        val bearerFormat = node.bearerFormat?.let(::sanitizeString)
        if (type == "http" && node.scheme == "bearer" && bearerFormat.isNullOrBlank()) {
            results.warn(
                "Security Scheme '$securitySchemeName' of type 'http' with scheme 'bearer' has an empty 'bearerFormat'.",
                asyncApiContext.getLine(node, node::bearerFormat)
            )
        }
    }

    private fun validateFlows(node: SecurityScheme, securitySchemeName: String, results: ValidationResults) {
        val type = node.type.let(::sanitizeString)
        val flows = node.flows
        if (type == "oauth2") {
            if (flows == null) {
                results.error(
                    "Security Scheme '$securitySchemeName' of type 'oauth2' requires at least one OAuth2 flow " +
                        "(implicit, password, clientCredentials, or authorizationCode).",
                    asyncApiContext.getLine(node, node::flows)
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
                    "Security Scheme '$securitySchemeName' of type 'oauth2' requires at least one OAuth2 flow...",
                    asyncApiContext.getLine(node, node::flows)
                )
            }
        }
    }

    private fun validateOpenIdConnectUrl(node: SecurityScheme, securitySchemeName: String, results: ValidationResults) {
        val type = node.type.let(::sanitizeString)
        val url = node.openIdConnectUrl?.let(::sanitizeString)

        if (type == "openIdConnect") {
            if (url.isNullOrBlank()) {
                results.error(
                    "Security Scheme '$securitySchemeName' of type 'openIdConnect' must provide a valid absolute " +
                        "'openIdConnectUrl'.",
                    asyncApiContext.getLine(node, node::openIdConnectUrl)
                )
            } else {
                val urlRegex = Regex("""^(https?|wss?)://\S+$""")
                if (!urlRegex.matches(url)) {
                    results.error(
                        "Security Scheme '$securitySchemeName' of type 'openIdConnect' must provide a valid absolute " +
                            "'openIdConnectUrl'. Got '$url'.",
                        asyncApiContext.getLine(node, node::openIdConnectUrl)
                    )
                }
            }
        }
    }

    private fun validateScopes(node: SecurityScheme, securitySchemeName: String, results: ValidationResults) {
        val type = node.type.let(::sanitizeString)
        val scopes = node.scopes?.map { scope -> scope.let(::sanitizeString) } ?: return
        if (type in setOf("oauth2", "openIdConnect") && scopes.isEmpty()) {
            results.warn(
                "Security Scheme '$securitySchemeName' of type '$type' defines an empty 'scopes' list.",
                asyncApiContext.getLine(node, node::scopes)
            )
        }
    }
}
