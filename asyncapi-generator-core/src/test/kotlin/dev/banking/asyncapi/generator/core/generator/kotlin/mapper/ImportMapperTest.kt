package dev.banking.asyncapi.generator.core.generator.kotlin.mapper

import dev.banking.asyncapi.generator.core.generator.kotlin.model.PropertyModel
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class ImportMapperTest {

    private val mapper = ImportMapper("com.example.model")

    @Test
    fun `computeImports should add imports for java built-ins`() {
        val fields = listOf(
            PropertyModel(
                name = "id",
                description = emptyList(),
                typeName = "UUID",
                defaultValue = null,
                annotations = emptyList()
            ),
            PropertyModel(
                name = "createdAt",
                description = emptyList(),
                typeName = "OffsetDateTime",
                defaultValue = null,
                annotations = emptyList()
            ),
            PropertyModel(
                name = "someDate",
                description = emptyList(),
                typeName = "LocalDate",
                defaultValue = null,
                annotations = emptyList()
            ),
            PropertyModel(
                name = "amount",
                description = emptyList(),
                typeName = "BigDecimal",
                defaultValue = null,
                annotations = emptyList()
            )
        )

        val imports = mapper.computeImports("MyClass", fields)

        assertTrue(imports.contains("java.util.UUID"))
        assertTrue(imports.contains("java.time.OffsetDateTime"))
        assertTrue(imports.contains("java.time.LocalDate"))
        assertTrue(imports.contains("java.math.BigDecimal"))
    }

    @Test
    fun `computeImports should add imports for validation annotations`() {
        val fields = listOf(
            PropertyModel(
                name = "email",
                description = emptyList(),
                typeName = "String",
                defaultValue = null,
                annotations = listOf(
                    "@field:Email",
                    "@field:Size(min=5)",
                    "@JsonProperty(access = JsonProperty.Access.READ_ONLY)"
                )
            )
        )

        val imports = mapper.computeImports("MyClass", fields)

        assertTrue(imports.contains("jakarta.validation.constraints.Email"))
        assertTrue(imports.contains("jakarta.validation.constraints.Size"))
        assertTrue(imports.contains("com.fasterxml.jackson.annotation.JsonProperty"))
        assertTrue(imports.contains("com.fasterxml.jackson.annotation.JsonProperty.Access"))
    }

    @Test
    fun `computeImports should import models from same package`() {
        val fields = listOf(
            PropertyModel(
                name = "user",
                description = emptyList(),
                typeName = "User",
                defaultValue = null,
                annotations = emptyList()
            ),
            PropertyModel(
                name = "product",
                description = emptyList(),
                typeName = "Product?",
                defaultValue = null,
                annotations = emptyList()
            )
        )

        val imports = mapper.computeImports("MyClass", fields)

        assertTrue(imports.contains("com.example.model.User"))
        assertTrue(imports.contains("com.example.model.Product"))
    }

    @Test
    fun `computeImports should handle List of models`() {
        val fields = listOf(
            PropertyModel(
                name = "users",
                description = emptyList(),
                typeName = "List<User>",
                defaultValue = null,
                annotations = emptyList()
            ),
            PropertyModel(
                name = "products",
                description = emptyList(),
                typeName = "List<Product?>",
                defaultValue = null,
                annotations = emptyList()
            )
        )

        val imports = mapper.computeImports("MyClass", fields)

        assertTrue(imports.contains("com.example.model.User"))
        assertTrue(imports.contains("com.example.model.Product"))
    }

    @Test
    fun `computeImports should not import built-in types`() {
        val fields = listOf(
            PropertyModel(
                name = "age",
                description = emptyList(),
                typeName = "Int",
                defaultValue = null,
                annotations = emptyList()
            ),
            PropertyModel(
                name = "name",
                description = emptyList(),
                typeName = "String",
                defaultValue = null,
                annotations = emptyList()
            ),
            PropertyModel(
                name = "flag",
                description = emptyList(),
                typeName = "Boolean?",
                defaultValue = null,
                annotations = emptyList()
            )
        )

        val imports = mapper.computeImports("MyClass", fields)

        assertTrue(imports.none { it.startsWith("com.example.model") })
    }

    @Test
    fun `computeImports should not import self-referencing class`() {
        val fields = listOf(
            PropertyModel(
                name = "parent",
                description = emptyList(),
                typeName = "MyClass",
                defaultValue = null,
                annotations = emptyList()
            )
        )

        val imports = mapper.computeImports("MyClass", fields)

        assertTrue(imports.none { it.contains("MyClass") })
    }

    @Test
    fun `computeImports should handle fully qualified types`() {
        val fields = listOf(
            PropertyModel(
                name = "status",
                description = emptyList(),
                typeName = "com.other.package.Status",
                defaultValue = null,
                annotations = emptyList(),
            )
        )

        val imports = mapper.computeImports("MyClass", fields)

        assertTrue(imports.none { it.contains("com.other.package.Status") })
    }

    @Test
    fun `computeImports should handle types with generic parameters`() {
        val fields = listOf(
            PropertyModel(
                name = "data",
                description = emptyList(),
                typeName = "Map<String, User>",
                defaultValue = null,
                annotations = emptyList()
            )
        )

        val imports = mapper.computeImports("MyClass", fields)

        assertTrue(imports.contains("com.example.model.User"))
        assertTrue(imports.none { it.contains("Map") })
    }

    @Test
    fun `computeImports should handle nullable generic parameters`() {
        val fields = listOf(
            PropertyModel(
                name = "nullableList",
                description = emptyList(),
                typeName = "List<User?>",
                defaultValue = null,
                annotations = emptyList()
            ),
            PropertyModel(
                name = "nullableMapValue",
                description = emptyList(),
                typeName = "Map<String, Product?>",
                defaultValue = null,
                annotations = emptyList()
            )
        )

        val imports = mapper.computeImports("MyClass", fields)

        assertTrue(imports.contains("com.example.model.User"))
        assertTrue(imports.contains("com.example.model.Product"))
    }

    @Test
    fun `computeImports should handle whitespace in generic parameters`() {
        val fields = listOf(
            PropertyModel(
                name = "spacedMap",
                description = emptyList(),
                typeName = "Map< String , User >", // Extra spaces
                defaultValue = null,
                annotations = emptyList()
            )
        )

        val imports = mapper.computeImports("MyClass", fields)

        assertTrue(imports.contains("com.example.model.User"))
    }

    @Test
    fun `computeImports should handle List of Maps`() {
        val fields = listOf(
            PropertyModel(
                name = "listOfMaps",
                description = emptyList(),
                typeName = "List<Map<String, User>>",
                defaultValue = null,
                annotations = emptyList()
            )
        )

        val imports = mapper.computeImports("MyClass", fields)
        assertTrue(imports.contains("com.example.model.User"))
    }
}
