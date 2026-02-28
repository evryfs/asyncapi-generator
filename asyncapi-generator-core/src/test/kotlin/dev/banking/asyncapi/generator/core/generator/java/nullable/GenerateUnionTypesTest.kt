package dev.banking.asyncapi.generator.core.generator.java.nullable

import dev.banking.asyncapi.generator.core.generator.AbstractJavaGeneratorClass
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals

class GenerateUnionTypesTest : AbstractJavaGeneratorClass() {

    @Test
    fun generate_union_types_with_strict_json_schema_semantics() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_union_types.yaml"),
            generated = "UnionTypes.java",
            modelPackage = "dev.banking.asyncapi.generator.core.model.generated.union",
        )
        val classBody = extractClassBody(generated)
        val expected = """
            public class UnionTypes implements Serializable {
            
                @NotNull
                private Object stringOrArray;

                private List<String> arrayOrNull;

                private Object stringArrayOrNull;

                private String stringOrNull;

                public UnionTypes() {
                    // Default constructor
                }

                // All-args constructor
                public UnionTypes(
                    Object stringOrArray,
                    List<String> arrayOrNull,
                    Object stringArrayOrNull,
                    String stringOrNull
                ) {
                    this.stringOrArray = stringOrArray;
                    this.arrayOrNull = arrayOrNull;
                    this.stringArrayOrNull = stringArrayOrNull;
                    this.stringOrNull = stringOrNull;
                }

                /**
                 * Get stringOrArray.
                 * @return Object
                 */
                public Object getStringOrArray() {
                    return stringOrArray;
                }

                /**
                 * Set stringOrArray.
                 * @param stringOrArray
                 */
                public void setStringOrArray(Object stringOrArray) {
                    this.stringOrArray = stringOrArray;
                }

                /**
                 * Get arrayOrNull.
                 * @return List<String>
                 */
                public List<String> getArrayOrNull() {
                    return arrayOrNull;
                }

                /**
                 * Set arrayOrNull.
                 * @param arrayOrNull
                 */
                public void setArrayOrNull(List<String> arrayOrNull) {
                    this.arrayOrNull = arrayOrNull;
                }

                /**
                 * Get stringArrayOrNull.
                 * @return Object
                 */
                public Object getStringArrayOrNull() {
                    return stringArrayOrNull;
                }

                /**
                 * Set stringArrayOrNull.
                 * @param stringArrayOrNull
                 */
                public void setStringArrayOrNull(Object stringArrayOrNull) {
                    this.stringArrayOrNull = stringArrayOrNull;
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

                @Override
                public boolean equals(Object o) {
                    if (this == o) return true;
                    if (o == null || getClass() != o.getClass()) return false;
                    UnionTypes that = (UnionTypes) o;
                    return
                        Objects.equals(stringOrArray, that.stringOrArray) &&

                        Objects.equals(arrayOrNull, that.arrayOrNull) &&

                        Objects.equals(stringArrayOrNull, that.stringArrayOrNull) &&

                        Objects.equals(stringOrNull, that.stringOrNull)
            ;
                }

                @Override
                public int hashCode() {
                    return Objects.hash(

                        stringOrArray,
                        arrayOrNull,
                        stringArrayOrNull,
                        stringOrNull
                    );
                }

                @Override
                public String toString() {
                    StringBuilder sb = new StringBuilder();
                    sb.append("class UnionTypes {\n");
                    sb.append("    stringOrArray: ").append(stringOrArray).append("\n");
                    sb.append("    arrayOrNull: ").append(arrayOrNull).append("\n");
                    sb.append("    stringArrayOrNull: ").append(stringArrayOrNull).append("\n");
                    sb.append("    stringOrNull: ").append(stringOrNull).append("\n");
                    sb.append("}");
                    return sb.toString();
                }
            }
            """.trimIndent()
        assertEquals(expected, classBody)
    }
}
