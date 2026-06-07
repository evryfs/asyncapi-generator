package dev.banking.asyncapi.generator.core.bundler.correlations

import dev.banking.asyncapi.generator.core.bundler.BundlingContext
import dev.banking.asyncapi.generator.core.model.correlations.CorrelationIdInterface
import dev.banking.asyncapi.generator.core.model.references.Reference
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CorrelationIdBundlerTest {

    private val bundler = CorrelationIdBundler()

    @Test
    fun `bundle marks an unvisited correlation ID reference as inline`() {
        val reference = Reference("#/components/correlationIds/user")
        val correlationId = CorrelationIdInterface.CorrelationIdReference(reference)

        val bundled = bundler.bundle(correlationId, BundlingContext.empty())

        assertThat(bundled).isSameAs(correlationId)
        assertThat(reference.inline).isTrue()
    }

    @Test
    fun `bundleMap keeps a visited correlation ID reference unchanged`() {
        val reference = Reference("#/components/correlationIds/user")
        val correlationId = CorrelationIdInterface.CorrelationIdReference(reference)

        val bundled = bundler.bundleMap(mapOf("user" to correlationId), BundlingContext.empty().enter(reference))

        assertThat(bundled).containsEntry("user", correlationId)
        assertThat(reference.inline).isFalse()
    }
}
