package com.example.titledobjects

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlin.test.Test
import kotlin.test.assertEquals
import com.example.titledobjects.java.Audit as JavaAudit
import com.example.titledobjects.kotlin.Audit as KotlinAudit

/**
 * An inline object without properties is a map, also when it has a title: a title only names
 * schemas that become models of their own.
 */
class TitledOpenObjectsTest {
    private val audit = """{"metadata":{"source":"batch","attempt":2},"labels":{"team":"payments"}}"""

    @Test
    fun `Kotlin titled open objects are maps`() {
        val read = jacksonObjectMapper().readValue<KotlinAudit>(audit)

        assertEquals(mapOf("source" to "batch", "attempt" to 2), read.metadata)
        assertEquals(mapOf("team" to "payments"), read.labels)
    }

    @Test
    fun `Java titled open objects are maps`() {
        val read = ObjectMapper().readValue(audit, JavaAudit::class.java)

        assertEquals(mapOf("source" to "batch", "attempt" to 2), read.getMetadata())
        assertEquals(mapOf("team" to "payments"), read.getLabels())
    }
}
