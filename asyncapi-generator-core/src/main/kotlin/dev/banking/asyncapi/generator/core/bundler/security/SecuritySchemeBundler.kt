package dev.banking.asyncapi.generator.core.bundler.security

import dev.banking.asyncapi.generator.core.bundler.BundlingScope
import dev.banking.asyncapi.generator.core.bundler.ReferenceBundler
import dev.banking.asyncapi.generator.core.model.security.SecuritySchemeInterface

/**
 * Bundles security scheme objects and references.
 */
internal class SecuritySchemeBundler {
    fun bundleMap(
        schemes: Map<String, SecuritySchemeInterface>?,
        scope: BundlingScope,
    ): Map<String, SecuritySchemeInterface>? =
        schemes?.mapValues { (_, scheme) ->
            bundle(scheme, scope)
        }

    fun bundleList(
        schemes: List<SecuritySchemeInterface>?,
        scope: BundlingScope,
    ): List<SecuritySchemeInterface>? =
        schemes?.map { scheme ->
            bundle(scheme, scope)
        }

    fun bundle(
        scheme: SecuritySchemeInterface,
        scope: BundlingScope,
    ): SecuritySchemeInterface =
        when (scheme) {
            is SecuritySchemeInterface.SecuritySchemeReference -> {
                scheme.copy(reference = ReferenceBundler.inlineIfUnvisited(scheme.reference, scope))
            }
            else -> scheme
        }
}
