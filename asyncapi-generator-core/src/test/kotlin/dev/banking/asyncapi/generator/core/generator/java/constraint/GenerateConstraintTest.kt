package dev.banking.asyncapi.generator.core.generator.java.constraint

import dev.banking.asyncapi.generator.core.generator.AbstractJavaGeneratorClass
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals

class GenerateConstraintTest : AbstractJavaGeneratorClass() {

    @Test
    fun generate_asyncapi_string_constraints_type_StringConstraintsType_dataClass() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_string_constraints_type.yaml"),
            generated = "StringConstraintsType.java",
            modelPackage = "dev.banking.asyncapi.generator.core.model.generated.constraint",
        )
        val classBody = extractClassBody(generated)
        val expected = """
               public class StringConstraintsType implements Serializable {

                   @Size(max = 255)
                   @Email
                   @NotNull
                   private String email;

                   @Size(min = 3, max = 30)
                   @Pattern(regexp = "[A-Za-z][A-Za-z0-9_-]*")
                   @NotNull
                   private String username;

                   @Size(max = 500)
                   private String freeText;

                   @Size(min = 2, max = 2)
                   @Pattern(regexp = "[A-Z]{2}")
                   private String countryCode;

                   public StringConstraintsType() {
                       // Default constructor
                   }

                   // All-args constructor
                   public StringConstraintsType(
                       String email,
                       String username,
                       String freeText,
                       String countryCode
                   ) {
                       this.email = email;
                       this.username = username;
                       this.freeText = freeText;
                       this.countryCode = countryCode;
                   }

                   /**
                    * Get email.
                    * E-mail address of the customer.
                    * @return String
                    */
                   public String getEmail() {
                       return email;
                   }

                   /**
                    * Set email.
                    * @param email E-mail address of the customer.
                    */
                   public void setEmail(String email) {
                       this.email = email;
                   }

                   /**
                    * Get username.
                    * Login name. Must start with a letter and then contain only letters, digits, underscores or hyphens.
                    * @return String
                    */
                   public String getUsername() {
                       return username;
                   }

                   /**
                    * Set username.
                    * @param username Login name. Must start with a letter and then contain only letters, digits, underscores or hyphens.
                    */
                   public void setUsername(String username) {
                       this.username = username;
                   }

                   /**
                    * Get freeText.
                    * Arbitrary text with only a maximum length restriction.
                    * @return String
                    */
                   public String getFreeText() {
                       return freeText;
                   }

                   /**
                    * Set freeText.
                    * @param freeText Arbitrary text with only a maximum length restriction.
                    */
                   public void setFreeText(String freeText) {
                       this.freeText = freeText;
                   }

                   /**
                    * Get countryCode.
                    * Two-letter ISO-3166 country code.
                    * @return String
                    */
                   public String getCountryCode() {
                       return countryCode;
                   }

                   /**
                    * Set countryCode.
                    * @param countryCode Two-letter ISO-3166 country code.
                    */
                   public void setCountryCode(String countryCode) {
                       this.countryCode = countryCode;
                   }

                   @Override
                   public boolean equals(Object o) {
                       if (this == o) return true;
                       if (o == null || getClass() != o.getClass()) return false;
                       StringConstraintsType that = (StringConstraintsType) o;
                       return
                           Objects.equals(email, that.email) &&

                           Objects.equals(username, that.username) &&

                           Objects.equals(freeText, that.freeText) &&

                           Objects.equals(countryCode, that.countryCode)
               ;
                   }

                   @Override
                   public int hashCode() {
                       return Objects.hash(
               
                           email,
                           username,
                           freeText,
                           countryCode
                       );
                   }

                   @Override
                   public String toString() {
                       StringBuilder sb = new StringBuilder();
                       sb.append("class StringConstraintsType {\n");
                       sb.append("    email: ").append(email).append("\n");
                       sb.append("    username: ").append(username).append("\n");
                       sb.append("    freeText: ").append(freeText).append("\n");
                       sb.append("    countryCode: ").append(countryCode).append("\n");
                       sb.append("}");
                       return sb.toString();
                   }
               }
           """.trimIndent()
        assertEquals(expected, classBody)
    }
}
