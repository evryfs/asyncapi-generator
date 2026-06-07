package dev.banking.asyncapi.generator.core.bundler.security

import dev.banking.asyncapi.generator.core.bundler.BundlingContext
import dev.banking.asyncapi.generator.core.bundler.ReferenceBundler
import dev.banking.asyncapi.generator.core.model.security.SecuritySchemeInterface

/**
 * Bundles security scheme objects and references.
 *
 * Expected behavior is covered by:
 * - `SecuritySchemeBundlerTest`
 */
class SecuritySchemeBundler {

    fun bundleMap(schemes: Map<String, SecuritySchemeInterface>?, visited: Set<String>): Map<String, SecuritySchemeInterface>? =
        bundleMap(schemes, BundlingContext.from(visited))

    fun bundleMap(
        schemes: Map<String, SecuritySchemeInterface>?,
        context: BundlingContext,
    ): Map<String, SecuritySchemeInterface>? =
        schemes?.mapValues { (_, scheme) ->
            bundle(scheme, context)
        }

    fun bundleList(schemes: List<SecuritySchemeInterface>?, visited: Set<String>): List<SecuritySchemeInterface>? =
        bundleList(schemes, BundlingContext.from(visited))

    fun bundleList(
        schemes: List<SecuritySchemeInterface>?,
        context: BundlingContext,
    ): List<SecuritySchemeInterface>? =
        schemes?.map { scheme ->
            bundle(scheme, context)
        }

    fun bundle(scheme: SecuritySchemeInterface, context: BundlingContext): SecuritySchemeInterface =
        when (scheme) {
            is SecuritySchemeInterface.SecuritySchemeReference -> {
                ReferenceBundler.inlineIfUnvisited(scheme.reference, context)
                scheme
            }
            else -> scheme
        }
}
