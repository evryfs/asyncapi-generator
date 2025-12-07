package com.tietoevry.banking.asyncapi.generator.core.generator.kotlin.complexorder

import com.tietoevry.banking.asyncapi.generator.core.generator.AbstractKotlinGeneratorClass
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals

class GenerateComplexOrderTest : AbstractKotlinGeneratorClass() {

    @Test
    fun generate_asyncapi_complex_order_payload_type_OrderLineType_dataClass() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_complex_order_payload_type.yaml"),
            generated = "OrderLineType.kt",
            modelPackage = "com.tietoevry.banking.asyncapi.generator.core.model.generated.complexorder",
        )
        val dataClass = extractElement(generated)
        val expected = """
        data class OrderLineType(

            @field:Size(min = 1, max = 50)
            val productCode: String,

            @field:Min(1L)
            @field:Max(100000L)
            val quantity: Int,

            @field:DecimalMin(value = "0", inclusive = false)
            val unitPrice: BigDecimal
        ) {
        }
    """.trimIndent()
        assertEquals(expected, dataClass)
    }

    @Test
    fun generate_asyncapi_complex_order_payload_type_ComplexOrderPayloadType_dataClass() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_complex_order_payload_type.yaml"),
            generated = "ComplexOrderPayloadType.kt",
            modelPackage = "com.tietoevry.banking.asyncapi.generator.core.model.generated.complexorder",
        )
        val dataClass = extractElement(generated)
        val expected = """
        data class ComplexOrderPayloadType(

            val orderId: UUID,

            val createdAt: OffsetDateTime,

            @field:Size(min = 3, max = 30)
            val status: String,

            @field:Valid
            val customer: CustomerWithContacts,

            @field:Valid
            val orderLines: List<OrderLineType>,

            @field:Size(max = 2000)
            val notes: String? = null
        ) {
        }
    """.trimIndent()
        assertEquals(expected, dataClass)
    }

    @Test
    fun generate_CustomerWithContacts_dataClass_from_referenced_file() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_complex_order_payload_type.yaml"),
            generated = "CustomerWithContacts.kt",
            modelPackage = "com.tietoevry.banking.asyncapi.generator.core.model.generated.complexorder",
        )
        val dataClass = extractElement(generated)

        val expected = """
           data class CustomerWithContacts(

               val customerId: UUID,

               @field:Size(max = 140)
               val fullName: String,

               val tags: List<String>? = null,

               @field:Valid
               val contactPoints: List<ContactPointType>
           ) {
           }
           """.trimIndent()

        assertEquals(expected, dataClass)
    }
}
