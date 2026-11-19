package dev.banking.asyncapi.generator.core.model.externaldocs

import com.fasterxml.jackson.annotation.JsonUnwrapped
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.references.ReferenceOr

/** A External Documentation Object written inline or as a `$ref`. */
sealed interface ExternalDocInterface : ReferenceOr<ExternalDoc> {
    /**
     * A External Documentation Object written inline.
     *
     * @property externalDoc the object
     */
    data class ExternalDocInline(
        @get:JsonUnwrapped
        val externalDoc: ExternalDoc,
    ) : ExternalDocInterface {
        override fun resolvedOrNull(): ExternalDoc = externalDoc

        override fun held(): Any = externalDoc
    }

    /**
     * A `$ref` to a External Documentation Object.
     *
     * @property reference the reference
     */
    data class ExternalDocReference(
        @get:JsonUnwrapped
        val reference: Reference,
    ) : ExternalDocInterface {
        override fun resolvedOrNull(): ExternalDoc? = reference.finalModel() as? ExternalDoc

        override fun held(): Any = reference
    }
}
