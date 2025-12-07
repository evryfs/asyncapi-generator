package com.tietoevry.banking.asyncapi.generator.core.model.channels

import com.tietoevry.banking.asyncapi.generator.core.model.bindings.BindingInterface
import com.tietoevry.banking.asyncapi.generator.core.model.externaldocs.ExternalDocInterface
import com.tietoevry.banking.asyncapi.generator.core.model.parameters.ParameterInterface
import com.tietoevry.banking.asyncapi.generator.core.model.references.Reference
import com.tietoevry.banking.asyncapi.generator.core.model.tags.TagInterface
import com.tietoevry.banking.asyncapi.generator.core.model.messages.MessageInterface

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
