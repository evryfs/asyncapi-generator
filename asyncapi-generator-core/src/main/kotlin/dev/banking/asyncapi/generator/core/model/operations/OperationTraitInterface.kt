package dev.banking.asyncapi.generator.core.model.operations

import com.fasterxml.jackson.annotation.JsonUnwrapped
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.references.ReferenceOr

/** A Operation Trait Object written inline or as a `$ref`. */
internal sealed interface OperationTraitInterface : ReferenceOr<OperationTrait> {
    /**
     * A Operation Trait Object written inline.
     *
     * @property operationTrait the object
     */
    data class OperationTraitInline(
        @get:JsonUnwrapped
        val operationTrait: OperationTrait,
    ) : OperationTraitInterface {
        override fun resolvedOrNull(): OperationTrait = operationTrait

        override fun held(): Any = operationTrait
    }

    /**
     * A `$ref` to a Operation Trait Object.
     *
     * @property reference the reference
     */
    data class OperationTraitReference(
        @get:JsonUnwrapped
        val reference: Reference,
    ) : OperationTraitInterface {
        override fun resolvedOrNull(): OperationTrait? = reference.finalModel() as? OperationTrait

        override fun held(): Any = reference
    }
}
