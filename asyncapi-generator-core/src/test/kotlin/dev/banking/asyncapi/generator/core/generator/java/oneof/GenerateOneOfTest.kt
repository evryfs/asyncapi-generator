package dev.banking.asyncapi.generator.core.generator.java.oneof

import dev.banking.asyncapi.generator.core.generator.AbstractJavaGeneratorClass
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals

class GenerateOneOfTest : AbstractJavaGeneratorClass() {

    @Test
    fun generate_java_oneOf_composition_Payment_interface() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_oneof_composition.yaml"),
            generated = "Payment.java",
            modelPackage = "dev.banking.asyncapi.generator.core.model.generated.oneof.composition",
        )
        val javaInterface = extractClassBody(generated)
        val expected = """
            public interface Payment extends Serializable {
            }
        """.trimIndent()
        assertEquals(expected, javaInterface)
    }

    @Test
    fun generate_java_oneOf_composition_PaymentType_enum() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_oneof_composition.yaml"),
            generated = "PaymentType.java",
            modelPackage = "dev.banking.asyncapi.generator.core.model.generated.oneof",
        )
        val javaEnum = extractClassBody(generated)
        val expected = """
            public enum PaymentType implements Serializable {
                CARD,
                BANK,
            }
        """.trimIndent()
        assertEquals(expected, javaEnum)
    }

    @Test
    fun generate_java_oneOf_composition_PaymentBase_class() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_oneof_composition.yaml"),
            generated = "PaymentBase.java",
            modelPackage = "dev.banking.asyncapi.generator.core.model.generated.oneof.composition",
        )
        val javaClass = extractClassBody(generated)
        val expected = """
            public class PaymentBase implements Serializable {

                @NotNull
                private PaymentType paymentType;

                public PaymentBase() {
                    // Default constructor
                }

                // All-args constructor
                public PaymentBase(
                    PaymentType paymentType
                ) {
                    this.paymentType = paymentType;
                }

                /**
                 * Get paymentType.
                 * @return PaymentType
                 */
                public PaymentType getPaymentType() {
                    return paymentType;
                }

                /**
                 * Set paymentType.
                 * @param paymentType
                 */
                public void setPaymentType(PaymentType paymentType) {
                    this.paymentType = paymentType;
                }

                @Override
                public boolean equals(Object o) {
                    if (this == o) return true;
                    if (o == null || getClass() != o.getClass()) return false;
                    PaymentBase that = (PaymentBase) o;
                    return
                        Objects.equals(paymentType, that.paymentType)
            ;
                }

                @Override
                public int hashCode() {
                    return Objects.hash(
            
                        paymentType
                    );
                }

                @Override
                public String toString() {
                    StringBuilder sb = new StringBuilder();
                    sb.append("class PaymentBase {\n");
                    sb.append("    paymentType: ").append(paymentType).append("\n");
                    sb.append("}");
                    return sb.toString();
                }
            }
        """.trimIndent()
        assertEquals(expected, javaClass)
    }

    @Test
    fun generate_java_oneOf_composition_CardPayment_class() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_oneof_composition.yaml"),
            generated = "CardPayment.java",
            modelPackage = "dev.banking.asyncapi.generator.core.model.generated.oneof.composition",
        )
        val javaClass = extractClassBody(generated)
        val expected = """
               public class CardPayment implements Payment, Serializable {

                   @NotNull
                   private PaymentType paymentType;

                   @Pattern(regexp = "^[0-9]{16}$")
                   @NotNull
                   private String cardNumber;

                   public CardPayment() {
                       // Default constructor
                   }

                   // All-args constructor
                   public CardPayment(
                       PaymentType paymentType,
                       String cardNumber
                   ) {
                       this.paymentType = paymentType;
                       this.cardNumber = cardNumber;
                   }

                   /**
                    * Get paymentType.
                    * @return PaymentType
                    */
                   public PaymentType getPaymentType() {
                       return paymentType;
                   }

                   /**
                    * Set paymentType.
                    * @param paymentType
                    */
                   public void setPaymentType(PaymentType paymentType) {
                       this.paymentType = paymentType;
                   }

                   /**
                    * Get cardNumber.
                    * @return String
                    */
                   public String getCardNumber() {
                       return cardNumber;
                   }

                   /**
                    * Set cardNumber.
                    * @param cardNumber
                    */
                   public void setCardNumber(String cardNumber) {
                       this.cardNumber = cardNumber;
                   }

                   @Override
                   public boolean equals(Object o) {
                       if (this == o) return true;
                       if (o == null || getClass() != o.getClass()) return false;
                       CardPayment that = (CardPayment) o;
                       return
                           Objects.equals(paymentType, that.paymentType) &&
               
                           Objects.equals(cardNumber, that.cardNumber)
               ;
                   }

                   @Override
                   public int hashCode() {
                       return Objects.hash(
               
                           paymentType,
                           cardNumber
                       );
                   }

                   @Override
                   public String toString() {
                       StringBuilder sb = new StringBuilder();
                       sb.append("class CardPayment {\n");
                       sb.append("    paymentType: ").append(paymentType).append("\n");
                       sb.append("    cardNumber: ").append(cardNumber).append("\n");
                       sb.append("}");
                       return sb.toString();
                   }
               }
           """.trimIndent()
        assertEquals(expected, javaClass)
    }

    @Test
    fun generate_java_oneOf_composition_BankPayment_class() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_oneof_composition.yaml"),
            generated = "BankPayment.java",
            modelPackage = "dev.banking.asyncapi.generator.core.model.generated.oneof.composition",
        )
        val javaClass = extractClassBody(generated)
        val expected = """
            public class BankPayment implements Payment, Serializable {

                @NotNull
                private PaymentType paymentType;
            
                @Pattern(regexp = "^[A-Z0-9]{15,34}$")
                @NotNull
                private String iban;
            
                public BankPayment() {
                    // Default constructor
                }
            
                // All-args constructor
                public BankPayment(
                    PaymentType paymentType,
                    String iban
                ) {
                    this.paymentType = paymentType;
                    this.iban = iban;
                }
            
                /**
                 * Get paymentType.
                 * @return PaymentType
                 */
                public PaymentType getPaymentType() {
                    return paymentType;
                }
            
                /**
                 * Set paymentType.
                 * @param paymentType
                 */
                public void setPaymentType(PaymentType paymentType) {
                    this.paymentType = paymentType;
                }
            
                /**
                 * Get iban.
                 * @return String
                 */
                public String getIban() {
                    return iban;
                }
            
                /**
                 * Set iban.
                 * @param iban
                 */
                public void setIban(String iban) {
                    this.iban = iban;
                }
            
                @Override
                public boolean equals(Object o) {
                    if (this == o) return true;
                    if (o == null || getClass() != o.getClass()) return false;
                    BankPayment that = (BankPayment) o;
                    return
                        Objects.equals(paymentType, that.paymentType) &&
            
                        Objects.equals(iban, that.iban)
            ;
                }
            
                @Override
                public int hashCode() {
                    return Objects.hash(
            
                        paymentType,
                        iban
                    );
                }
            
                @Override
                public String toString() {
                    StringBuilder sb = new StringBuilder();
                    sb.append("class BankPayment {\n");
                    sb.append("    paymentType: ").append(paymentType).append("\n");
                    sb.append("    iban: ").append(iban).append("\n");
                    sb.append("}");
                    return sb.toString();
                }
            }
        """.trimIndent()
        assertEquals(expected, javaClass)
    }
}
