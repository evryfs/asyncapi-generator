package dev.banking.asyncapi.generator.core.model.operations

import com.fasterxml.jackson.annotation.JsonAnyGetter
import dev.banking.asyncapi.generator.core.model.bindings.BindingInterface
import dev.banking.asyncapi.generator.core.model.externaldocs.ExternalDocInterface
import dev.banking.asyncapi.generator.core.model.security.SecuritySchemeInterface
import dev.banking.asyncapi.generator.core.model.tags.TagInterface

internal data class OperationTrait(
    val title: String? = null,
    val summary: String? = null,
    val description: String? = null,
    val security: List<SecuritySchemeInterface>? = null,
    val tags: List<TagInterface>? = null,
    val externalDocs: ExternalDocInterface? = null,
    val bindings: Map<String, BindingInterface>? = null,
    @get:JsonAnyGetter val extensions: Map<String, Any?>? = null,
)
