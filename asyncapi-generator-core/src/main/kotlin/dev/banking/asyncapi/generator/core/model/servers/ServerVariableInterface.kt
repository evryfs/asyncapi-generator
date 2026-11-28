package dev.banking.asyncapi.generator.core.model.servers

import com.fasterxml.jackson.annotation.JsonUnwrapped
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.references.ReferenceOr

/** A Server Variable Object written inline or as a `$ref`. */
internal sealed interface ServerVariableInterface : ReferenceOr<ServerVariable> {
    /**
     * A Server Variable Object written inline.
     *
     * @property serverVariable the object
     */
    data class ServerVariableInline(
        @get:JsonUnwrapped
        val serverVariable: ServerVariable,
    ) : ServerVariableInterface {
        override fun resolvedOrNull(): ServerVariable = serverVariable

        override fun held(): Any = serverVariable
    }

    /**
     * A `$ref` to a Server Variable Object.
     *
     * @property reference the reference
     */
    data class ServerVariableReference(
        @get:JsonUnwrapped
        val reference: Reference,
    ) : ServerVariableInterface {
        override fun resolvedOrNull(): ServerVariable? = reference.finalModel() as? ServerVariable

        override fun held(): Any = reference
    }
}
