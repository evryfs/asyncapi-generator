package dev.banking.asyncapi.generator.core.model.messages

import dev.banking.asyncapi.generator.core.model.bindings.BindingInterface
import dev.banking.asyncapi.generator.core.model.correlations.CorrelationIdInterface
import dev.banking.asyncapi.generator.core.model.externaldocs.ExternalDocInterface
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import dev.banking.asyncapi.generator.core.model.tags.TagInterface

data class Message(
    val headers: Map<String, SchemaInterface>? = null,
    val payload: SchemaInterface? = null,
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
    val traits: List<MessageTraitInterface>? = null,
)
