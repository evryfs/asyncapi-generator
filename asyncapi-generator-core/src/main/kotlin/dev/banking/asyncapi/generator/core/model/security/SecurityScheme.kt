package dev.banking.asyncapi.generator.core.model.security

import com.fasterxml.jackson.annotation.JsonProperty

data class SecurityScheme(
    val type: String,
    val description: String? = null,
    val name: String? = null,
    @get:JsonProperty("in") val inField: String? = null,
    val scheme: String? = null,
    val bearerFormat: String? = null,
    val openIdConnectUrl: String? = null,
    val flows: OAuthFlows? = null,
    val scopes: List<String>? = null,
)
