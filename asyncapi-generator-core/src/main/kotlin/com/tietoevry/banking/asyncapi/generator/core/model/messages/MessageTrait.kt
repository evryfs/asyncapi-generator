package com.tietoevry.banking.asyncapi.generator.core.model.messages

import com.tietoevry.banking.asyncapi.generator.core.model.bindings.BindingInterface
import com.tietoevry.banking.asyncapi.generator.core.model.correlations.CorrelationIdInterface
import com.tietoevry.banking.asyncapi.generator.core.model.externaldocs.ExternalDocInterface
import com.tietoevry.banking.asyncapi.generator.core.model.tags.TagInterface
import com.tietoevry.banking.asyncapi.generator.core.model.schemas.SchemaInterface

data class MessageTrait(
    val headers: Map<String, SchemaInterface>? = null,
    val correlationId: CorrelationIdInterface? = null,
    val contentType: String? = null,
    val name: String? = null,
    val title: String? = null,
    val summary: String? = null,
    val description: String? = null,
    val tags: List<TagInterface>? = null,
    val externalDocs: ExternalDocInterface? = null,
    val bindings: Map<String, BindingInterface>? = null,
    val examples: List<MessageExample>? = null,
)
