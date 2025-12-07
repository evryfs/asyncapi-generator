package dev.banking.asyncapi.generator.core.model.correlations

data class CorrelationId(
    val location: String,
    val description: String? = null
)
