package dev.banking.asyncapi.generator.core.generator.kotlin.mapper

import dev.banking.asyncapi.generator.core.generator.context.GeneratorContext
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class StringMapperTest {

    private val mapper = StringMapper()
    private val root = KotlinTypeMapper(GeneratorContext(emptyMap()))

    @Test
    fun `map should handle standard string formats`() {
        assertEquals("UUID", mapper.map(Schema(type = "string", format = "uuid"), "p", root))
        assertEquals("LocalDate", mapper.map(Schema(type = "string", format = "date"), "p", root))
        assertEquals("OffsetDateTime", mapper.map(Schema(type = "string", format = "date-time"), "p", root))
        assertEquals("LocalTime", mapper.map(Schema(type = "string", format = "time"), "p", root))
    }

    @Test
    fun `map should fallback to String for unknown formats`() {
        assertEquals("String", mapper.map(Schema(type = "string", format = "unknown"), "p", root))
        assertEquals("String", mapper.map(Schema(type = "string", format = "email"), "p", root))
    }
}
