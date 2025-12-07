package com.tietoevry.banking.asyncapi.generator.core.generator.java.allof

import com.tietoevry.banking.asyncapi.generator.core.generator.AbstractJavaGeneratorClass
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals

class GenerateAllOfTest : AbstractJavaGeneratorClass() {

    @Test
    fun generate_asyncapi_allOf_composition_BaseAccount_dataClass() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_allof_composition.yaml"),
            generated = "BaseAccount.java",
            modelPackage = "com.tietoevry.banking.asyncapi.generator.core.model.generated.allof.composition",
        )
        val classBody = extractClassBody(generated)
        val expected = """
               public class BaseAccount implements Serializable {

                   @Size(min = 4, max = 35)
                   @NotNull
                   private String accountId;

                   private Boolean active;

                   public BaseAccount() {
                       // Default constructor
                   }

                   // All-args constructor
                   public BaseAccount(
                       String accountId,
                       Boolean active
                   ) {
                       this.accountId = accountId;
                       this.active = active;
                   }

                   /**
                    * Get accountId.
                    * @return String
                    */
                   public String getAccountId() {
                       return accountId;
                   }

                   /**
                    * Set accountId.
                    * @param accountId
                    */
                   public void setAccountId(String accountId) {
                       this.accountId = accountId;
                   }

                   /**
                    * Get active.
                    * @return Boolean
                    */
                   public Boolean getActive() {
                       return active;
                   }

                   /**
                    * Set active.
                    * @param active
                    */
                   public void setActive(Boolean active) {
                       this.active = active;
                   }

                   @Override
                   public boolean equals(Object o) {
                       if (this == o) return true;
                       if (o == null || getClass() != o.getClass()) return false;
                       BaseAccount that = (BaseAccount) o;
                       return
                           Objects.equals(accountId, that.accountId) &&

                           Objects.equals(active, that.active)
               ;
                   }

                   @Override
                   public int hashCode() {
                       return Objects.hash(
               
                           accountId,
                           active
                       );
                   }

                   @Override
                   public String toString() {
                       StringBuilder sb = new StringBuilder();
                       sb.append("class BaseAccount {\n");
                       sb.append("    accountId: ").append(accountId).append("\n");
                       sb.append("    active: ").append(active).append("\n");
                       sb.append("}");
                       return sb.toString();
                   }
               }
           """.trimIndent()

        assertEquals(expected, classBody)
    }

    @Test
    fun generate_asyncapi_allOf_composition_ExtendedAccount_dataClass() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_allof_composition.yaml"),
            generated = "ExtendedAccount.java",
            modelPackage = "com.tietoevry.banking.asyncapi.generator.core.model.generated.allof.composition",
        )
        val classBody = extractClassBody(generated)
        val expected = """
               public class ExtendedAccount implements Serializable {

                   @DecimalMin(value = "0", inclusive = true)
                   private BigDecimal overdraftLimit;

                   @NotNull
                   private AccountType accountType;

                   @Size(min = 4, max = 35)
                   @NotNull
                   private String accountId;

                   private Boolean active;

                   public ExtendedAccount() {
                       // Default constructor
                   }

                   // All-args constructor
                   public ExtendedAccount(
                       BigDecimal overdraftLimit,
                       AccountType accountType,
                       String accountId,
                       Boolean active
                   ) {
                       this.overdraftLimit = overdraftLimit;
                       this.accountType = accountType;
                       this.accountId = accountId;
                       this.active = active;
                   }

                   /**
                    * Get overdraftLimit.
                    * @return BigDecimal
                    */
                   public BigDecimal getOverdraftLimit() {
                       return overdraftLimit;
                   }

                   /**
                    * Set overdraftLimit.
                    * @param overdraftLimit
                    */
                   public void setOverdraftLimit(BigDecimal overdraftLimit) {
                       this.overdraftLimit = overdraftLimit;
                   }

                   /**
                    * Get accountType.
                    * @return AccountType
                    */
                   public AccountType getAccountType() {
                       return accountType;
                   }

                   /**
                    * Set accountType.
                    * @param accountType
                    */
                   public void setAccountType(AccountType accountType) {
                       this.accountType = accountType;
                   }

                   /**
                    * Get accountId.
                    * @return String
                    */
                   public String getAccountId() {
                       return accountId;
                   }

                   /**
                    * Set accountId.
                    * @param accountId
                    */
                   public void setAccountId(String accountId) {
                       this.accountId = accountId;
                   }

                   /**
                    * Get active.
                    * @return Boolean
                    */
                   public Boolean getActive() {
                       return active;
                   }

                   /**
                    * Set active.
                    * @param active
                    */
                   public void setActive(Boolean active) {
                       this.active = active;
                   }

                   @Override
                   public boolean equals(Object o) {
                       if (this == o) return true;
                       if (o == null || getClass() != o.getClass()) return false;
                       ExtendedAccount that = (ExtendedAccount) o;
                       return
                           Objects.equals(overdraftLimit, that.overdraftLimit) &&

                           Objects.equals(accountType, that.accountType) &&

                           Objects.equals(accountId, that.accountId) &&

                           Objects.equals(active, that.active)
               ;
                   }

                   @Override
                   public int hashCode() {
                       return Objects.hash(
               
                           overdraftLimit,
                           accountType,
                           accountId,
                           active
                       );
                   }

                   @Override
                   public String toString() {
                       StringBuilder sb = new StringBuilder();
                       sb.append("class ExtendedAccount {\n");
                       sb.append("    overdraftLimit: ").append(overdraftLimit).append("\n");
                       sb.append("    accountType: ").append(accountType).append("\n");
                       sb.append("    accountId: ").append(accountId).append("\n");
                       sb.append("    active: ").append(active).append("\n");
                       sb.append("}");
                       return sb.toString();
                   }
               }
           """.trimIndent()
        assertEquals(expected, classBody)
    }

    @Test
    fun generate_AccountType_enum_from_allOf_composition() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_allof_composition.yaml"),
            generated = "AccountType.java",
            modelPackage = "com.tietoevry.banking.asyncapi.generator.core.model.generated.allof.composition",
        )
        val classBody = extractClassBody(generated)
        val expected = """
               public enum AccountType implements Serializable {
                   CURRENT,
                   SAVINGS,
               }
           """.trimIndent()

        assertEquals(expected, classBody)
    }

    @Test
    fun generate_asyncapi_allOf_overrides_Dog_dataClass() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_allof_overrides.yaml"),
            generated = "Dog.java",
            modelPackage = "com.tietoevry.banking.asyncapi.generator.core.model.generated.allof.overrides",
        )
        val classBody = extractClassBody(generated)
        val expected = """
               public class Dog implements Serializable {

                   @Min(4L)
                   private Integer legs;

                   @NotNull
                   private String breed;

                   @NotNull
                   private String name;

                   public Dog() {
                       // Default constructor
                   }

                   // All-args constructor
                   public Dog(
                       Integer legs,
                       String breed,
                       String name
                   ) {
                       this.legs = legs;
                       this.breed = breed;
                       this.name = name;
                   }

                   /**
                    * Get legs.
                    * @return Integer
                    */
                   public Integer getLegs() {
                       return legs;
                   }

                   /**
                    * Set legs.
                    * @param legs
                    */
                   public void setLegs(Integer legs) {
                       this.legs = legs;
                   }

                   /**
                    * Get breed.
                    * @return String
                    */
                   public String getBreed() {
                       return breed;
                   }

                   /**
                    * Set breed.
                    * @param breed
                    */
                   public void setBreed(String breed) {
                       this.breed = breed;
                   }

                   /**
                    * Get name.
                    * @return String
                    */
                   public String getName() {
                       return name;
                   }

                   /**
                    * Set name.
                    * @param name
                    */
                   public void setName(String name) {
                       this.name = name;
                   }

                   @Override
                   public boolean equals(Object o) {
                       if (this == o) return true;
                       if (o == null || getClass() != o.getClass()) return false;
                       Dog that = (Dog) o;
                       return
                           Objects.equals(legs, that.legs) &&

                           Objects.equals(breed, that.breed) &&

                           Objects.equals(name, that.name)
               ;
                   }

                   @Override
                   public int hashCode() {
                       return Objects.hash(
               
                           legs,
                           breed,
                           name
                       );
                   }

                   @Override
                   public String toString() {
                       StringBuilder sb = new StringBuilder();
                       sb.append("class Dog {\n");
                       sb.append("    legs: ").append(legs).append("\n");
                       sb.append("    breed: ").append(breed).append("\n");
                       sb.append("    name: ").append(name).append("\n");
                       sb.append("}");
                       return sb.toString();
                   }
               }
           """.trimIndent()
        assertEquals(expected, classBody)
    }

    @Test
    fun generate_asyncapi_allOf_constraint_intersection_ExtendedRange_dataClass() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_allof_constraint_intersection.yaml"),
            generated = "ExtendedRange.java",
            modelPackage = "com.tietoevry.banking.asyncapi.generator.core.model.generated.allof.constraint",
        )
        val classBody = extractClassBody(generated)
        val expected = """
               public class ExtendedRange implements Serializable {

                   @DecimalMin(value = "10", inclusive = true)
                   @DecimalMax(value = "50", inclusive = true)
                   @NotNull
                   private BigDecimal value;

                   @NotNull
                   private String label;

                   public ExtendedRange() {
                       // Default constructor
                   }

                   // All-args constructor
                   public ExtendedRange(
                       BigDecimal value,
                       String label
                   ) {
                       this.value = value;
                       this.label = label;
                   }

                   /**
                    * Get value.
                    * @return BigDecimal
                    */
                   public BigDecimal getValue() {
                       return value;
                   }

                   /**
                    * Set value.
                    * @param value
                    */
                   public void setValue(BigDecimal value) {
                       this.value = value;
                   }

                   /**
                    * Get label.
                    * @return String
                    */
                   public String getLabel() {
                       return label;
                   }

                   /**
                    * Set label.
                    * @param label
                    */
                   public void setLabel(String label) {
                       this.label = label;
                   }

                   @Override
                   public boolean equals(Object o) {
                       if (this == o) return true;
                       if (o == null || getClass() != o.getClass()) return false;
                       ExtendedRange that = (ExtendedRange) o;
                       return
                           Objects.equals(value, that.value) &&

                           Objects.equals(label, that.label)
               ;
                   }

                   @Override
                   public int hashCode() {
                       return Objects.hash(
               
                           value,
                           label
                       );
                   }

                   @Override
                   public String toString() {
                       StringBuilder sb = new StringBuilder();
                       sb.append("class ExtendedRange {\n");
                       sb.append("    value: ").append(value).append("\n");
                       sb.append("    label: ").append(label).append("\n");
                       sb.append("}");
                       return sb.toString();
                   }
               }
           """.trimIndent()

        assertEquals(expected, classBody)
    }
}
