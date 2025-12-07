package dev.banking.asyncapi.generator.core.model.operations

import com.fasterxml.jackson.annotation.JsonUnwrapped
import dev.banking.asyncapi.generator.core.model.references.Reference

sealed interface OperationTraitInterface {

    data class OperationTraitInline(
        @get:JsonUnwrapped
        val operationTrait: OperationTrait,
    ) : OperationTraitInterface

    data class OperationTraitReference(
        @get:JsonUnwrapped
        val reference: Reference,
    ) : OperationTraitInterface
}
