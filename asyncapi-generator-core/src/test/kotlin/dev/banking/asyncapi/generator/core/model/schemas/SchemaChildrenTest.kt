package dev.banking.asyncapi.generator.core.model.schemas

import org.junit.jupiter.api.Test
import java.lang.reflect.Field
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType
import kotlin.test.assertTrue

class SchemaChildrenTest {
    @Test
    fun `child schemas include every schema-valued field of a schema`() {
        val schema = Schema()
        val expected =
            Schema::class.java.declaredFields.filter { it.holdsSchemas() }.associate { field ->
                val child = SchemaInterface.SchemaInline(Schema(title = field.name))
                field.isAccessible = true
                field.set(schema, field.wrap(child))
                field.name to child
            }

        val children = schema.childSchemas()

        assertTrue(expected.isNotEmpty())
        expected.forEach { (field, child) ->
            assertTrue(children.any { it === child }, "child schemas miss the field '$field'")
        }
    }

    // A dependency is either a schema or a list of property names, so its map holds `Any`.
    private fun Field.holdsSchemas(): Boolean =
        type == SchemaInterface::class.java ||
            name == "dependencies" ||
            ((type == List::class.java || type == Map::class.java) && elementType().isSchema())

    private fun Field.elementType(): Type = (genericType as ParameterizedType).actualTypeArguments.last()

    private fun Type.isSchema(): Boolean =
        (if (this is WildcardType) upperBounds.first() else this) == SchemaInterface::class.java

    private fun Field.wrap(child: SchemaInterface): Any =
        when (type) {
            List::class.java -> listOf(child)
            Map::class.java -> mapOf("child" to child)
            else -> child
        }
}
