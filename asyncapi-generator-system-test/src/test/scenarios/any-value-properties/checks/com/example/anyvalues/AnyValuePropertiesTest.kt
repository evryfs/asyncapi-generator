package com.example.anyvalues

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlin.test.Test
import kotlin.test.assertEquals
import com.example.anyvalues.java.Event as JavaEvent
import com.example.anyvalues.kotlin.Event as KotlinEvent

/** A property whose schema is `true` accepts any value, as JSON Schema defines it. */
class AnyValuePropertiesTest {
    private val values = listOf("""{"note":"text"}""", "42", "\"text\"", "[1,2]", "true")

    @Test
    fun `Kotlin reads any value into a property whose schema is true`() {
        for (value in values) {
            val event = jacksonObjectMapper().readValue<KotlinEvent>("""{"id":"e-1","details":$value}""")

            assertEquals(ObjectMapper().readValue(value, Any::class.java), event.details)
        }
    }

    @Test
    fun `Java reads any value into a property whose schema is true`() {
        for (value in values) {
            val event = ObjectMapper().readValue("""{"id":"e-1","details":$value}""", JavaEvent::class.java)

            assertEquals(ObjectMapper().readValue(value, Any::class.java), event.getDetails())
        }
    }
}
