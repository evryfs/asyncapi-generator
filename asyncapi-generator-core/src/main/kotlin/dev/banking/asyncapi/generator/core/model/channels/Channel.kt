package dev.banking.asyncapi.generator.core.model.channels

import dev.banking.asyncapi.generator.core.model.bindings.BindingInterface
import dev.banking.asyncapi.generator.core.model.externaldocs.ExternalDocInterface
import dev.banking.asyncapi.generator.core.model.messages.MessageInterface
import dev.banking.asyncapi.generator.core.model.parameters.ParameterInterface
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.tags.TagInterface

data class Channel(
    val address: String? = null,
    val messages: Map<String, MessageInterface>? = null,
    val title: String? = null,
    val summary: String? = null,
    val description: String? = null,
    val servers: List<Reference>? = null,
    val parameters: Map<String, ParameterInterface>? = null,
    val tags: List<TagInterface>? = null,
    val externalDocs: ExternalDocInterface? = null,
    val bindings: Map<String, BindingInterface>? = null,
)
