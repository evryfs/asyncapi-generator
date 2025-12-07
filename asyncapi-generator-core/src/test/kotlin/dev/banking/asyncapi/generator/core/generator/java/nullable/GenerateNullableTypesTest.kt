package dev.banking.asyncapi.generator.core.generator.java.nullable

import dev.banking.asyncapi.generator.core.generator.AbstractJavaGeneratorClass
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals

class GenerateNullableTypesTest : AbstractJavaGeneratorClass() {

    @Test
    fun generate_nullable_object_with_various_nullable_fields() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_nullable_types.yaml"),
            generated = "NullableObject.java",
            modelPackage = "dev.banking.asyncapi.generator.core.model.generated.nullable",
        )
        val classBody = extractClassBody(generated)
        val expected = """
              public class NullableObject implements Serializable {

                  @NotNull
                  private String requiredString;

                  private String optionalString;

                  private List<String> nullableStringArray;

                  private String stringOrNull;

                  private Integer integerOrNull;

                  private Boolean booleanOrNull;

                  public NullableObject() {
                      // Default constructor
                  }

                  // All-args constructor
                  public NullableObject(
                      String requiredString,
                      String optionalString,
                      List<String> nullableStringArray,
                      String stringOrNull,
                      Integer integerOrNull,
                      Boolean booleanOrNull
                  ) {
                      this.requiredString = requiredString;
                      this.optionalString = optionalString;
                      this.nullableStringArray = nullableStringArray;
                      this.stringOrNull = stringOrNull;
                      this.integerOrNull = integerOrNull;
                      this.booleanOrNull = booleanOrNull;
                  }

                  /**
                   * Get requiredString.
                   * @return String
                   */
                  public String getRequiredString() {
                      return requiredString;
                  }

                  /**
                   * Set requiredString.
                   * @param requiredString
                   */
                  public void setRequiredString(String requiredString) {
                      this.requiredString = requiredString;
                  }

                  /**
                   * Get optionalString.
                   * @return String
                   */
                  public String getOptionalString() {
                      return optionalString;
                  }

                  /**
                   * Set optionalString.
                   * @param optionalString
                   */
                  public void setOptionalString(String optionalString) {
                      this.optionalString = optionalString;
                  }

                  /**
                   * Get nullableStringArray.
                   * @return List<String>
                   */
                  public List<String> getNullableStringArray() {
                      return nullableStringArray;
                  }

                  /**
                   * Set nullableStringArray.
                   * @param nullableStringArray
                   */
                  public void setNullableStringArray(List<String> nullableStringArray) {
                      this.nullableStringArray = nullableStringArray;
                  }

                  /**
                   * Get stringOrNull.
                   * @return String
                   */
                  public String getStringOrNull() {
                      return stringOrNull;
                  }

                  /**
                   * Set stringOrNull.
                   * @param stringOrNull
                   */
                  public void setStringOrNull(String stringOrNull) {
                      this.stringOrNull = stringOrNull;
                  }

                  /**
                   * Get integerOrNull.
                   * @return Integer
                   */
                  public Integer getIntegerOrNull() {
                      return integerOrNull;
                  }

                  /**
                   * Set integerOrNull.
                   * @param integerOrNull
                   */
                  public void setIntegerOrNull(Integer integerOrNull) {
                      this.integerOrNull = integerOrNull;
                  }

                  /**
                   * Get booleanOrNull.
                   * @return Boolean
                   */
                  public Boolean getBooleanOrNull() {
                      return booleanOrNull;
                  }

                  /**
                   * Set booleanOrNull.
                   * @param booleanOrNull
                   */
                  public void setBooleanOrNull(Boolean booleanOrNull) {
                      this.booleanOrNull = booleanOrNull;
                  }

                  @Override
                  public boolean equals(Object o) {
                      if (this == o) return true;
                      if (o == null || getClass() != o.getClass()) return false;
                      NullableObject that = (NullableObject) o;
                      return
                          Objects.equals(requiredString, that.requiredString) &&

                          Objects.equals(optionalString, that.optionalString) &&

                          Objects.equals(nullableStringArray, that.nullableStringArray) &&

                          Objects.equals(stringOrNull, that.stringOrNull) &&

                          Objects.equals(integerOrNull, that.integerOrNull) &&

                          Objects.equals(booleanOrNull, that.booleanOrNull)
              ;
                  }

                  @Override
                  public int hashCode() {
                      return Objects.hash(
              
                          requiredString,
                          optionalString,
                          nullableStringArray,
                          stringOrNull,
                          integerOrNull,
                          booleanOrNull
                      );
                  }

                  @Override
                  public String toString() {
                      StringBuilder sb = new StringBuilder();
                      sb.append("class NullableObject {\n");
                      sb.append("    requiredString: ").append(requiredString).append("\n");
                      sb.append("    optionalString: ").append(optionalString).append("\n");
                      sb.append("    nullableStringArray: ").append(nullableStringArray).append("\n");
                      sb.append("    stringOrNull: ").append(stringOrNull).append("\n");
                      sb.append("    integerOrNull: ").append(integerOrNull).append("\n");
                      sb.append("    booleanOrNull: ").append(booleanOrNull).append("\n");
                      sb.append("}");
                      return sb.toString();
                  }
              }
              """.trimIndent()
        assertEquals(expected, classBody)
    }
}
