package com.tietoevry.banking.asyncapi.generator.core.model.tags

import com.tietoevry.banking.asyncapi.generator.core.model.externaldocs.ExternalDocInterface

data class Tag(
    val name: String,
    val description: String? = null,
    val externalDocs: ExternalDocInterface? = null
)

