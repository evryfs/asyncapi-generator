package dev.banking.asyncapi.generator.core.bundler

import dev.banking.asyncapi.generator.core.model.references.Reference
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BundlingContextTest {

    @Test
    fun `empty context has no visited references`() {
        val reference = Reference("#/components/schemas/User")

        val context = BundlingContext.empty()

        assertThat(context.hasVisited(reference)).isFalse()
    }

    @Test
    fun `enter returns a context with the visited reference`() {
        val reference = Reference("#/components/schemas/User")
        val original = BundlingContext.empty()

        val next = original.enter(reference)

        assertThat(next).isNotSameAs(original)
        assertThat(original.hasVisited(reference)).isFalse()
        assertThat(next.hasVisited(reference)).isTrue()
    }

    @Test
    fun `references from different source documents have distinct traversal identities`() {
        val first = Reference("#/components/schemas/User", sourceId = "first.yaml")
        val second = Reference("#/components/schemas/User", sourceId = "second.yaml")

        val context = BundlingContext.empty().enter(first)

        assertThat(context.hasVisited(first)).isTrue()
        assertThat(context.hasVisited(second)).isFalse()
    }
}
