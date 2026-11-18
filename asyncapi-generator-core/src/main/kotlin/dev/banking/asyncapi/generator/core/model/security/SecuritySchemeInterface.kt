package dev.banking.asyncapi.generator.core.model.security

import com.fasterxml.jackson.annotation.JsonUnwrapped
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.references.ReferenceOr

/** A Security Scheme Object written inline or as a `$ref`. */
sealed interface SecuritySchemeInterface : ReferenceOr<SecurityScheme> {
    /**
     * A Security Scheme Object written inline.
     *
     * @property security the object
     */
    data class SecuritySchemeInline(
        @get:JsonUnwrapped
        val security: SecurityScheme,
    ) : SecuritySchemeInterface {
        override fun resolvedOrNull(): SecurityScheme = security

        override fun held(): Any = security
    }

    /**
     * A `$ref` to a Security Scheme Object.
     *
     * @property reference the reference
     */
    data class SecuritySchemeReference(
        @get:JsonUnwrapped
        val reference: Reference,
    ) : SecuritySchemeInterface {
        override fun resolvedOrNull(): SecurityScheme? = reference.finalModel() as? SecurityScheme

        override fun held(): Any = reference
    }
}
