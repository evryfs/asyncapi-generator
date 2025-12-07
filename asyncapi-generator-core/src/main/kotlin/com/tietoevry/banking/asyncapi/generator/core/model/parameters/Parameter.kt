package com.tietoevry.banking.asyncapi.generator.core.model.parameters

data class Parameter(
    val description: String? = null,
    val location: String? = null,
    val enum: List<String>? = null,
    val default: String? = null,
    val examples: List<String>? = null,
)


