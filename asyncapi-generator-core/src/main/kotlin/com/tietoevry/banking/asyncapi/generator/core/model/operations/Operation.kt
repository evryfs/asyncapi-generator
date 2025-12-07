package com.tietoevry.banking.asyncapi.generator.core.model.operations

import com.tietoevry.banking.asyncapi.generator.core.model.bindings.BindingInterface
import com.tietoevry.banking.asyncapi.generator.core.model.externaldocs.ExternalDocInterface
import com.tietoevry.banking.asyncapi.generator.core.model.references.Reference
import com.tietoevry.banking.asyncapi.generator.core.model.tags.TagInterface
import com.tietoevry.banking.asyncapi.generator.core.model.security.SecuritySchemeInterface

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
