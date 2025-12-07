package com.tietoevry.banking.asyncapi.generator.core.generator.java.transaction

import com.tietoevry.banking.asyncapi.generator.core.generator.AbstractJavaGeneratorClass
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals

class GenerateTransactionTest : AbstractJavaGeneratorClass() {

    @Test
    fun generate_asyncapi_simple_transaction_type_SimpleTransactionType_dataClass() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_simple_transaction_type.yaml"),
            generated = "SimpleTransactionType.java",
            modelPackage = "com.tietoevry.banking.asyncapi.generator.core.model.generated.transaction",
        )
        val classBody = extractClassBody(generated)
        val expected = """
               public class SimpleTransactionType implements Serializable {

                   @NotNull
                   private UUID transactionId;

                   @DecimalMin(value = "-1000000000", inclusive = true)
                   @DecimalMax(value = "1000000000", inclusive = true)
                   @NotNull
                   private BigDecimal amount;

                   @Size(min = 3, max = 3)
                   @Pattern(regexp = "[A-Z]{3}")
                   @NotNull
                   private String currency;

                   @NotNull
                   private LocalDate bookingDate;

                   @NotNull
                   private OffsetDateTime createdAt;

                   @NotNull
                   private Boolean active;

                   @Size(max = 200)
                   private String description;

                   public SimpleTransactionType() {
                       // Default constructor
                   }

                   // All-args constructor
                   public SimpleTransactionType(
                       UUID transactionId,
                       BigDecimal amount,
                       String currency,
                       LocalDate bookingDate,
                       OffsetDateTime createdAt,
                       Boolean active,
                       String description
                   ) {
                       this.transactionId = transactionId;
                       this.amount = amount;
                       this.currency = currency;
                       this.bookingDate = bookingDate;
                       this.createdAt = createdAt;
                       this.active = active;
                       this.description = description;
                   }

                   /**
                    * Get transactionId.
                    * Globally unique identifier for this transaction.
                    * @return UUID
                    */
                   public UUID getTransactionId() {
                       return transactionId;
                   }

                   /**
                    * Set transactionId.
                    * @param transactionId Globally unique identifier for this transaction.
                    */
                   public void setTransactionId(UUID transactionId) {
                       this.transactionId = transactionId;
                   }

                   /**
                    * Get amount.
                    * Monetary amount of this transaction.
                    * @return BigDecimal
                    */
                   public BigDecimal getAmount() {
                       return amount;
                   }

                   /**
                    * Set amount.
                    * @param amount Monetary amount of this transaction.
                    */
                   public void setAmount(BigDecimal amount) {
                       this.amount = amount;
                   }

                   /**
                    * Get currency.
                    * ISO-4217 currency code.
                    * @return String
                    */
                   public String getCurrency() {
                       return currency;
                   }

                   /**
                    * Set currency.
                    * @param currency ISO-4217 currency code.
                    */
                   public void setCurrency(String currency) {
                       this.currency = currency;
                   }

                   /**
                    * Get bookingDate.
                    * Booking date in system (local ledger) time.
                    * @return LocalDate
                    */
                   public LocalDate getBookingDate() {
                       return bookingDate;
                   }

                   /**
                    * Set bookingDate.
                    * @param bookingDate Booking date in system (local ledger) time.
                    */
                   public void setBookingDate(LocalDate bookingDate) {
                       this.bookingDate = bookingDate;
                   }

                   /**
                    * Get createdAt.
                    * Timestamp when the transaction was created.
                    * @return OffsetDateTime
                    */
                   public OffsetDateTime getCreatedAt() {
                       return createdAt;
                   }

                   /**
                    * Set createdAt.
                    * @param createdAt Timestamp when the transaction was created.
                    */
                   public void setCreatedAt(OffsetDateTime createdAt) {
                       this.createdAt = createdAt;
                   }

                   /**
                    * Get active.
                    * Indicates whether the transaction is currently active.
                    * @return Boolean
                    */
                   public Boolean getActive() {
                       return active;
                   }

                   /**
                    * Set active.
                    * @param active Indicates whether the transaction is currently active.
                    */
                   public void setActive(Boolean active) {
                       this.active = active;
                   }

                   /**
                    * Get description.
                    * Optional human-readable description of the transaction. This can be shown in statements or UI.
                    * @return String
                    */
                   public String getDescription() {
                       return description;
                   }

                   /**
                    * Set description.
                    * @param description Optional human-readable description of the transaction. This can be shown in statements or UI.
                    */
                   public void setDescription(String description) {
                       this.description = description;
                   }

                   @Override
                   public boolean equals(Object o) {
                       if (this == o) return true;
                       if (o == null || getClass() != o.getClass()) return false;
                       SimpleTransactionType that = (SimpleTransactionType) o;
                       return
                           Objects.equals(transactionId, that.transactionId) &&

                           Objects.equals(amount, that.amount) &&

                           Objects.equals(currency, that.currency) &&

                           Objects.equals(bookingDate, that.bookingDate) &&

                           Objects.equals(createdAt, that.createdAt) &&

                           Objects.equals(active, that.active) &&

                           Objects.equals(description, that.description)
               ;
                   }

                   @Override
                   public int hashCode() {
                       return Objects.hash(
               
                           transactionId,
                           amount,
                           currency,
                           bookingDate,
                           createdAt,
                           active,
                           description
                       );
                   }

                   @Override
                   public String toString() {
                       StringBuilder sb = new StringBuilder();
                       sb.append("class SimpleTransactionType {\n");
                       sb.append("    transactionId: ").append(transactionId).append("\n");
                       sb.append("    amount: ").append(amount).append("\n");
                       sb.append("    currency: ").append(currency).append("\n");
                       sb.append("    bookingDate: ").append(bookingDate).append("\n");
                       sb.append("    createdAt: ").append(createdAt).append("\n");
                       sb.append("    active: ").append(active).append("\n");
                       sb.append("    description: ").append(description).append("\n");
                       sb.append("}");
                       return sb.toString();
                   }
               }
           """.trimIndent()
        assertEquals(expected, classBody)
    }
}
