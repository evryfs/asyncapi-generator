package com.tietoevry.banking.asyncapi.generator.core.generator.kotlin.mapper

import com.tietoevry.banking.asyncapi.generator.core.generator.kotlin.model.KotlinFieldTemplate
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class ImportMapperTest {

    private val mapper = ImportMapper("com.example.model")

    @Test
    fun `computeImports should add imports for java built-ins`() {
        val fields = listOf(
            KotlinFieldTemplate(name = "id", type = "UUID", nullable = false, docFirstLine = null),
            KotlinFieldTemplate(name = "createdAt", type = "OffsetDateTime", nullable = false, docFirstLine = null)
        )

        val imports = mapper.computeImports("MyClass", fields)

        assertTrue(imports.contains("java.util.UUID"))
        assertTrue(imports.contains("java.time.OffsetDateTime"))
    }

    @Test
    fun `computeImports should add imports for validation annotations`() {
        val fields = listOf(
            KotlinFieldTemplate(
                name = "email",
                type = "String",
                nullable = false,
                docFirstLine = null,
                annotations = listOf("@field:Email", "@field:Size(min=5)")
            )
        )

        val imports = mapper.computeImports("MyClass", fields)

        assertTrue(imports.contains("jakarta.validation.constraints.Email"))
        assertTrue(imports.contains("jakarta.validation.constraints.Size"))
    }

    @Test
    fun `computeImports should import models from same package`() {
        val fields = listOf(
            KotlinFieldTemplate(name = "user", type = "User", nullable = false, docFirstLine = null)
        )

        val imports = mapper.computeImports("MyClass", fields)

        assertTrue(imports.contains("com.example.model.User"))
    }
}
