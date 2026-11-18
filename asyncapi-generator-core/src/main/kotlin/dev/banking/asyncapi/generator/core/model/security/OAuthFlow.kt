package dev.banking.asyncapi.generator.core.model.security

import com.fasterxml.jackson.annotation.JsonAnyGetter

data class OAuthFlow(
    val authorizationUrl: String? = null,
    val tokenUrl: String? = null,
    val refreshUrl: String? = null,
    val availableScopes: Map<String, String>?,
    @get:JsonAnyGetter val extensions: Map<String, Any?>? = null,
)
