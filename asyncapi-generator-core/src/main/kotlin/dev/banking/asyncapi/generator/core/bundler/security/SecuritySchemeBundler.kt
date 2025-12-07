package dev.banking.asyncapi.generator.core.bundler.security

import dev.banking.asyncapi.generator.core.model.security.SecuritySchemeInterface

class SecuritySchemeBundler {

    fun bundleMap(schemes: Map<String, SecuritySchemeInterface>?, visited: Set<String>): Map<String, SecuritySchemeInterface>? =
        schemes?.mapValues { (_, scheme) ->
            when (scheme) {
                is SecuritySchemeInterface.SecuritySchemeReference -> {
                    val ref = scheme.reference.ref
                    if (!visited.contains(ref)) {
                        scheme.reference.inline()
                    }
                    scheme
                }
                else -> scheme
            }
        }

    fun bundleList(schemes: List<SecuritySchemeInterface>?, visited: Set<String>): List<SecuritySchemeInterface>? =
        schemes?.map { scheme ->
            when (scheme) {
                is SecuritySchemeInterface.SecuritySchemeReference -> {
                    val ref = scheme.reference.ref
                    if (!visited.contains(ref)) {
                        scheme.reference.inline()
                    }
                    scheme
                }
                else -> scheme
            }
        }
}
