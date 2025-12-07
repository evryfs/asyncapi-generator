package dev.banking.asyncapi.generator.core.model.servers

import com.fasterxml.jackson.annotation.JsonUnwrapped
import dev.banking.asyncapi.generator.core.model.references.Reference

sealed interface ServerVariableInterface {

    data class ServerVariableInline(
        @get:JsonUnwrapped
        val serverVariable: ServerVariable,
    ) : ServerVariableInterface

    data class ServerVariableReference(
        @get:JsonUnwrapped
        val reference: Reference,
    ) : ServerVariableInterface
}
