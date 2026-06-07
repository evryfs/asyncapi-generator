package dev.banking.asyncapi.generator.core.bundler.parameters

import dev.banking.asyncapi.generator.core.bundler.BundlingContext
import dev.banking.asyncapi.generator.core.model.parameters.ParameterInterface
import dev.banking.asyncapi.generator.core.model.references.Reference
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ParameterBundlerTest {

    private val bundler = ParameterBundler()

    @Test
    fun `bundleMap marks an unvisited parameter reference as inline`() {
        val reference = Reference("#/components/parameters/userId")
        val parameter = ParameterInterface.ParameterReference(reference)

        val bundled = bundler.bundleMap(mapOf("userId" to parameter), BundlingContext.empty())

        assertThat(bundled).containsEntry("userId", parameter)
        assertThat(reference.inline).isTrue()
    }

    @Test
    fun `bundle keeps a visited parameter reference unchanged`() {
        val reference = Reference("#/components/parameters/userId")
        val parameter = ParameterInterface.ParameterReference(reference)

        val bundled = bundler.bundle(parameter, BundlingContext.empty().enter(reference))

        assertThat(bundled).isSameAs(parameter)
        assertThat(reference.inline).isFalse()
    }
}
