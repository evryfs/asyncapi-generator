package com.tietoevry.banking.asyncapi.generator.core.generator.java.mapper

import com.tietoevry.banking.asyncapi.generator.core.generator.java.model.JavaFieldTemplate
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class ImportMapperTest {

    private val mapper = ImportMapper("com.example.model")

    @Test
    fun `computeImports should add imports for java built-ins`() {
        val fields = listOf(
            JavaFieldTemplate(
                name = "id",
                type = "UUID",
                getterName = "getId",
                setterName = "setId",
                docFirstLine = null
            ),
            JavaFieldTemplate(
                name = "createdAt",
                type = "OffsetDateTime",
                getterName = "getCreatedAt",
                setterName = "setCreatedAt",
                docFirstLine = null
            )
        )

        val imports = mapper.computeImports("MyClass", fields)

        assertTrue(imports.contains("java.util.UUID"))
        assertTrue(imports.contains("java.time.OffsetDateTime"))
    }

    @Test
    fun `computeImports should add imports for validation annotations`() {
        val fields = listOf(
            JavaFieldTemplate(
                name = "email",
                type = "String",
                getterName = "getEmail",
                setterName = "setEmail",
                docFirstLine = null,
                annotations = listOf("@Email", "@Size(min=5)")
            )
        )

        val imports = mapper.computeImports("MyClass", fields)

        assertTrue(imports.contains("jakarta.validation.constraints.Email"))
        assertTrue(imports.contains("jakarta.validation.constraints.Size"))
    }

    @Test
    fun `computeImports should import models from same package`() {
        val fields = listOf(
            JavaFieldTemplate(
                name = "user",
                type = "User",
                getterName = "getUser",
                setterName = "setUser",
                docFirstLine = null
            )
        )

        val imports = mapper.computeImports("MyClass", fields)

        assertTrue(imports.contains("com.example.model.User"))
    }
}
