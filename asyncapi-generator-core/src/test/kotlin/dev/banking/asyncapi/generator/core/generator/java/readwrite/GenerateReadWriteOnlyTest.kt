package dev.banking.asyncapi.generator.core.generator.java.readwrite

import dev.banking.asyncapi.generator.core.generator.AbstractJavaGeneratorClass
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals

class GenerateReadWriteOnlyTest : AbstractJavaGeneratorClass() {

    @Test
    fun generate_data_class_with_readOnly_writeOnly_fields() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_read_write_only.yaml"),
            generated = "AccessControlledObject.java",
            modelPackage = "dev.banking.asyncapi.generator.core.model.generated.access",
        )
        val classBody = extractClassBody(generated)
        val expected = """
              public class AccessControlledObject implements Serializable {

                  @JsonProperty(access = Access.READ_ONLY)
                  private String id;

                  @JsonProperty(access = Access.WRITE_ONLY)
                  private String password;

                  private String status;

                  @JsonProperty(access = Access.READ_WRITE)
                  private String secretKey;

                  public AccessControlledObject() {
                      // Default constructor
                  }

                  // All-args constructor
                  public AccessControlledObject(
                      String id,
                      String password,
                      String status,
                      String secretKey
                  ) {
                      this.id = id;
                      this.password = password;
                      this.status = status;
                      this.secretKey = secretKey;
                  }

                  /**
                   * Get id.
                   * A read-only identifier.
                   * @return String
                   */
                  public String getId() {
                      return id;
                  }

                  /**
                   * Set id.
                   * @param id A read-only identifier.
                   */
                  public void setId(String id) {
                      this.id = id;
                  }

                  /**
                   * Get password.
                   * A write-only password field.
                   * @return String
                   */
                  public String getPassword() {
                      return password;
                  }

                  /**
                   * Set password.
                   * @param password A write-only password field.
                   */
                  public void setPassword(String password) {
                      this.password = password;
                  }

                  /**
                   * Get status.
                   * A regular read-write field.
                   * @return String
                   */
                  public String getStatus() {
                      return status;
                  }

                  /**
                   * Set status.
                   * @param status A regular read-write field.
                   */
                  public void setStatus(String status) {
                      this.status = status;
                  }

                  /**
                   * Get secretKey.
                   * @return String
                   */
                  public String getSecretKey() {
                      return secretKey;
                  }

                  /**
                   * Set secretKey.
                   * @param secretKey
                   */
                  public void setSecretKey(String secretKey) {
                      this.secretKey = secretKey;
                  }

                  @Override
                  public boolean equals(Object o) {
                      if (this == o) return true;
                      if (o == null || getClass() != o.getClass()) return false;
                      AccessControlledObject that = (AccessControlledObject) o;
                      return
                          Objects.equals(id, that.id) &&

                          Objects.equals(password, that.password) &&

                          Objects.equals(status, that.status) &&

                          Objects.equals(secretKey, that.secretKey)
              ;
                  }

                  @Override
                  public int hashCode() {
                      return Objects.hash(
              
                          id,
                          password,
                          status,
                          secretKey
                      );
                  }

                  @Override
                  public String toString() {
                      StringBuilder sb = new StringBuilder();
                      sb.append("class AccessControlledObject {\n");
                      sb.append("    id: ").append(id).append("\n");
                      sb.append("    password: ").append(password).append("\n");
                      sb.append("    status: ").append(status).append("\n");
                      sb.append("    secretKey: ").append(secretKey).append("\n");
                      sb.append("}");
                      return sb.toString();
                  }
              }
              """.trimIndent()
        assertEquals(expected, classBody)
    }
}
