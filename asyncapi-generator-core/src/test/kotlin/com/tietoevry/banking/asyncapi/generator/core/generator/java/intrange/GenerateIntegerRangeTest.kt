package com.tietoevry.banking.asyncapi.generator.core.generator.java.intrange

import com.tietoevry.banking.asyncapi.generator.core.generator.AbstractJavaGeneratorClass
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals

class GenerateIntegerRangeTest : AbstractJavaGeneratorClass() {

    @Test
    fun generate_asyncapi_integer_range_type_IntegerRangesType_dataClass() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_integer_range_type.yaml"),
            generated = "IntegerRangesType.java",
            modelPackage = "com.tietoevry.banking.asyncapi.generator.core.model.generated.intrange",
        )
        val classBody = extractClassBody(generated)
        val expected = """
              public class IntegerRangesType implements Serializable {

                  @Min(0L)
                  @Max(100000L)
                  @NotNull
                  private Integer smallCounter;

                  @Min(0L)
                  @Max(9007199254740991L)
                  @NotNull
                  private Long largeCounter;

                  @Min(-1000L)
                  @Max(1000L)
                  private Integer boundedWithoutFormat;

                  private Integer unboundedInteger;

                  public IntegerRangesType() {
                      // Default constructor
                  }

                  // All-args constructor
                  public IntegerRangesType(
                      Integer smallCounter,
                      Long largeCounter,
                      Integer boundedWithoutFormat,
                      Integer unboundedInteger
                  ) {
                      this.smallCounter = smallCounter;
                      this.largeCounter = largeCounter;
                      this.boundedWithoutFormat = boundedWithoutFormat;
                      this.unboundedInteger = unboundedInteger;
                  }

                  /**
                   * Get smallCounter.
                   * A small counter that always fits within 32-bit signed range.
                   * @return Integer
                   */
                  public Integer getSmallCounter() {
                      return smallCounter;
                  }

                  /**
                   * Set smallCounter.
                   * @param smallCounter A small counter that always fits within 32-bit signed range.
                   */
                  public void setSmallCounter(Integer smallCounter) {
                      this.smallCounter = smallCounter;
                  }

                  /**
                   * Get largeCounter.
                   * A large counter that may exceed 32-bit integer range.
                   * @return Long
                   */
                  public Long getLargeCounter() {
                      return largeCounter;
                  }

                  /**
                   * Set largeCounter.
                   * @param largeCounter A large counter that may exceed 32-bit integer range.
                   */
                  public void setLargeCounter(Long largeCounter) {
                      this.largeCounter = largeCounter;
                  }

                  /**
                   * Get boundedWithoutFormat.
                   * Integer without explicit format, but bounds suggest Int is safe.
                   * @return Integer
                   */
                  public Integer getBoundedWithoutFormat() {
                      return boundedWithoutFormat;
                  }

                  /**
                   * Set boundedWithoutFormat.
                   * @param boundedWithoutFormat Integer without explicit format, but bounds suggest Int is safe.
                   */
                  public void setBoundedWithoutFormat(Integer boundedWithoutFormat) {
                      this.boundedWithoutFormat = boundedWithoutFormat;
                  }

                  /**
                   * Get unboundedInteger.
                   * Integer without explicit format or bounds. Should default to Long as a safe choice.
                   * @return Integer
                   */
                  public Integer getUnboundedInteger() {
                      return unboundedInteger;
                  }

                  /**
                   * Set unboundedInteger.
                   * @param unboundedInteger Integer without explicit format or bounds. Should default to Long as a safe choice.
                   */
                  public void setUnboundedInteger(Integer unboundedInteger) {
                      this.unboundedInteger = unboundedInteger;
                  }

                  @Override
                  public boolean equals(Object o) {
                      if (this == o) return true;
                      if (o == null || getClass() != o.getClass()) return false;
                      IntegerRangesType that = (IntegerRangesType) o;
                      return
                          Objects.equals(smallCounter, that.smallCounter) &&

                          Objects.equals(largeCounter, that.largeCounter) &&

                          Objects.equals(boundedWithoutFormat, that.boundedWithoutFormat) &&

                          Objects.equals(unboundedInteger, that.unboundedInteger)
              ;
                  }

                  @Override
                  public int hashCode() {
                      return Objects.hash(
              
                          smallCounter,
                          largeCounter,
                          boundedWithoutFormat,
                          unboundedInteger
                      );
                  }

                  @Override
                  public String toString() {
                      StringBuilder sb = new StringBuilder();
                      sb.append("class IntegerRangesType {\n");
                      sb.append("    smallCounter: ").append(smallCounter).append("\n");
                      sb.append("    largeCounter: ").append(largeCounter).append("\n");
                      sb.append("    boundedWithoutFormat: ").append(boundedWithoutFormat).append("\n");
                      sb.append("    unboundedInteger: ").append(unboundedInteger).append("\n");
                      sb.append("}");
                      return sb.toString();
                  }
              }
              """.trimIndent()
        assertEquals(expected, classBody)
    }
}
