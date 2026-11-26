package com.example.literals

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlin.test.Test
import kotlin.test.assertEquals
import com.example.literals.java.Alert as JavaAlert
import com.example.literals.kotlin.Alert as KotlinAlert

/**
 * Defaults and enum values are kept exactly as the contract writes them, whatever character they start with.
 * Java models do not set defaults, so only the Kotlin check reads them.
 */
class LiteralValuesTest {
    private val routes = listOf("|pipe", "'single'", "plain")

    @Test
    fun `Kotlin keeps defaults and enum values as written`() {
        val mapper = jacksonObjectMapper()
        val read = mapper.readValue<KotlinAlert>("{}")

        assertEquals("'quoted'", read.note)
        assertEquals("> 5", read.threshold)
        routes.forEach { route ->
            val alert = mapper.readValue<KotlinAlert>(mapper.writeValueAsString(mapOf("route" to route)))
            assertEquals(route, mapper.readTree(mapper.writeValueAsString(alert)).get("route").asText())
        }
    }

    @Test
    fun `Java keeps enum values as written`() {
        val mapper = ObjectMapper()
        routes.forEach { route ->
            val alert = mapper.readValue(mapper.writeValueAsString(mapOf("route" to route)), JavaAlert::class.java)
            assertEquals(route, mapper.readTree(mapper.writeValueAsString(alert)).get("route").asText())
        }
    }
}
