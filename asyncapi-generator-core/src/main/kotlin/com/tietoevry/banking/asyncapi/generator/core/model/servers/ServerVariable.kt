package com.tietoevry.banking.asyncapi.generator.core.model.servers

data class ServerVariable(
    val enum: List<String>? = null,
    val default: String? = null,
    val description: String? = null,
    val examples: List<String>? = null,
)
