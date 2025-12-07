package com.tietoevry.banking.asyncapi.generator.core.parser.externaldocs

import com.tietoevry.banking.asyncapi.generator.core.model.externaldocs.ExternalDoc
import com.tietoevry.banking.asyncapi.generator.core.model.references.Reference

fun myExternalDocs() = ExternalDoc(
    description = "\"My API Documentation",
    url = "\"https://example.com/api-docs"
)

fun refExternalDocs() = Reference(ref = "'#/components/externalDocs/MyExternalDocs")
