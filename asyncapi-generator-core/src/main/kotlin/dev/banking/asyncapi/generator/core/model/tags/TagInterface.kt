package dev.banking.asyncapi.generator.core.model.tags

import com.fasterxml.jackson.annotation.JsonUnwrapped
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.references.ReferenceOr

/** A Tag Object written inline or as a `$ref`. */
sealed interface TagInterface : ReferenceOr<Tag> {
    /**
     * A Tag Object written inline.
     *
     * @property tag the object
     */
    data class TagInline(
        @get:JsonUnwrapped
        val tag: Tag,
    ) : TagInterface {
        override fun resolvedOrNull(): Tag = tag

        override fun held(): Any = tag
    }

    /**
     * A `$ref` to a Tag Object.
     *
     * @property reference the reference
     */
    data class TagReference(
        @get:JsonUnwrapped
        val reference: Reference,
    ) : TagInterface {
        override fun resolvedOrNull(): Tag? = reference.finalModel() as? Tag

        override fun held(): Any = reference
    }
}
