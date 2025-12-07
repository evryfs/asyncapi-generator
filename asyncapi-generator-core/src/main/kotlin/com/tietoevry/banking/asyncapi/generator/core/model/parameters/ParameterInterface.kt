package com.tietoevry.banking.asyncapi.generator.core.model.parameters

import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.tietoevry.banking.asyncapi.generator.core.model.references.Reference

sealed interface ParameterInterface {

    data class ParameterInline(
        @get:JsonUnwrapped
        val parameter: Parameter,
    ) : ParameterInterface

    data class ParameterReference(
        @get:JsonUnwrapped
        val reference: Reference,
    ) : ParameterInterface
}
