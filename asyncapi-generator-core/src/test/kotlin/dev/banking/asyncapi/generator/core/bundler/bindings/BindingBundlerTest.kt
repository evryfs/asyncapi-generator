package dev.banking.asyncapi.generator.core.bundler.bindings

import dev.banking.asyncapi.generator.core.bundler.BundlingContext
import dev.banking.asyncapi.generator.core.model.bindings.BindingInterface
import dev.banking.asyncapi.generator.core.model.references.Reference
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BindingBundlerTest {

    private val bundler = BindingBundler()

    @Test
    fun `bundle marks an unvisited binding reference as inline`() {
        val reference = Reference("#/components/messageBindings/kafka")
        val binding = BindingInterface.BindingReference(reference)

        val bundled = bundler.bundle(binding, BundlingContext.empty())

        assertThat(bundled).isSameAs(binding)
        assertThat(reference.inline).isTrue()
    }

    @Test
    fun `bundle keeps a visited binding reference unchanged`() {
        val reference = Reference("#/components/messageBindings/kafka")
        val binding = BindingInterface.BindingReference(reference)

        val bundled = bundler.bundle(binding, BundlingContext.empty().enter(reference))

        assertThat(bundled).isSameAs(binding)
        assertThat(reference.inline).isFalse()
    }
}
