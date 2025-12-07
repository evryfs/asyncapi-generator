package com.tietoevry.banking.asyncapi.generator.core.generator.kotlin.allof

import com.tietoevry.banking.asyncapi.generator.core.generator.AbstractKotlinGeneratorClass
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals

class GenerateAllOfTest : AbstractKotlinGeneratorClass() {

    @Test
    fun generate_asyncapi_allOf_composition_BaseAccount_dataClass() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_allof_composition.yaml"),
            generated = "BaseAccount.kt",
            modelPackage = "com.tietoevry.banking.asyncapi.generator.core.model.generated.allof.composition",
        )
        val dataClass = extractElement(generated)
        val expected = """
        data class BaseAccount(

            @field:Size(min = 4, max = 35)
            val accountId: String,

            val active: Boolean? = null
        ) {
        }
    """.trimIndent()
        assertEquals(expected, dataClass)
    }

    @Test
    fun generate_asyncapi_allOf_composition_ExtendedAccount_dataClass() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_allof_composition.yaml"),
            generated = "ExtendedAccount.kt",
            modelPackage = "com.tietoevry.banking.asyncapi.generator.core.model.generated.allof.composition",
        )
        val dataClass = extractElement(generated)
        val expected = """
        data class ExtendedAccount(

            @field:DecimalMin(value = "0", inclusive = true)
            val overdraftLimit: BigDecimal? = null,

            val accountType: AccountType,

            @field:Size(min = 4, max = 35)
            val accountId: String,

            val active: Boolean? = null
        ) {
        }
    """.trimIndent()
        assertEquals(expected, dataClass)
    }

    @Test
    fun generate_AccountType_enum_from_allOf_composition() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_allof_composition.yaml"),
            generated = "AccountType.kt",
            modelPackage = "com.tietoevry.banking.asyncapi.generator.core.model.generated.allof.composition",
        )
        val enumClass = extractElement(generated)

        val expected = """
           enum class AccountType {
               CURRENT,
               SAVINGS,
           }
           """.trimIndent()

        assertEquals(expected, enumClass)
    }

    @Test
    fun generate_asyncapi_allOf_overrides_Dog_dataClass() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_allof_overrides.yaml"),
            generated = "Dog.kt",
            modelPackage = "com.tietoevry.banking.asyncapi.generator.core.model.generated.allof.overrides",
        )
        val dataClass = extractElement(generated)
        val expected = """
           data class Dog(

               @field:Min(4L)
               val legs: Int? = null,

               val breed: String,

               val name: String
           ) {
           }
       """.trimIndent()
        assertEquals(expected, dataClass)
    }

    @Test
    fun generate_asyncapi_allOf_constraint_intersection_ExtendedRange_dataClass() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_allof_constraint_intersection.yaml"),
            generated = "ExtendedRange.kt",
            modelPackage = "com.tietoevry.banking.asyncapi.generator.core.model.generated.allof.constraint",
        )
        val dataClass = extractElement(generated)
        val expected = """
        data class ExtendedRange(

            @field:DecimalMin(value = "10", inclusive = true)
            @field:DecimalMax(value = "50", inclusive = true)
            val value: BigDecimal,

            val label: String
        ) {
        }
    """.trimIndent()
        assertEquals(expected, dataClass)
    }
}
