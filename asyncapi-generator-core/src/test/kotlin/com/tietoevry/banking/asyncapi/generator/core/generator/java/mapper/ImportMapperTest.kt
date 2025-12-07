package com.tietoevry.banking.asyncapi.generator.core.generator.java.mapper

import com.tietoevry.banking.asyncapi.generator.core.generator.java.model.PropertyModel
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class ImportMapperTest {

    private val mapper = ImportMapper("com.example.model")

    @Test
    fun `computeImports should add imports for java built-ins`() {
        val fields = listOf(
            prop("id", "UUID"),
            prop("createdAt", "OffsetDateTime"),
            prop("someDate", "LocalDate"),
            prop("amount", "BigDecimal")
        )

        val imports = mapper.computeImports("MyClass", fields)

        assertTrue(imports.contains("java.util.UUID"))
        assertTrue(imports.contains("java.time.OffsetDateTime"))
        assertTrue(imports.contains("java.time.LocalDate"))
        assertTrue(imports.contains("java.math.BigDecimal"))
        assertTrue(imports.contains("java.util.Objects"))
        assertTrue(imports.contains("java.io.Serializable"))
    }

    @Test
    fun `computeImports should add imports for validation annotations`() {
        val fields = listOf(
            prop("email", "String", listOf("@Email", "@Size(min=5)", "@JsonProperty(access = Access.READ_ONLY)"))
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
            prop("user", "User"),
            prop("product", "Product")
        )

        val imports = mapper.computeImports("MyClass", fields)

        assertTrue(imports.contains("com.example.model.User"))
        assertTrue(imports.contains("com.example.model.Product"))
    }

    @Test
    fun `computeImports should handle List of models`() {
        val fields = listOf(
            prop("users", "List<User>"),
            prop("products", "List<Product>")
        )

        val imports = mapper.computeImports("MyClass", fields)

        assertTrue(imports.contains("java.util.List"))
        assertTrue(imports.contains("com.example.model.User"))
        assertTrue(imports.contains("com.example.model.Product"))
    }

    @Test
    fun `computeImports should handle Map of models`() {
        val fields = listOf(
            prop("data", "Map<String, User>")
        )

        val imports = mapper.computeImports("MyClass", fields)

        assertTrue(imports.contains("java.util.Map"))
        assertTrue(imports.contains("com.example.model.User"))
    }

    @Test
    fun `computeImports should handle nested generics (List of Maps)`() {
        val fields = listOf(
            prop("complex", "List<Map<String, User>>")
        )

        val imports = mapper.computeImports("MyClass", fields)

        assertTrue(imports.contains("java.util.List"))
        assertTrue(imports.contains("java.util.Map"))
        assertTrue(imports.contains("com.example.model.User"))
    }

    @Test
    fun `computeImports should handle whitespace in generics`() {
        val fields = listOf(
            prop("spaced", "Map< String , User >")
        )

        val imports = mapper.computeImports("MyClass", fields)

        assertTrue(imports.contains("java.util.Map"))
        assertTrue(imports.contains("com.example.model.User"))
    }

    @Test
    fun `computeImports should not import self-referencing class`() {
        val fields = listOf(
            prop("parent", "MyClass")
        )

        val imports = mapper.computeImports("MyClass", fields)

        assertTrue(imports.none { it.contains("MyClass") })
    }

    @Test
    fun `computeImports should not import fully qualified types`() {
        val fields = listOf(
            prop("status", "com.other.package.Status")
        )

        val imports = mapper.computeImports("MyClass", fields)

        assertTrue(imports.none { it.contains("com.other.package.Status") })
    }

    @Test
    fun `computeImports should not import built-in types`() {
        val fields = listOf(
            prop("age", "Integer"),
            prop("name", "String"),
            prop("flag", "Boolean")
        )

        val imports = mapper.computeImports("MyClass", fields)

        assertTrue(imports.none { it.startsWith("com.example.model") })
    }


    private fun prop(name: String, type: String, annotations: List<String> = emptyList()): PropertyModel {
        return PropertyModel(
            name = name,
            description = emptyList(),
            typeName = type,
            getterName = "get${name.replaceFirstChar { it.uppercase() }}",
            setterName = "set${name.replaceFirstChar { it.uppercase() }}",
            annotations = annotations
        )
    }
}
