package dev.banking.asyncapi.generator.core.model.parameters

import com.fasterxml.jackson.annotation.JsonUnwrapped
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.references.ReferenceOr

/** A Parameter Object written inline or as a `$ref`. */
sealed interface ParameterInterface : ReferenceOr<Parameter> {
    /**
     * A Parameter Object written inline.
     *
     * @property parameter the object
     */
    data class ParameterInline(
        @get:JsonUnwrapped
        val parameter: Parameter,
    ) : ParameterInterface {
        override fun resolvedOrNull(): Parameter = parameter

        override fun held(): Any = parameter
    }

    /**
     * A `$ref` to a Parameter Object.
     *
     * @property reference the reference
     */
    data class ParameterReference(
        @get:JsonUnwrapped
        val reference: Reference,
    ) : ParameterInterface {
        override fun resolvedOrNull(): Parameter? = reference.finalModel() as? Parameter

        override fun held(): Any = reference
    }
}
