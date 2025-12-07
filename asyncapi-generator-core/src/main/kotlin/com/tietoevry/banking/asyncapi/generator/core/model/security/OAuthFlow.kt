package com.tietoevry.banking.asyncapi.generator.core.model.security

data class OAuthFlow(
    val authorizationUrl: String? = null,
    val tokenUrl: String? = null,
    val refreshUrl: String? = null,
    val availableScopes: Map<String, String>?
)

