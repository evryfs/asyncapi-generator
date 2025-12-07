package com.tietoevry.banking.asyncapi.generator.core.model.components

import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.tietoevry.banking.asyncapi.generator.core.model.references.Reference

sealed interface ComponentInterface {

    data class ComponentInline(
        @get:JsonUnwrapped
        val component: Component,
    ) : ComponentInterface

    data class ComponentReference(
        @get:JsonUnwrapped
        val reference: Reference,
    ) : ComponentInterface
}
