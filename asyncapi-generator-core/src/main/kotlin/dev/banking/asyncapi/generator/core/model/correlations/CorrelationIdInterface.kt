package dev.banking.asyncapi.generator.core.model.correlations

import com.fasterxml.jackson.annotation.JsonUnwrapped
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.references.ReferenceOr

/** A Correlation ID Object written inline or as a `$ref`. */
internal sealed interface CorrelationIdInterface : ReferenceOr<CorrelationId> {
    /**
     * A Correlation ID Object written inline.
     *
     * @property correlationId the object
     */
    data class CorrelationIdInline(
        @get:JsonUnwrapped
        val correlationId: CorrelationId,
    ) : CorrelationIdInterface {
        override fun resolvedOrNull(): CorrelationId = correlationId

        override fun held(): Any = correlationId
    }

    /**
     * A `$ref` to a Correlation ID Object.
     *
     * @property reference the reference
     */
    data class CorrelationIdReference(
        @get:JsonUnwrapped val reference: Reference,
    ) : CorrelationIdInterface {
        override fun resolvedOrNull(): CorrelationId? = reference.finalModel() as? CorrelationId

        override fun held(): Any = reference
    }
}
