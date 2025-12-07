package com.tietoevry.banking.asyncapi.generator.core.generator.kotlin.oneof

import com.tietoevry.banking.asyncapi.generator.core.generator.AbstractKotlinGeneratorClass
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals

class GenerateOneOfTest : AbstractKotlinGeneratorClass() {

    @Test
    fun generate_asyncapi_oneOf_composition_PaymentBase_dataClass() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_oneof_composition.yaml"),
            generated = "PaymentBase.kt",
            modelPackage = "com.tietoevry.banking.asyncapi.generator.core.model.generated.oneof.composition",
        )
        val dataClass = extractElement(generated)
        val expected = """
        data class PaymentBase(

            val paymentType: PaymentType
        ) {
        }
    """.trimIndent()
        assertEquals(expected, dataClass)
    }

    @Test
    fun generate_asyncapi_oneOf_composition_CardPayment_dataClass() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_oneof_composition.yaml"),
            generated = "CardPayment.kt",
            modelPackage = "com.tietoevry.banking.asyncapi.generator.core.model.generated.oneof.composition",
        )
        val dataClass = extractElement(generated)
        val expected = """
        data class CardPayment(

            val paymentType: PaymentType,

            @field:Pattern(regexp = "^[0-9]{16}$")
            val cardNumber: String
        ) : Payment {
        }
    """.trimIndent()
        assertEquals(expected, dataClass)
    }

    @Test
    fun generate_asyncapi_oneOf_composition_BankPayment_dataClass() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_oneof_composition.yaml"),
            generated = "BankPayment.kt",
            modelPackage = "com.tietoevry.banking.asyncapi.generator.core.model.generated.oneof.composition",
        )
        val dataClass = extractElement(generated)
        val expected = """
        data class BankPayment(

            val paymentType: PaymentType,

            @field:Pattern(regexp = "^[A-Z0-9]{15,34}$")
            val iban: String
        ) : Payment {
        }
    """.trimIndent()
        assertEquals(expected, dataClass)
    }

    @Test
    fun generate_PaymentType_enum_from_oneOf_composition() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_oneof_composition.yaml"),
            generated = "PaymentType.kt",
            modelPackage = "com.tietoevry.banking.asyncapi.generator.core.model.generated.oneof",
        )
        val enumClass = extractElement(generated)

        val expected = """
           enum class PaymentType {
               CARD,
               BANK,
           }
           """.trimIndent()

        assertEquals(expected, enumClass)
    }

    @Test
    fun generate_asyncapi_oneOf_composition_Payment_sealedInterface() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_oneof_composition.yaml"),
            generated = "Payment.kt",
            modelPackage = "com.tietoevry.banking.asyncapi.generator.core.model.generated.oneof.composition",
        )
        val sealedInterface = extractElement(generated)
        val expected = """
        sealed interface Payment
    """.trimIndent()
        assertEquals(expected, sealedInterface)
    }
}
