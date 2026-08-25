package dev.banking.asyncapi.generator.core.context

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ModelTrackingTest {

    private val sourceTracking = SourceTracking()
    private val tracking = ModelTracking(sourceTracking)

    @Test
    fun `findReference returns null for unregistered reference`() {
        val reference = dev.banking.asyncapi.generator.core.model.references.Reference("#/components/schemas/User")

        assertNull(tracking.findReference(reference))
    }

    @Test
    fun `getSourceLocation returns null for unregistered model`() {
        assertNull(tracking.getSourceLocation("unknown"))
    }

    @Test
    fun `getFieldNames returns empty set for unregistered model`() {
        assertEquals(emptySet(), tracking.getFieldNames("unknown"))
    }

    @Test
    fun `getFieldValue returns null for unregistered model`() {
        assertNull(tracking.getFieldValue("unknown", "field"))
    }
}
