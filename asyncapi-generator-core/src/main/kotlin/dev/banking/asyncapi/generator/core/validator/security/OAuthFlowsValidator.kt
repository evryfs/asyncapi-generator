package dev.banking.asyncapi.generator.core.validator.security

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.security.OAuthFlow
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
import dev.banking.asyncapi.generator.core.validator.util.ValidationFormats
import dev.banking.asyncapi.generator.core.validator.util.requireAbsoluteUri
import dev.banking.asyncapi.generator.core.validator.walk.ValidationPath

/** Validates the OAuth Flows Object and its flow-specific requirements. */
internal class OAuthFlowsValidator(
    private val asyncApiContext: AsyncApiContext,
) {
    fun validate(
        node: SecurityScheme,
        path: ValidationPath,
        results: ValidationCollector,
    ) {
        if (node.type != "oauth2") return
        val flows = node.flows
        if (
            flows == null ||
            (
                flows.implicit == null &&
                    flows.password == null &&
                    flows.clientCredentials == null &&
                    flows.authorizationCode == null
            )
        ) {
            results.error(
                SECURITY_OAUTH_FLOWS,
                "$path of type 'oauth2' requires at least one OAuth2 flow.",
                sourceLocation =
                    asyncApiContext.modelTracking.getSourceLocation(node, node::flows)
                        ?: asyncApiContext.modelTracking.getSourceLocation(node),
            )
            return
        }
        if (!results.visit(flows)) return

        flows.implicit?.let { flow ->
            validateFlow(
                flow,
                path.child("Implicit"),
                requiresAuthorizationUrl = true,
                requiresTokenUrl = false,
                results = results,
            )
        }
        flows.password?.let { flow ->
            validateFlow(
                flow,
                path.child("Password"),
                requiresAuthorizationUrl = false,
                requiresTokenUrl = true,
                results = results,
            )
        }
        flows.clientCredentials?.let { flow ->
            validateFlow(
                flow,
                path.child("Client Credentials"),
                requiresAuthorizationUrl = false,
                requiresTokenUrl = true,
                results = results,
            )
        }
        flows.authorizationCode?.let { flow ->
            validateFlow(
                flow,
                path.child("Authorization Code"),
                requiresAuthorizationUrl = true,
                requiresTokenUrl = true,
                results = results,
            )
        }
        validateRequestedScopes(node, results)
    }

    private fun validateFlow(
        flow: OAuthFlow,
        flowPath: ValidationPath,
        requiresAuthorizationUrl: Boolean,
        requiresTokenUrl: Boolean,
        results: ValidationCollector,
    ) {
        if (!results.visit(flow)) return
        val path = flowPath.child("OAuth2 flow")
        if (requiresAuthorizationUrl) {
            validateRequiredUrl(
                flow,
                flow.authorizationUrl,
                "authorizationUrl",
                OAUTH_AUTHORIZATION_URL_REQUIRED,
                OAUTH_AUTHORIZATION_URL_FORMAT,
                path,
                results,
            )
        }
        if (requiresTokenUrl) {
            validateRequiredUrl(
                flow,
                flow.tokenUrl,
                "tokenUrl",
                OAUTH_TOKEN_URL_REQUIRED,
                OAUTH_TOKEN_URL_FORMAT,
                path,
                results,
            )
        }
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
        value: String?,
        fieldName: String,
        requiredRule: ValidationRule,
        formatRule: ValidationRule,
        path: ValidationPath,
        results: ValidationCollector,
    ) {
        if (value.isNullOrBlank()) {
            results.error(
                requiredRule,
                "$path must define '$fieldName'.",
                sourceLocation =
                    asyncApiContext.modelTracking.getSourceLocation(flow, fieldName)
                        ?: asyncApiContext.modelTracking.getSourceLocation(flow),
            )
        } else {
            results.requireAbsoluteUri(
                value,
                fieldName,
                path,
                formatRule,
                asyncApiContext.modelTracking.getSourceLocation(flow, fieldName),
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
            listOfNotNull(
                flows.implicit,
                flows.password,
                flows.clientCredentials,
                flows.authorizationCode,
            ).flatMapTo(mutableSetOf()) { flow -> flow.availableScopes.orEmpty().keys }
        val unknownScopes = requestedScopes.filterNot(availableScopes::contains)
        if (unknownScopes.isNotEmpty()) {
            results.error(
                OAUTH_SCOPE_AVAILABLE,
                "OAuth2 requested scopes $unknownScopes are not declared by any configured flow.",
                sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::scopes),
            )
        }
    }
}
