package com.tietoevry.banking.asyncapi.generator.core.parser.tags

import com.tietoevry.banking.asyncapi.generator.core.model.externaldocs.ExternalDoc
import com.tietoevry.banking.asyncapi.generator.core.model.externaldocs.ExternalDocInterface
import com.tietoevry.banking.asyncapi.generator.core.model.references.Reference
import com.tietoevry.banking.asyncapi.generator.core.model.tags.Tag

fun inlineTag() = Tag(
    name = "\"user",
    description = "\"User related operations",
    externalDocs = ExternalDocInterface.ExternalDocInline(
        ExternalDoc(
            url = "\"https://example.com/docs/user",
            description = "\"User docs"
        )
    )
)

fun refTag() = Reference(ref = "'#/components/tags/inlineTag")
