package dev.banking.asyncapi.generator.core.model.bindings

import com.fasterxml.jackson.annotation.JsonUnwrapped
import dev.banking.asyncapi.generator.core.model.references.Reference

sealed interface BindingInterface {

    data class BindingInline(
        @get:JsonUnwrapped
        val binding: Binding,
    ) : BindingInterface

    data class BindingReference(
        @get:JsonUnwrapped
        val reference: Reference,
    ) : BindingInterface
}
