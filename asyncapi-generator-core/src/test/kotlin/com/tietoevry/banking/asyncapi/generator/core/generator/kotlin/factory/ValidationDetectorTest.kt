package com.tietoevry.banking.asyncapi.generator.core.generator.kotlin.factory

import com.tietoevry.banking.asyncapi.generator.core.generator.context.GeneratorContext
import com.tietoevry.banking.asyncapi.generator.core.model.schemas.Schema
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ValidationDetectorTest {

    @Test
    fun `needsCascadedValidation should return true for known object schema`() {
        val schemas = mapOf("MyModel" to Schema(type = "object"))
        val context = GeneratorContext(schemas)
        val detector = ValidationDetector(context)

        assertTrue(detector.needsCascadedValidation("MyModel"))
        assertTrue(detector.needsCascadedValidation("MyModel?"))
    }

    @Test
    fun `needsCascadedValidation should return false for primitives`() {
        val context = GeneratorContext(emptyMap())
        val detector = ValidationDetector(context)

        assertFalse(detector.needsCascadedValidation("String"))
        assertFalse(detector.needsCascadedValidation("Int"))
        assertFalse(detector.needsCascadedValidation("java.util.UUID"))
    }

    @Test
    fun `needsCascadedValidation should handle List of models`() {
        val schemas = mapOf("MyItem" to Schema(type = "object"))
        val context = GeneratorContext(schemas)
        val detector = ValidationDetector(context)

        assertTrue(detector.needsCascadedValidation("List<MyItem>"))
        assertTrue(detector.needsCascadedValidation("List<MyItem>?"))
        assertFalse(detector.needsCascadedValidation("List<String>"))
    }

    @Test
    fun `needsCascadedValidation should handle Map values`() {
        val schemas = mapOf("MyValue" to Schema(type = "object"))
        val context = GeneratorContext(schemas)
        val detector = ValidationDetector(context)

        assertTrue(detector.needsCascadedValidation("Map<String, MyValue>"))
        assertFalse(detector.needsCascadedValidation("Map<String, Int>"))
    }

    @Test
    fun `needsCascadedValidation should assume PascalCase is model if schema missing`() {
        val context = GeneratorContext(emptyMap())
        val detector = ValidationDetector(context)

        assertTrue(detector.needsCascadedValidation("UnknownModel"))
        assertFalse(detector.needsCascadedValidation("unknownModel"))
    }
}
