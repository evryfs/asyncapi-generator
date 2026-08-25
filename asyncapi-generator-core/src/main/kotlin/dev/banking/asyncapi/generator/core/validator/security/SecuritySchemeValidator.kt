package dev.banking.asyncapi.generator.core.validator.security

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.security.SecuritySchemeInterface
import dev.banking.asyncapi.generator.core.model.security.SecurityScheme
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.SECURITY_SCHEME
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SECURITY_IN_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SECURITY_IN_VALUE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SECURITY_NAME_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SECURITY_OPEN_ID_URL_FORMAT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SECURITY_OPEN_ID_URL_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SECURITY_SCHEME_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SECURITY_TYPE_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SECURITY_TYPE_VALUE
import dev.banking.asyncapi.generator.core.resolver.ReferenceResolver
import dev.banking.asyncapi.generator.core.validator.util.ValidationCollector
import dev.banking.asyncapi.generator.core.validator.util.ValidationFormats

internal class SecuritySchemeValidator(
    val asyncApiContext: AsyncApiContext,
) {
    private val referenceResolver = ReferenceResolver(asyncApiContext)
    private val oAuthFlowsValidator = OAuthFlowsValidator(asyncApiContext)

    fun validateInterface(node: SecuritySchemeInterface, contextString: String, results: ValidationCollector) {
        when (node) {
            is SecuritySchemeInterface.SecuritySchemeInline ->
                validate(node.security, contextString, results)

            is SecuritySchemeInterface.SecuritySchemeReference ->
                referenceResolver.resolve(node.reference, SECURITY_SCHEME, contextString, results)
        }
    }

    fun validateList(
        securitySchemeInterfaces: List<SecuritySchemeInterface>,
        contextString: String,
        results: ValidationCollector,
    ) {
        securitySchemeInterfaces.forEachIndexed { index, securitySchemeInterface ->
            val securitySchemeContext = "$contextString [index=$index]"
            validateInterface(securitySchemeInterface, securitySchemeContext, results)
        }
    }

    fun validate(node: SecurityScheme, contextString: String, results: ValidationCollector) {
        if (!results.visit(node)) return
        validateType(node, contextString, results)
        validateName(node, contextString, results)
        validateInField(node, contextString, results)
        validateScheme(node, contextString, results)
        oAuthFlowsValidator.validate(node, contextString, results)
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
        val type = node.type
        if (type.isEmpty()) {
            results.error(
                SECURITY_TYPE_REQUIRED,
                "$contextString 'type' field in SecurityScheme is required.",
                sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::type),
                doc = "https://www.asyncapi.com/docs/reference/specification/v3.0.0#securitySchemeObject",
            )
        } else if (type !in validTypes) {
            results.error(
                SECURITY_TYPE_VALUE,
                "$contextString invalid type '$type'. Expected one of: ${validTypes.joinToString(", ")}",
                sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::type),
                doc = "https://www.asyncapi.com/docs/reference/specification/v3.0.0#securitySchemeObject",
            )
        }
    }

    private fun validateName(node: SecurityScheme, contextString: String, results: ValidationCollector) {
        if (node.type == "httpApiKey" && node.name.isNullOrEmpty()) {
            results.error(
                SECURITY_NAME_REQUIRED,
                "$contextString of type 'httpApiKey' requires non-empty 'name'.",
                sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::name)
                    ?: asyncApiContext.modelTracking.getSourceLocation(node),
                doc = "https://www.asyncapi.com/docs/reference/specification/v3.0.0#securitySchemeObject",
            )
        }
    }

    private fun validateInField(node: SecurityScheme, contextString: String, results: ValidationCollector) {
        val validInValues = when (node.type) {
            "apiKey" -> setOf("user", "password")
            "httpApiKey" -> setOf("query", "header", "cookie")
            else -> null
        } ?: return
        val inField = node.inField
        if (inField.isNullOrEmpty()) {
            results.error(
                SECURITY_IN_REQUIRED,
                "$contextString of type '${node.type}' requires a non-empty 'in' field.",
                sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::inField)
                    ?: asyncApiContext.modelTracking.getSourceLocation(node),
            )
        } else if (inField !in validInValues) {
            results.error(
                SECURITY_IN_VALUE,
                "$contextString invalid 'in' value '$inField' for type '${node.type}'. " +
                    "Expected one of: ${validInValues.joinToString(", ")}",
                sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::inField),
            )
        }
    }

    private fun validateScheme(node: SecurityScheme, contextString: String, results: ValidationCollector) {
        if (node.type == "http" && node.scheme.isNullOrEmpty()) {
            results.error(
                SECURITY_SCHEME_REQUIRED,
                "$contextString of type 'http' requires non-empty 'scheme'.",
                sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::scheme)
                    ?: asyncApiContext.modelTracking.getSourceLocation(node),
                doc = "https://www.asyncapi.com/docs/reference/specification/v3.0.0#securitySchemeObject",
            )
        }
    }

    private fun validateOpenIdConnectUrl(node: SecurityScheme, contextString: String, results: ValidationCollector) {
        if (node.type == "openIdConnect") {
            val url = node.openIdConnectUrl
            if (url.isNullOrEmpty()) {
                results.error(
                    SECURITY_OPEN_ID_URL_REQUIRED,
                    "$contextString of type 'openIdConnect' must provide a valid absolute 'openIdConnectUrl'.",
                    sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::openIdConnectUrl)
                        ?: asyncApiContext.modelTracking.getSourceLocation(node),
                    doc = "https://www.asyncapi.com/docs/reference/specification/v3.0.0#securitySchemeObject",
                )
            } else {
                if (ValidationFormats.absoluteUri(url) == null) {
                    results.error(
                        SECURITY_OPEN_ID_URL_FORMAT,
                        "$contextString of type 'openIdConnect' must provide a valid absolute 'openIdConnectUrl'. " +
                            "Got '$url'.",
                        sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::openIdConnectUrl),
                        doc = "https://www.asyncapi.com/docs/reference/specification/v3.0.0#securitySchemeObject",
                    )
                }
            }
        }
    }
}
