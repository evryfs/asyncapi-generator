package dev.banking.asyncapi.generator.core.model.security

import com.fasterxml.jackson.annotation.JsonAnyGetter

internal data class OAuthFlows(
    val implicit: OAuthFlow? = null,
    val password: OAuthFlow? = null,
    val clientCredentials: OAuthFlow? = null,
    val authorizationCode: OAuthFlow? = null,
    @get:JsonAnyGetter val extensions: Map<String, Any?>? = null,
)
