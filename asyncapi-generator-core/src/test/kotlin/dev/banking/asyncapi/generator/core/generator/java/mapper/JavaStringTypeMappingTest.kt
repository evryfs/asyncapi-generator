package dev.banking.asyncapi.generator.core.generator.java.mapper

import dev.banking.asyncapi.generator.core.generator.context.GeneratorContext
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class JavaStringTypeMappingTest {

    private val mapper = JavaTypeMapper(GeneratorContext(emptyMap()))

    @Test
    fun `map should handle standard string formats`() {
        assertEquals("UUID", mapper.mapJavaType("p", Schema(type = "string", format = "uuid")))
        assertEquals("LocalDate", mapper.mapJavaType("p", Schema(type = "string", format = "date")))
        assertEquals("OffsetDateTime", mapper.mapJavaType("p", Schema(type = "string", format = "date-time")))
        assertEquals("LocalTime", mapper.mapJavaType("p", Schema(type = "string", format = "time")))
    }

    @Test
    fun `map should fallback to String for unknown formats`() {
        assertEquals("String", mapper.mapJavaType("p", Schema(type = "string", format = "unknown")))
        assertEquals("String", mapper.mapJavaType("p", Schema(type = "string", format = "email")))
    }
}
