package dev.banking.asyncapi.generator.core.model.externaldocs

import com.fasterxml.jackson.annotation.JsonUnwrapped
import dev.banking.asyncapi.generator.core.model.references.Reference

sealed interface ExternalDocInterface {

    data class ExternalDocInline(
        @get:JsonUnwrapped
        val externalDoc: ExternalDoc,
    ) : ExternalDocInterface

    data class ExternalDocReference(
        @get:JsonUnwrapped
        val reference: Reference,
    ) : ExternalDocInterface
}

