package com.tietoevry.banking.asyncapi.generator.core.generator.java.enumvalue

import com.tietoevry.banking.asyncapi.generator.core.generator.AbstractJavaGeneratorClass
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals

class GenerateEnumValueTest : AbstractJavaGeneratorClass() {

    @Test
    fun generate_data_class_with_enum_default_values() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_enum_default_value.yaml"),
            generated = "Task.java",
            modelPackage = "com.tietoevry.banking.asyncapi.generator.core.model.generated.enumdefault",
        )
        val classBody = extractClassBody(generated)
        val expected = """
              public class Task implements Serializable {

                  private String id;

                  private TaskStatus status;

                  private Priority priority;

                  public Task() {
                      // Default constructor
                  }

                  // All-args constructor
                  public Task(
                      String id,
                      TaskStatus status,
                      Priority priority
                  ) {
                      this.id = id;
                      this.status = status;
                      this.priority = priority;
                  }

                  /**
                   * Get id.
                   * @return String
                   */
                  public String getId() {
                      return id;
                  }

                  /**
                   * Set id.
                   * @param id
                   */
                  public void setId(String id) {
                      this.id = id;
                  }

                  /**
                   * Get status.
                   * @return TaskStatus
                   */
                  public TaskStatus getStatus() {
                      return status;
                  }

                  /**
                   * Set status.
                   * @param status
                   */
                  public void setStatus(TaskStatus status) {
                      this.status = status;
                  }

                  /**
                   * Get priority.
                   * @return Priority
                   */
                  public Priority getPriority() {
                      return priority;
                  }

                  /**
                   * Set priority.
                   * @param priority
                   */
                  public void setPriority(Priority priority) {
                      this.priority = priority;
                  }

                  @Override
                  public boolean equals(Object o) {
                      if (this == o) return true;
                      if (o == null || getClass() != o.getClass()) return false;
                      Task that = (Task) o;
                      return
                          Objects.equals(id, that.id) &&

                          Objects.equals(status, that.status) &&

                          Objects.equals(priority, that.priority)
              ;
                  }

                  @Override
                  public int hashCode() {
                      return Objects.hash(
              
                          id,
                          status,
                          priority
                      );
                  }

                  @Override
                  public String toString() {
                      StringBuilder sb = new StringBuilder();
                      sb.append("class Task {\n");
                      sb.append("    id: ").append(id).append("\n");
                      sb.append("    status: ").append(status).append("\n");
                      sb.append("    priority: ").append(priority).append("\n");
                      sb.append("}");
                      return sb.toString();
                  }
              }
              """.trimIndent()
        assertEquals(expected, classBody)
    }


    @Test
    fun generate_TaskStatus_enum() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_enum_default_value.yaml"),
            generated = "TaskStatus.java",
            modelPackage = "com.tietoevry.banking.asyncapi.generator.core.model.generated.enumdefault",
        )
        val classBody = extractClassBody(generated)
        val expected = """
              public enum TaskStatus implements Serializable {
                  OPEN,
                  IN_PROGRESS,
                  CLOSED,
              }
              """.trimIndent()
        assertEquals(expected, classBody)
    }

    @Test
    fun generate_Priority_enum() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_enum_default_value.yaml"),
            generated = "Priority.java",
            modelPackage = "com.tietoevry.banking.asyncapi.generator.core.model.generated.enumdefault",
        )
        val classBody = extractClassBody(generated)
        val expected = """
              public enum Priority implements Serializable {
                  LOW,
                  MEDIUM,
                  HIGH,
              }
              """.trimIndent()
        assertEquals(expected, classBody)
    }
}
