package dev.banking.asyncapi.generator.core.model.security

import com.fasterxml.jackson.annotation.JsonUnwrapped
import dev.banking.asyncapi.generator.core.model.references.Reference

sealed interface SecuritySchemeInterface {

    data class SecuritySchemeInline(
        @get:JsonUnwrapped
        val security: SecurityScheme,
    ) : SecuritySchemeInterface

    data class SecuritySchemeReference(
        @get:JsonUnwrapped
        val reference: Reference,
    ) : SecuritySchemeInterface
}
