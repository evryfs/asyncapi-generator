package dev.banking.asyncapi.generator.core.parser.correlations

import dev.banking.asyncapi.generator.core.model.correlations.CorrelationId

fun myCorrelationId() = CorrelationId(
    location = $$"\"$message.header#/correlationId",
    description = "\"My custom correlation ID"
)
