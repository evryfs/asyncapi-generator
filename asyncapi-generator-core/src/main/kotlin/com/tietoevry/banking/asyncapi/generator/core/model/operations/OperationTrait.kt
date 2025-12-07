package com.tietoevry.banking.asyncapi.generator.core.model.operations

import com.tietoevry.banking.asyncapi.generator.core.model.bindings.BindingInterface
import com.tietoevry.banking.asyncapi.generator.core.model.externaldocs.ExternalDocInterface
import com.tietoevry.banking.asyncapi.generator.core.model.tags.TagInterface
import com.tietoevry.banking.asyncapi.generator.core.model.security.SecuritySchemeInterface

data class OperationTrait(
    val title: String? = null,
    val summary: String? = null,
    val description: String? = null,
    val security: Map<String, SecuritySchemeInterface>? = null,
    val tags: List<TagInterface>? = null,
    val externalDocs: ExternalDocInterface? = null,
    val bindings: Map<String, BindingInterface>? = null,
)
