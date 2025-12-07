package com.tietoevry.banking.asyncapi.generator.core.model.info

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.tietoevry.banking.asyncapi.generator.core.model.externaldocs.ExternalDocInterface
import com.tietoevry.banking.asyncapi.generator.core.model.tags.TagInterface

data class Info(
    val title: String,
    val version: String,
    val description: String? = null,
    val termsOfService: String? = null,
    val contact: Contact? = null,
    val license: License? = null,
    val tags: List<TagInterface>? = null,
    val externalDocs: ExternalDocInterface? = null,
    @get:JsonAnyGetter val extensions: Map<String, Any?>? = null
)
