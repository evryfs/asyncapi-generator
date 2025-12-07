package com.tietoevry.banking.asyncapi.generator.core.model.correlations

import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.tietoevry.banking.asyncapi.generator.core.model.references.Reference

sealed interface CorrelationIdInterface {

    data class CorrelationIdInline(
        @get:JsonUnwrapped
        val correlationId: CorrelationId,
    ) : CorrelationIdInterface

    data class CorrelationIdReference(
        @get:JsonUnwrapped val reference: Reference,
    ) : CorrelationIdInterface
}
