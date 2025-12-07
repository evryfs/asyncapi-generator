package com.tietoevry.banking.asyncapi.generator.core.parser.correlations

import com.tietoevry.banking.asyncapi.generator.core.model.correlations.CorrelationId

fun myCorrelationId() = CorrelationId(
    location = $$"\"$message.header#/correlationId",
    description = "\"My custom correlation ID"
)
