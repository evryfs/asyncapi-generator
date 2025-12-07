package com.tietoevry.banking.asyncapi.generator.core.generator.java.settingsobject

import com.tietoevry.banking.asyncapi.generator.core.generator.AbstractJavaGeneratorClass
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals

class GenerateSettingsObjectTest : AbstractJavaGeneratorClass() {

    @Test
    fun generate_asyncapi_settings_object_type_SettingsObjectType_dataClass() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_settings_object_type.yaml"),
            generated = "SettingsObjectType.java",
            modelPackage = "com.tietoevry.banking.asyncapi.generator.core.model.generated.settingsobject",
        )
        val classBody = extractClassBody(generated)
        val expected = """
              public class SettingsObjectType implements Serializable {

                  @NotNull
                  private Mode mode;

                  @Min(0L)
                  @Max(10L)
                  private Integer retryLimit;

                  private Map<String, Object> extraSettings;

                  public SettingsObjectType() {
                      // Default constructor
                  }

                  // All-args constructor
                  public SettingsObjectType(
                      Mode mode,
                      Integer retryLimit,
                      Map<String, Object> extraSettings
                  ) {
                      this.mode = mode;
                      this.retryLimit = retryLimit;
                      this.extraSettings = extraSettings;
                  }

                  /**
                   * Get mode.
                   * Operational mode for the process.
                   * @return Mode
                   */
                  public Mode getMode() {
                      return mode;
                  }

                  /**
                   * Set mode.
                   * @param mode Operational mode for the process.
                   */
                  public void setMode(Mode mode) {
                      this.mode = mode;
                  }

                  /**
                   * Get retryLimit.
                   * Maximum number of retries allowed.
                   * @return Integer
                   */
                  public Integer getRetryLimit() {
                      return retryLimit;
                  }

                  /**
                   * Set retryLimit.
                   * @param retryLimit Maximum number of retries allowed.
                   */
                  public void setRetryLimit(Integer retryLimit) {
                      this.retryLimit = retryLimit;
                  }

                  /**
                   * Get extraSettings.
                   * Arbitrary key-value pairs for advanced configuration. Keys are strings; values can be of any type.
                   * @return Map<String, Object>
                   */
                  public Map<String, Object> getExtraSettings() {
                      return extraSettings;
                  }

                  /**
                   * Set extraSettings.
                   * @param extraSettings Arbitrary key-value pairs for advanced configuration. Keys are strings; values can be of any type.
                   */
                  public void setExtraSettings(Map<String, Object> extraSettings) {
                      this.extraSettings = extraSettings;
                  }

                  @Override
                  public boolean equals(Object o) {
                      if (this == o) return true;
                      if (o == null || getClass() != o.getClass()) return false;
                      SettingsObjectType that = (SettingsObjectType) o;
                      return
                          Objects.equals(mode, that.mode) &&

                          Objects.equals(retryLimit, that.retryLimit) &&

                          Objects.equals(extraSettings, that.extraSettings)
              ;
                  }

                  @Override
                  public int hashCode() {
                      return Objects.hash(
              
                          mode,
                          retryLimit,
                          extraSettings
                      );
                  }

                  @Override
                  public String toString() {
                      StringBuilder sb = new StringBuilder();
                      sb.append("class SettingsObjectType {\n");
                      sb.append("    mode: ").append(mode).append("\n");
                      sb.append("    retryLimit: ").append(retryLimit).append("\n");
                      sb.append("    extraSettings: ").append(extraSettings).append("\n");
                      sb.append("}");
                      return sb.toString();
                  }
              }
              """.trimIndent()

        assertEquals(expected, classBody)
    }

    @Test
    fun generate_asyncapi_settings_object_type_Mode_enumClass() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_settings_object_type.yaml"),
            generated = "Mode.java",
            modelPackage = "com.tietoevry.banking.asyncapi.generator.core.model.generated.settingsobject",
        )
        val classBody = extractClassBody(generated)
        val expected = """
              public enum Mode implements Serializable {
                  FAST,
                  SAFE,
                  DEBUG,
              }
              """.trimIndent()
        assertEquals(expected, classBody)
    }
}
