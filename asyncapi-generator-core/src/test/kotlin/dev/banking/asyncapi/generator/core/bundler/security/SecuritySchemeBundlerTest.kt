package dev.banking.asyncapi.generator.core.bundler.security

import dev.banking.asyncapi.generator.core.bundler.BundlingContext
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.security.SecuritySchemeInterface
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SecuritySchemeBundlerTest {

    private val bundler = SecuritySchemeBundler()

    @Test
    fun `bundleList marks an unvisited security scheme reference as inline`() {
        val reference = Reference("#/components/securitySchemes/sasl")
        val securityScheme = SecuritySchemeInterface.SecuritySchemeReference(reference)

        val bundled = bundler.bundleList(listOf(securityScheme), BundlingContext.empty())

        assertThat(bundled).containsExactly(securityScheme)
        assertThat(reference.inline).isTrue()
    }

    @Test
    fun `bundleMap keeps a visited security scheme reference unchanged`() {
        val reference = Reference("#/components/securitySchemes/sasl")
        val securityScheme = SecuritySchemeInterface.SecuritySchemeReference(reference)

        val bundled = bundler.bundleMap(mapOf("sasl" to securityScheme), BundlingContext.empty().enter(reference))

        assertThat(bundled).containsEntry("sasl", securityScheme)
        assertThat(reference.inline).isFalse()
    }
}
