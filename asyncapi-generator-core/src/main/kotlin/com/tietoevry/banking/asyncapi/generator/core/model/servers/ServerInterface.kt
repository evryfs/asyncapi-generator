package com.tietoevry.banking.asyncapi.generator.core.model.servers

import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.tietoevry.banking.asyncapi.generator.core.model.references.Reference

sealed interface ServerInterface {

    data class ServerInline(
        @get:JsonUnwrapped
        val server: Server,
    ) : ServerInterface

    data class ServerReference(
        @get:JsonUnwrapped
        val reference: Reference,
    ) : ServerInterface
}
