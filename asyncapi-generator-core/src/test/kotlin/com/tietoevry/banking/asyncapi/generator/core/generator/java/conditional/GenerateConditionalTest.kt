package com.tietoevry.banking.asyncapi.generator.core.generator.java.conditional

import com.tietoevry.banking.asyncapi.generator.core.generator.AbstractJavaGeneratorClass
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals

class GenerateConditionalTest : AbstractJavaGeneratorClass() {

    @Test
    fun generate_asyncapi_conditional_example_ConditionalExample_dataClass() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_conditional_example.yaml"),
            generated = "ConditionalExample.java",
            modelPackage = "com.tietoevry.banking.asyncapi.generator.core.model.generated.conditional",
        )
        val classBody = extractClassBody(generated)
        val expected = """
               public class ConditionalExample implements Serializable {

                   @NotNull
                   private Type type;

                   @NotNull
                   private Object value;

                   public ConditionalExample() {
                       // Default constructor
                   }

                   // All-args constructor
                   public ConditionalExample(
                       Type type,
                       Object value
                   ) {
                       this.type = type;
                       this.value = value;
                   }

                   /**
                    * Get type.
                    * @return Type
                    */
                   public Type getType() {
                       return type;
                   }

                   /**
                    * Set type.
                    * @param type
                    */
                   public void setType(Type type) {
                       this.type = type;
                   }

                   /**
                    * Get value.
                    * Will be validated differently depending on `type`.
                    * @return Object
                    */
                   public Object getValue() {
                       return value;
                   }

                   /**
                    * Set value.
                    * @param value Will be validated differently depending on `type`.
                    */
                   public void setValue(Object value) {
                       this.value = value;
                   }

                   @Override
                   public boolean equals(Object o) {
                       if (this == o) return true;
                       if (o == null || getClass() != o.getClass()) return false;
                       ConditionalExample that = (ConditionalExample) o;
                       return
                           Objects.equals(type, that.type) &&

                           Objects.equals(value, that.value)
               ;
                   }

                   @Override
                   public int hashCode() {
                       return Objects.hash(
               
                           type,
                           value
                       );
                   }

                   @Override
                   public String toString() {
                       StringBuilder sb = new StringBuilder();
                       sb.append("class ConditionalExample {\n");
                       sb.append("    type: ").append(type).append("\n");
                       sb.append("    value: ").append(value).append("\n");
                       sb.append("}");
                       return sb.toString();
                   }
               }
           """.trimIndent()
        assertEquals(expected, classBody)
    }


    @Test
    fun generate_asyncapi_conditional_example_Type_enumClass() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_conditional_example.yaml"),
            generated = "Type.java",
            modelPackage = "com.tietoevry.banking.asyncapi.generator.core.model.generated.conditional",
        )
        val classBody = extractClassBody(generated)
        val expected = """
              public enum Type implements Serializable {
                  NUMERIC,
                  TEXT,
              }
              """.trimIndent()
        assertEquals(expected, classBody)
    }
}
