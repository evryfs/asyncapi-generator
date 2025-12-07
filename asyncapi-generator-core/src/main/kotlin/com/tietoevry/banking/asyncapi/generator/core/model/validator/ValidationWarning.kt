package com.tietoevry.banking.asyncapi.generator.core.model.validator

data class ValidationWarning(
    val message: String,
    val line: Int?,
)
