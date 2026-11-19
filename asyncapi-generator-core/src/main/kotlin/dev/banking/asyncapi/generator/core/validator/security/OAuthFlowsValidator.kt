package dev.banking.asyncapi.generator.core.validator.security

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.security.OAuthFlow
import dev.banking.asyncapi.generator.core.model.security.OAuthFlows
import dev.banking.asyncapi.generator.core.model.security.SecurityScheme
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.OAUTH_AUTHORIZATION_URL_FORMAT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.OAUTH_AUTHORIZATION_URL_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.OAUTH_REFRESH_URL_FORMAT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.OAUTH_SCOPE_AVAILABLE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.OAUTH_TOKEN_URL_FORMAT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.OAUTH_TOKEN_URL_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SECURITY_OAUTH_FLOWS
import dev.banking.asyncapi.generator.core.validator.util.ValidationCollector
import dev.banking.asyncapi.generator.core.validator.util.requireAbsoluteUri
import dev.banking.asyncapi.generator.core.validator.walk.ValidationPath

/**
 * Validates the OAuth Flows Object and its flow-specific requirements.
 *
 * @param asyncApiContext the load the security schemes belong to
 */
internal class OAuthFlowsValidator(
    private val asyncApiContext: AsyncApiContext,
) {
    /** Checks the flows of an `oauth2` security scheme [node]; other scheme types are left alone. */
    fun validate(
        node: SecurityScheme,
        path: ValidationPath,
        results: ValidationCollector,
    ) {
        if (node.type == "oauth2") validateFlows(node, path, results)
    }

    private fun validateFlows(
        node: SecurityScheme,
        path: ValidationPath,
        results: ValidationCollector,
    ) {
        val flows = node.flows?.takeIf { declaredFlows(it).isNotEmpty() }
        if (flows == null) {
            results.error(
                SECURITY_OAUTH_FLOWS,
                "$path of type 'oauth2' requires at least one OAuth2 flow.",
                sourceLocation =
                    asyncApiContext.modelTracking.getSourceLocation(node, node::flows)
                        ?: asyncApiContext.modelTracking.getSourceLocation(node),
            )
        } else if (results.visit(flows)) {
            flows.implicit?.let { validateFlow(it, path.child("Implicit"), AUTHORIZATION_URL, results) }
            flows.password?.let { validateFlow(it, path.child("Password"), TOKEN_URL, results) }
            flows.clientCredentials?.let { validateFlow(it, path.child("Client Credentials"), TOKEN_URL, results) }
            flows.authorizationCode?.let {
                validateFlow(it, path.child("Authorization Code"), AUTHORIZATION_URL + TOKEN_URL, results)
            }
            validateRequestedScopes(node, results)
        }
    }

    private fun validateFlow(
        flow: OAuthFlow,
        flowPath: ValidationPath,
        requiredUrls: List<RequiredUrl>,
        results: ValidationCollector,
    ) {
        if (!results.visit(flow)) return
        val path = flowPath.child("OAuth2 flow")
        requiredUrls.forEach { url -> validateRequiredUrl(flow, url, path, results) }
        results.requireAbsoluteUri(
            flow.refreshUrl,
            "refreshUrl",
            path,
            OAUTH_REFRESH_URL_FORMAT,
            asyncApiContext.modelTracking.getSourceLocation(flow, flow::refreshUrl),
        )
    }

    private fun validateRequiredUrl(
        flow: OAuthFlow,
        url: RequiredUrl,
        path: ValidationPath,
        results: ValidationCollector,
    ) {
        val value = url.valueOf(flow)
        if (value.isNullOrBlank()) {
            results.error(
                url.requiredRule,
                "$path must define '${url.fieldName}'.",
                sourceLocation =
                    asyncApiContext.modelTracking.getSourceLocation(flow, url.fieldName)
                        ?: asyncApiContext.modelTracking.getSourceLocation(flow),
            )
        } else {
            results.requireAbsoluteUri(
                value,
                url.fieldName,
                path,
                url.formatRule,
                asyncApiContext.modelTracking.getSourceLocation(flow, url.fieldName),
            )
        }
    }

    private fun validateRequestedScopes(
        node: SecurityScheme,
        results: ValidationCollector,
    ) {
        val requestedScopes = node.scopes ?: return
        val flows = node.flows ?: return
        val availableScopes =
            declaredFlows(flows).flatMapTo(mutableSetOf()) { flow -> flow.availableScopes.orEmpty().keys }
        val unknownScopes = requestedScopes.filterNot(availableScopes::contains)
        if (unknownScopes.isNotEmpty()) {
            results.error(
                OAUTH_SCOPE_AVAILABLE,
                "OAuth2 requested scopes $unknownScopes are not declared by any configured flow.",
                sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::scopes),
            )
        }
    }

    private fun declaredFlows(flows: OAuthFlows): List<OAuthFlow> =
        listOfNotNull(flows.implicit, flows.password, flows.clientCredentials, flows.authorizationCode)

    /**
     * A URL a flow must define as an absolute URI.
     *
     * @property fieldName the name of the flow field holding the URL
     * @property requiredRule the rule reported when the URL is missing
     * @property formatRule the rule reported when the URL is not an absolute URI
     * @property valueOf reads the URL from a flow
     */
    private class RequiredUrl(
        val fieldName: String,
        val requiredRule: ValidationRule,
        val formatRule: ValidationRule,
        val valueOf: (OAuthFlow) -> String?,
    )

    private companion object {
        val AUTHORIZATION_URL =
            listOf(
                RequiredUrl(
                    "authorizationUrl",
                    OAUTH_AUTHORIZATION_URL_REQUIRED,
                    OAUTH_AUTHORIZATION_URL_FORMAT,
                    OAuthFlow::authorizationUrl,
                ),
            )
        val TOKEN_URL =
            listOf(RequiredUrl("tokenUrl", OAUTH_TOKEN_URL_REQUIRED, OAUTH_TOKEN_URL_FORMAT, OAuthFlow::tokenUrl))
    }
}
