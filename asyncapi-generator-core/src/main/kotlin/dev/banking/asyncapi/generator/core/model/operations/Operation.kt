package dev.banking.asyncapi.generator.core.model.operations

import dev.banking.asyncapi.generator.core.model.bindings.BindingInterface
import dev.banking.asyncapi.generator.core.model.externaldocs.ExternalDocInterface
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.security.SecuritySchemeInterface
import dev.banking.asyncapi.generator.core.model.tags.TagInterface

data class Operation(
    val title: String? = null,
    val action: String,
    val summary: String? = null,
    val description: String? = null,
    val channel: Reference? = null,
    val messages: List<Reference>? = null,
    val bindings: Map<String, BindingInterface>? = null,
    val traits: List<OperationTraitInterface>? = null,
    val tags: List<TagInterface>? = null,
    val externalDocs: ExternalDocInterface? = null,
    val reply: OperationReplyInterface? = null,
    val security: List<SecuritySchemeInterface>? = null,
)
