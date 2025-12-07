package com.tietoevry.banking.asyncapi.generator.core.generator.java.moneyamount

import com.tietoevry.banking.asyncapi.generator.core.generator.AbstractJavaGeneratorClass
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals

class GenerateMoneyAmountTest : AbstractJavaGeneratorClass() {

    @Test
    fun generate_asyncapi_money_amount_type_MoneyAmountType_dataClass() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_money_amount_type.yaml"),
            generated = "MoneyAmountType.java",
            modelPackage = "com.tietoevry.banking.asyncapi.generator.core.model.generated.moneyamount",
        )
        val classBody = extractClassBody(generated)
        val expected = """
               public class MoneyAmountType implements Serializable {

                   @Size(min = 3, max = 3)
                   @Pattern(regexp = "[A-Z]{3}")
                   @NotNull
                   private String currency;

                   @DecimalMin(value = "-1.0E12", inclusive = false)
                   @DecimalMax(value = "1.0E12", inclusive = false)
                   @NotNull
                   private BigDecimal value;

                   public MoneyAmountType() {
                       // Default constructor
                   }

                   // All-args constructor
                   public MoneyAmountType(
                       String currency,
                       BigDecimal value
                   ) {
                       this.currency = currency;
                       this.value = value;
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
                    * Get value.
                    * Signed monetary value, in currency minor units.
                    * @return BigDecimal
                    */
                   public BigDecimal getValue() {
                       return value;
                   }

                   /**
                    * Set value.
                    * @param value Signed monetary value, in currency minor units.
                    */
                   public void setValue(BigDecimal value) {
                       this.value = value;
                   }

                   @Override
                   public boolean equals(Object o) {
                       if (this == o) return true;
                       if (o == null || getClass() != o.getClass()) return false;
                       MoneyAmountType that = (MoneyAmountType) o;
                       return
                           Objects.equals(currency, that.currency) &&

                           Objects.equals(value, that.value)
               ;
                   }

                   @Override
                   public int hashCode() {
                       return Objects.hash(
               
                           currency,
                           value
                       );
                   }

                   @Override
                   public String toString() {
                       StringBuilder sb = new StringBuilder();
                       sb.append("class MoneyAmountType {\n");
                       sb.append("    currency: ").append(currency).append("\n");
                       sb.append("    value: ").append(value).append("\n");
                       sb.append("}");
                       return sb.toString();
                   }
               }
           """.trimIndent()
        assertEquals(expected, classBody)
    }

}
