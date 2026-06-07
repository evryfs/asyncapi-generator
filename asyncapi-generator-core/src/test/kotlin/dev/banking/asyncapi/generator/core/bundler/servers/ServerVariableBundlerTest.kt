package dev.banking.asyncapi.generator.core.bundler.servers

import dev.banking.asyncapi.generator.core.bundler.BundlingContext
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.servers.ServerVariableInterface
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ServerVariableBundlerTest {

    private val bundler = ServerVariableBundler()

    @Test
    fun `bundle marks an unvisited server variable reference as inline`() {
        val reference = Reference("#/components/serverVariables/environment")
        val serverVariable = ServerVariableInterface.ServerVariableReference(reference)

        val bundled = bundler.bundle(serverVariable, BundlingContext.empty())

        assertThat(bundled).isSameAs(serverVariable)
        assertThat(reference.inline).isTrue()
    }

    @Test
    fun `bundleMap keeps a visited server variable reference unchanged`() {
        val reference = Reference("#/components/serverVariables/environment")
        val serverVariable = ServerVariableInterface.ServerVariableReference(reference)

        val bundled = bundler.bundleMap(mapOf("environment" to serverVariable), BundlingContext.empty().enter(reference))

        assertThat(bundled).containsEntry("environment", serverVariable)
        assertThat(reference.inline).isFalse()
    }
}
