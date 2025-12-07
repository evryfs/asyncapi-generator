package dev.banking.asyncapi.generator.core.model.tags

import dev.banking.asyncapi.generator.core.model.externaldocs.ExternalDocInterface

data class Tag(
    val name: String,
    val description: String? = null,
    val externalDocs: ExternalDocInterface? = null
)

