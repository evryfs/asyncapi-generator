package dev.banking.asyncapi.generator.core.bundler.externaldocs

import dev.banking.asyncapi.generator.core.bundler.BundlingScope
import dev.banking.asyncapi.generator.core.bundler.ReferenceBundler
import dev.banking.asyncapi.generator.core.model.externaldocs.ExternalDocInterface

/**
 * Bundles external documentation objects and references.
 */
internal class ExternalDocsBundler {
    fun bundleMap(
        externalDocs: Map<String, ExternalDocInterface>?,
        scope: BundlingScope,
    ): Map<String, ExternalDocInterface>? {
        if (externalDocs == null) return null
        return externalDocs.mapValues { (_, external) -> bundle(external, scope) }
    }

    fun bundle(
        externalDoc: ExternalDocInterface,
        scope: BundlingScope,
    ): ExternalDocInterface =
        when (externalDoc) {
            is ExternalDocInterface.ExternalDocReference -> {
                externalDoc.copy(reference = ReferenceBundler.inlineIfUnvisited(externalDoc.reference, scope))
            }
            else -> externalDoc
        }
}
