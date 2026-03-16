package dev.banking.asyncapi.generator.core.parser.externaldocs

import dev.banking.asyncapi.generator.core.model.externaldocs.ExternalDoc
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.EXTERNAL_DOC

fun myExternalDocs() = ExternalDoc(
    description = "\"My API Documentation",
    url = "\"https://example.com/api-docs"
)

fun refExternalDocs() = Reference(ref = "'#/components/externalDocs/MyExternalDocs", referenceCategoryKey = EXTERNAL_DOC)
