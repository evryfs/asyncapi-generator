package dev.banking.asyncapi.generator.core.validator.security

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.security.SecurityScheme
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SECURITY_IN_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SECURITY_IN_VALUE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SECURITY_NAME_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SECURITY_OPENID_URL_FORMAT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SECURITY_OPENID_URL_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SECURITY_SCHEME_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SECURITY_TYPE_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SECURITY_TYPE_VALUE
import dev.banking.asyncapi.generator.core.validator.util.ValidationCollector
import dev.banking.asyncapi.generator.core.validator.util.ValidationFormats
import dev.banking.asyncapi.generator.core.validator.walk.ObjectRules
import dev.banking.asyncapi.generator.core.validator.walk.ValidationPath
import dev.banking.asyncapi.generator.core.validator.walk.ValidationWalk

/**
 * Validates the Security Scheme Object, including its OAuth flows.
 *
 * @param asyncApiContext the load the security schemes belong to
 */
internal class SecuritySchemeRules(
    private val asyncApiContext: AsyncApiContext,
) : ObjectRules<SecurityScheme> {
    private val oAuthFlowsValidator = OAuthFlowsValidator(asyncApiContext)

    override fun check(
        node: SecurityScheme,
        path: ValidationPath,
        walk: ValidationWalk,
    ) {
        val results = walk.results
        validateType(node, path, results)
        validateName(node, path, results)
        validateInField(node, path, results)
        validateScheme(node, path, results)
        oAuthFlowsValidator.validate(node, path, results)
        validateOpenIdConnectUrl(node, path, results)
    }

    private fun validateType(
        node: SecurityScheme,
        path: ValidationPath,
        results: ValidationCollector,
    ) {
        val validTypes =
            setOf(
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
                "gssapi",
            )
        val type = node.type
        if (type.isBlank()) {
            results.error(
                SECURITY_TYPE_REQUIRED,
                "$path 'type' field in SecurityScheme is required.",
                sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::type),
            )
        } else if (type !in validTypes) {
            results.error(
                SECURITY_TYPE_VALUE,
                "$path invalid type '$type'. Expected one of: ${validTypes.joinToString(", ")}",
                sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::type),
            )
        }
    }

    private fun validateName(
        node: SecurityScheme,
        path: ValidationPath,
        results: ValidationCollector,
    ) {
        if (node.type == "httpApiKey" && node.name.isNullOrBlank()) {
            results.error(
                SECURITY_NAME_REQUIRED,
                "$path of type 'httpApiKey' requires non-empty 'name'.",
                sourceLocation =
                    asyncApiContext.modelTracking.getSourceLocation(node, node::name)
                        ?: asyncApiContext.modelTracking.getSourceLocation(node),
            )
        }
    }

    private fun validateInField(
        node: SecurityScheme,
        path: ValidationPath,
        results: ValidationCollector,
    ) {
        val validInValues =
            when (node.type) {
                "apiKey" -> setOf("user", "password")
                "httpApiKey" -> setOf("query", "header", "cookie")
                else -> null
            } ?: return
        val inField = node.inField
        if (inField.isNullOrBlank()) {
            results.error(
                SECURITY_IN_REQUIRED,
                "$path of type '${node.type}' requires a non-empty 'in' field.",
                sourceLocation =
                    asyncApiContext.modelTracking.getSourceLocation(node, node::inField)
                        ?: asyncApiContext.modelTracking.getSourceLocation(node),
            )
        } else if (inField !in validInValues) {
            results.error(
                SECURITY_IN_VALUE,
                "$path invalid 'in' value '$inField' for type '${node.type}'. " +
                    "Expected one of: ${validInValues.joinToString(", ")}",
                sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::inField),
            )
        }
    }

    private fun validateScheme(
        node: SecurityScheme,
        path: ValidationPath,
        results: ValidationCollector,
    ) {
        if (node.type == "http" && node.scheme.isNullOrBlank()) {
            results.error(
                SECURITY_SCHEME_REQUIRED,
                "$path of type 'http' requires non-empty 'scheme'.",
                sourceLocation =
                    asyncApiContext.modelTracking.getSourceLocation(node, node::scheme)
                        ?: asyncApiContext.modelTracking.getSourceLocation(node),
            )
        }
    }

    private fun validateOpenIdConnectUrl(
        node: SecurityScheme,
        path: ValidationPath,
        results: ValidationCollector,
    ) {
        if (node.type == "openIdConnect") {
            val url = node.openIdConnectUrl
            if (url.isNullOrBlank()) {
                results.error(
                    SECURITY_OPENID_URL_REQUIRED,
                    "$path of type 'openIdConnect' must provide a valid absolute 'openIdConnectUrl'.",
                    sourceLocation =
                        asyncApiContext.modelTracking.getSourceLocation(node, node::openIdConnectUrl)
                            ?: asyncApiContext.modelTracking.getSourceLocation(node),
                )
            } else {
                if (ValidationFormats.absoluteUri(url) == null) {
                    results.error(
                        SECURITY_OPENID_URL_FORMAT,
                        "$path of type 'openIdConnect' must provide a valid absolute 'openIdConnectUrl'. " +
                            "Got '$url'.",
                        sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::openIdConnectUrl),
                    )
                }
            }
        }
    }
}
