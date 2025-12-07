package com.tietoevry.banking.asyncapi.generator.core.model.validator

data class ValidationError(
    val message: String,
    val line: Int?,
)

