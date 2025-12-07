package dev.banking.asyncapi.generator.core.generator.java.mapper

import dev.banking.asyncapi.generator.core.generator.context.GeneratorContext
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ArrayMapperTest {

    @Test
    fun `map should return null for non-array types`() {
        val context = GeneratorContext(emptyMap())
        val mapper = ArrayMapper(context)
        val root = JavaTypeMapper(context)

        assertNull(mapper.map(Schema(type = "string"), "prop", root))
    }

    @Test
    fun `map should handle list of primitives`() {
        val context = GeneratorContext(emptyMap())
        val mapper = ArrayMapper(context)
        val root = JavaTypeMapper(context)

        val schema = Schema(
            type = "array",
            items = SchemaInterface.SchemaInline(Schema(type = "string"))
        )

        assertEquals("List<String>", mapper.map(schema, "tags", root))
    }

    @Test
    fun `map should handle list of references`() {
        val userSchema = Schema(type = "object", title = "User")
        val context = GeneratorContext(mapOf("User" to userSchema))
        val mapper = ArrayMapper(context)
        val root = JavaTypeMapper(context)

        val schema = Schema(
            type = "array",
            items = SchemaInterface.SchemaReference(Reference("#/components/schemas/User"))
        )

        assertEquals("List<User>", mapper.map(schema, "users", root))
    }

    @Test
    fun `map should handle list of enums (reference)`() {
        val statusSchema = Schema(type = "string", enum = listOf("ACTIVE", "INACTIVE"))
        val context = GeneratorContext(mapOf("Status" to statusSchema))
        val mapper = ArrayMapper(context)
        val root = JavaTypeMapper(context)

        val schema = Schema(
            type = "array",
            items = SchemaInterface.SchemaReference(Reference("#/components/schemas/Status"))
        )

        assertEquals("List<Status>", mapper.map(schema, "statuses", root))
    }
}
