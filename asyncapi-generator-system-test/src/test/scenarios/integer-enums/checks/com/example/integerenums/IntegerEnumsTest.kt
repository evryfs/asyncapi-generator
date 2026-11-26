package com.example.integerenums

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlin.test.Test
import kotlin.test.assertEquals
import com.example.integerenums.java.Reading as JavaReading
import com.example.integerenums.kotlin.Reading as KotlinReading

/** A referenced enum that is not a string enum is typed as its scalar, as there is no enum type for it. */
class IntegerEnumsTest {
    private val reading = """{"level":2,"scale":0.5,"history":[1,3]}"""

    @Test
    fun `Kotlin types referenced integer and number enums as scalars`() {
        val read = jacksonObjectMapper().readValue<KotlinReading>(reading)

        assertEquals(2, read.level)
        assertEquals(0.5, read.scale)
        assertEquals(listOf(1, 3), read.history)
    }

    @Test
    fun `Java types referenced integer and number enums as scalars`() {
        val read = ObjectMapper().readValue(reading, JavaReading::class.java)

        assertEquals(2, read.getLevel())
        assertEquals(0.5, read.getScale())
        assertEquals(listOf(1, 3), read.getHistory())
    }
}
