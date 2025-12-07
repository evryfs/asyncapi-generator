package dev.banking.asyncapi.generator.core.model.tags

import com.fasterxml.jackson.annotation.JsonUnwrapped
import dev.banking.asyncapi.generator.core.model.references.Reference

sealed interface TagInterface {

    data class TagInline(
        @get:JsonUnwrapped
        val tag: Tag,
    ) : TagInterface

    data class TagReference(
        @get:JsonUnwrapped
        val reference: Reference,
    ) : TagInterface
}
