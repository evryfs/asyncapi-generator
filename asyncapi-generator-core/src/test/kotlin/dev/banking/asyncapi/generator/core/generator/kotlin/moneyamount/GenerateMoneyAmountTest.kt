package dev.banking.asyncapi.generator.core.generator.kotlin.moneyamount

import dev.banking.asyncapi.generator.core.generator.AbstractKotlinGeneratorClass
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals

class GenerateMoneyAmountTest : AbstractKotlinGeneratorClass() {

    @Test
    fun generate_asyncapi_money_amount_type_MoneyAmountType_dataClass() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_money_amount_type.yaml"),
            generated = "MoneyAmountType.kt",
            modelPackage = "dev.banking.asyncapi.generator.core.model.generated.moneyamount",
        )
        val dataClass = extractElement(generated)
        val expected = """
        data class MoneyAmountType(

            @field:Size(min = 3, max = 3)
            @field:Pattern(regexp = "[A-Z]{3}")
            val currency: String,

            @field:DecimalMin(value = "-1.0E12", inclusive = false)
            @field:DecimalMax(value = "1.0E12", inclusive = false)
            val value: BigDecimal
        ) {
        }
    """.trimIndent()
        assertEquals(expected, dataClass)
    }
}
