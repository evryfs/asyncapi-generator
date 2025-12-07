package com.tietoevry.banking.asyncapi.generator.core.model.operations

import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.tietoevry.banking.asyncapi.generator.core.model.references.Reference

sealed interface OperationInterface {

    data class OperationInline(
        @get:JsonUnwrapped
        val operation: Operation,
    ) : OperationInterface

    data class OperationReference(
        @get:JsonUnwrapped
        val reference: Reference,
    ) : OperationInterface
}
