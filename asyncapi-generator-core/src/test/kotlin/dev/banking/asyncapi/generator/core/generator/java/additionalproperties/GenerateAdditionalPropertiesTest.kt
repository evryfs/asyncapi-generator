package dev.banking.asyncapi.generator.core.generator.java.additionalproperties

import dev.banking.asyncapi.generator.core.generator.AbstractJavaGeneratorClass
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals

class GenerateAdditionalPropertiesTest : AbstractJavaGeneratorClass() {

    @Test
    fun generate_data_class_imports() {
        val generatedContent = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_additionalproperties_map_objects.yaml"),
            generated = "ContainerObject.java",
            modelPackage = "dev.banking.asyncapi.generator.core.model.generated.additionalproperties",
        )

        val imports = extractImports(generatedContent)

        val expectedImports = """
               import dev.banking.asyncapi.generator.core.model.generated.additionalproperties.SomeItem;
               import jakarta.validation.Valid;
               import java.io.Serializable;
               import java.util.Map;
               import java.util.Objects;
           """.trimIndent()

        assertEquals(expectedImports, imports)
    }

    @Test
    fun generate_data_class_body() {
        val generatedContent = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_additionalproperties_map_objects.yaml"),
            generated = "ContainerObject.java",
            modelPackage = "dev.banking.asyncapi.generator.core.model.generated.additionalproperties",
        )

        val classBody = extractClassBody(generatedContent)

        val expectedBody = """
           public class ContainerObject implements Serializable {

               private Map<String, String> stringTags;

               private Map<String, Object> anyTags;

               @Valid
               private Map<String, SomeItem> itemMap;

               public ContainerObject() {
                   // Default constructor
               }

               // All-args constructor
               public ContainerObject(
                   Map<String, String> stringTags,
                   Map<String, Object> anyTags,
                   Map<String, SomeItem> itemMap
               ) {
                   this.stringTags = stringTags;
                   this.anyTags = anyTags;
                   this.itemMap = itemMap;
               }

               /**
                * Get stringTags.
                * A map of string-to-string tags.
                * @return Map<String, String>
                */
               public Map<String, String> getStringTags() {
                   return stringTags;
               }

               /**
                * Set stringTags.
                * @param stringTags A map of string-to-string tags.
                */
               public void setStringTags(Map<String, String> stringTags) {
                   this.stringTags = stringTags;
               }

               /**
                * Get anyTags.
                * A map with values of any type.
                * @return Map<String, Object>
                */
               public Map<String, Object> getAnyTags() {
                   return anyTags;
               }

               /**
                * Set anyTags.
                * @param anyTags A map with values of any type.
                */
               public void setAnyTags(Map<String, Object> anyTags) {
                   this.anyTags = anyTags;
               }

               /**
                * Get itemMap.
                * A map of string-to-SomeItem objects.
                * @return Map<String, SomeItem>
                */
               public Map<String, SomeItem> getItemMap() {
                   return itemMap;
               }

               /**
                * Set itemMap.
                * @param itemMap A map of string-to-SomeItem objects.
                */
               public void setItemMap(Map<String, SomeItem> itemMap) {
                   this.itemMap = itemMap;
               }

               @Override
               public boolean equals(Object o) {
                   if (this == o) return true;
                   if (o == null || getClass() != o.getClass()) return false;
                   ContainerObject that = (ContainerObject) o;
                   return
                       Objects.equals(stringTags, that.stringTags) &&

                       Objects.equals(anyTags, that.anyTags) &&

                       Objects.equals(itemMap, that.itemMap)
           ;
               }

               @Override
               public int hashCode() {
                   return Objects.hash(
           
                       stringTags,
                       anyTags,
                       itemMap
                   );
               }

               @Override
               public String toString() {
                   StringBuilder sb = new StringBuilder();
                   sb.append("class ContainerObject {\n");
                   sb.append("    stringTags: ").append(stringTags).append("\n");
                   sb.append("    anyTags: ").append(anyTags).append("\n");
                   sb.append("    itemMap: ").append(itemMap).append("\n");
                   sb.append("}");
                   return sb.toString();
               }
           }
           """.trimIndent()

        assertEquals(expectedBody, classBody)
    }
}
