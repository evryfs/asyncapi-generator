package dev.banking.asyncapi.generator.core.model.servers

import com.fasterxml.jackson.annotation.JsonUnwrapped
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.references.ReferenceOr

/** A Server Object written inline or as a `$ref`. */
sealed interface ServerInterface : ReferenceOr<Server> {
    /**
     * A Server Object written inline.
     *
     * @property server the object
     */
    data class ServerInline(
        @get:JsonUnwrapped
        val server: Server,
    ) : ServerInterface {
        override fun resolvedOrNull(): Server = server

        override fun held(): Any = server
    }

    /**
     * A `$ref` to a Server Object.
     *
     * @property reference the reference
     */
    data class ServerReference(
        @get:JsonUnwrapped
        val reference: Reference,
    ) : ServerInterface {
        override fun resolvedOrNull(): Server? = reference.finalModel() as? Server

        override fun held(): Any = reference
    }
}
