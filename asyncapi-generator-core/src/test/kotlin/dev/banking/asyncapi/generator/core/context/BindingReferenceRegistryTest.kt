package dev.banking.asyncapi.generator.core.context

import dev.banking.asyncapi.generator.core.model.bindings.BindingLocation
import dev.banking.asyncapi.generator.core.model.references.Reference
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BindingReferenceRegistryTest {

    private val registry = BindingReferenceRegistry()

    @Test
    fun `getOrigin returns null for unregistered reference`() {
        val reference = Reference("#/components/serverBindings/kafka")

        assertNull(registry.getOrigin(reference))
    }

    @Test
    fun `getOrigin returns registered origin`() {
        val reference = Reference("#/components/serverBindings/kafka")

        registry.register(reference, BindingLocation.CHANNEL, "kafka")

        val origin = registry.getOrigin(reference)
        assertEquals(BindingLocation.CHANNEL, origin?.location)
        assertEquals("kafka", origin?.protocol)
    }
}
