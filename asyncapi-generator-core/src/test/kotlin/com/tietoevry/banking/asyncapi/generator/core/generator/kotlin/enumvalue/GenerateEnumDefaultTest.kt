package com.tietoevry.banking.asyncapi.generator.core.generator.kotlin.enumvalue

import com.tietoevry.banking.asyncapi.generator.core.generator.AbstractKotlinGeneratorClass
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals

class GenerateEnumDefaultTest : AbstractKotlinGeneratorClass() {

    @Test
    fun generate_data_class_with_enum_default_values() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_enum_default_value.yaml"),
            generated = "Task.kt",
            modelPackage = "com.tietoevry.banking.asyncapi.generator.core.model.generated.enumdefault",
        )
        val dataClass = extractElement(generated)

        val expected = """
           data class Task(

               val id: String? = null,

               val status: TaskStatus? = TaskStatus.IN_PROGRESS,

               val priority: Priority? = Priority.MEDIUM
           ) {
           }
           """.trimIndent()

        assertEquals(expected, dataClass)
    }

    @Test
    fun generate_TaskStatus_enum() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_enum_default_value.yaml"),
            generated = "TaskStatus.kt",
            modelPackage = "com.tietoevry.banking.asyncapi.generator.core.model.generated.enumdefault",
        )
        val enumClass = extractElement(generated)

        val expected = """
           enum class TaskStatus {
               OPEN,
               IN_PROGRESS,
               CLOSED,
           }
           """.trimIndent()

        assertEquals(expected, enumClass)
    }

    @Test
    fun generate_Priority_enum() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_enum_default_value.yaml"),
            generated = "Priority.kt",
            modelPackage = "com.tietoevry.banking.asyncapi.generator.core.model.generated.enumdefault",
        )
        val enumClass = extractElement(generated)

        val expected = """
           enum class Priority {
               LOW,
               MEDIUM,
               HIGH,
           }
           """.trimIndent()

        assertEquals(expected, enumClass)
    }
}
