package dev.banking.asyncapi.generator.core.generator.kotlin.mapper

import dev.banking.asyncapi.generator.core.generator.context.GeneratorContext
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class KotlinArrayTypeMappingTest {
    @Test
    fun `map should handle list of primitives`() {
        val context = GeneratorContext(emptyMap())
        val mapper = KotlinTypeMapper(context)

        val schema = Schema(
            type = "array",
            items = SchemaInterface.SchemaInline(Schema(type = "string"))
        )

        assertEquals("List<String>", mapper.mapKotlinType("tags", schema))
    }

    @Test
    fun `map should handle list of references`() {
        val userSchema = Schema(type = "object", title = "User")
        val context = GeneratorContext(mapOf("User" to userSchema))
        val mapper = KotlinTypeMapper(context)

        val schema = Schema(
            type = "array",
            items = SchemaInterface.SchemaReference(Reference("#/components/schemas/User"))
        )

        assertEquals("List<User>", mapper.mapKotlinType("users", schema))
    }

    @Test
    fun `map should handle list of enums (reference)`() {
        val statusSchema = Schema(type = "string", enum = listOf("ACTIVE", "INACTIVE"))
        val context = GeneratorContext(mapOf("Status" to statusSchema))
        val mapper = KotlinTypeMapper(context)

        val schema = Schema(
            type = "array",
            items = SchemaInterface.SchemaReference(Reference("#/components/schemas/Status"))
        )

        assertEquals("List<Status>", mapper.mapKotlinType("statuses", schema))
    }
}
