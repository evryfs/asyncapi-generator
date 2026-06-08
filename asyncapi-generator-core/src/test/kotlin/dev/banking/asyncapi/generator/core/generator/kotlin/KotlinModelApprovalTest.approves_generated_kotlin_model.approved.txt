package dev.banking.asyncapi.generator.core.model.generated.transaction

import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

/**
 * A very small transaction object used to validate primitive mappings and basic constraints in the Kotlin generator.
 *
 * @param [transactionId] Globally unique identifier for this transaction.
 *
 * @param [amount] Monetary amount of this transaction.
 *
 * @param [currency] ISO-4217 currency code.
 *
 * @param [bookingDate] Booking date in system (local ledger) time.
 *
 * @param [createdAt] Timestamp when the transaction was created.
 *
 * @param [active] Indicates whether the transaction is currently active.
 *
 * @param [description] Optional human-readable description of the transaction. This can be shown in statements or UI.
 *
 */
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
