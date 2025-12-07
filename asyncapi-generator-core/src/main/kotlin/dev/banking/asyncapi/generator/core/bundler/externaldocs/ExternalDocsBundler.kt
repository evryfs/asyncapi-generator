package dev.banking.asyncapi.generator.core.bundler.externaldocs

import dev.banking.asyncapi.generator.core.model.externaldocs.ExternalDocInterface

class ExternalDocsBundler {

    fun bundleMap(
        externalDocs: Map<String, ExternalDocInterface>?,
        visited: Set<String>,
    ): Map<String, ExternalDocInterface>? {
        if (externalDocs == null) return null
        return externalDocs.mapValues { (_, external) -> bundle(external, visited) }
    }

    fun bundle(externalDoc: ExternalDocInterface, visited: Set<String>): ExternalDocInterface =
        when (externalDoc) {
            is ExternalDocInterface.ExternalDocReference -> {
                val ref = externalDoc.reference.ref
                if (!visited.contains(ref)) {
                    externalDoc.reference.inline()
                }
                externalDoc
            }
            else -> externalDoc
        }
}
