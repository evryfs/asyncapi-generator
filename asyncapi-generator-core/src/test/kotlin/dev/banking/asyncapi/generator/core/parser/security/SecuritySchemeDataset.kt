package dev.banking.asyncapi.generator.core.parser.security

import dev.banking.asyncapi.generator.core.model.security.OAuthFlow
import dev.banking.asyncapi.generator.core.model.security.OAuthFlows
import dev.banking.asyncapi.generator.core.model.security.SecurityScheme

fun saslScram() = SecurityScheme(
    type = "scramSha256",
    description = "Provide your username and password for SASL/SCRAM authentication",
)

fun certs() = SecurityScheme(
    type = "X509",
    description = "Download the certificate files from the service provider",
)

fun basicAuth() = SecurityScheme(
    type = "http",
    description = "Basic HTTP authentication using username and password",
    scheme = "basic",
)

fun bearerAuth() = SecurityScheme(
    type = "http",
    description = "Bearer token authentication with JWT format",
    scheme = "bearer",
    bearerFormat = "JWT",
)

fun apiKeyHeader() = SecurityScheme(
    type = "httpApiKey",
    description = "API key passed in HTTP header",
    name = "X-API-Key",
    inField = "header",
)

fun apiKeyQuery() = SecurityScheme(
    type = "httpApiKey",
    description = "API key passed in query parameter",
    name = "apiKey",
    inField = "query",
)

fun openIdConnectExample() = SecurityScheme(
    type = "openIdConnect",
    description = "OpenID Connect discovery URL example",
    openIdConnectUrl = "\"https://example.com/.well-known/openid-configuration",
)

fun oauthExample() = SecurityScheme(
    type = "oauth2",
    description = "Example OAuth2 flow",
    flows = OAuthFlows(
        implicit = OAuthFlow(
            authorizationUrl = "\"https://example.com/api/oauth/authorize",
            availableScopes = mapOf(
                "write:pets" to "modify pets in your account",
                "read:pets" to "read your pets"
            )
        ),
        password = OAuthFlow(
            tokenUrl = "\"https://example.com/api/oauth/token",
            availableScopes = mapOf(
                "admin" to "full access"
            )
        ),
        clientCredentials = OAuthFlow(
            tokenUrl = "\"https://example.com/api/oauth/token",
            availableScopes = mapOf(
                "write:docs" to "modify documents"
            )
        ),
        authorizationCode = OAuthFlow(
            authorizationUrl = "\"https://example.com/api/oauth/authorize",
            tokenUrl = "\"https://example.com/api/oauth/token",
            availableScopes = mapOf(
                "read:docs" to "read documents",
                "write:docs" to "modify documents"
            )
        )
    ),
    scopes = listOf("\"read:pets", "\"write:pets")
)
