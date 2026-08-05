package dev.banking.asyncapi.generator.core.bundler.externaldocs

import dev.banking.asyncapi.generator.core.bundler.BundlingContext
import dev.banking.asyncapi.generator.core.bundler.ReferenceBundler
import dev.banking.asyncapi.generator.core.model.externaldocs.ExternalDocInterface

/**
 * Bundles external documentation objects and references.
 *
 * Expected behavior is covered by:
 * - `ExternalDocsBundlerTest`
 */
internal class ExternalDocsBundler {
    fun bundleMap(
        externalDocs: Map<String, ExternalDocInterface>?,
        context: BundlingContext,
    ): Map<String, ExternalDocInterface>? {
        if (externalDocs == null) return null
        return externalDocs.mapValues { (_, external) -> bundle(external, context) }
    }
    fun bundle(externalDoc: ExternalDocInterface, context: BundlingContext): ExternalDocInterface =
        when (externalDoc) {
            is ExternalDocInterface.ExternalDocReference -> {
                ReferenceBundler.inlineIfUnvisited(externalDoc.reference, context)
                externalDoc
            }
            else -> externalDoc
        }
}
