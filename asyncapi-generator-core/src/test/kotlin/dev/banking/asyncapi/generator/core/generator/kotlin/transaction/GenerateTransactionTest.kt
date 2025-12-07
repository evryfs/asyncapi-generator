package dev.banking.asyncapi.generator.core.generator.kotlin.transaction

import dev.banking.asyncapi.generator.core.generator.AbstractKotlinGeneratorClass
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals

class GenerateTransactionTest : AbstractKotlinGeneratorClass() {

    @Test
    fun generate_asyncapi_simple_transaction_type_SimpleTransactionType_dataClass() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_simple_transaction_type.yaml"),
            generated = "SimpleTransactionType.kt",
            modelPackage = "dev.banking.asyncapi.generator.core.model.generated.transaction",
        )
        val dataClass = extractElement(generated)
        val expected = """
        data class SimpleTransactionType(

            val transactionId: UUID,

            @field:DecimalMin(value = "-1000000000", inclusive = true)
            @field:DecimalMax(value = "1000000000", inclusive = true)
            val amount: BigDecimal,

            @field:Size(min = 3, max = 3)
            @field:Pattern(regexp = "[A-Z]{3}")
            val currency: String,

            val bookingDate: LocalDate,

            val createdAt: OffsetDateTime,

            val active: Boolean,

            @field:Size(max = 200)
            val description: String? = null
        ) {
        }
    """.trimIndent()
        assertEquals(expected, dataClass)
    }
}
