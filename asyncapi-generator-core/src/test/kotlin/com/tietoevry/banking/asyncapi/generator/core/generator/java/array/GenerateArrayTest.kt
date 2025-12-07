package com.tietoevry.banking.asyncapi.generator.core.generator.java.array

import com.tietoevry.banking.asyncapi.generator.core.generator.AbstractJavaGeneratorClass
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals

class GenerateArrayTest : AbstractJavaGeneratorClass() {

    @Test
    fun generate_asyncapi_array_primitive_object_ContactPointType_dataClass() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_array_primitive_object.yaml"),
            generated = "ContactPointType.java",
            modelPackage = "com.tietoevry.banking.asyncapi.generator.core.model.generated.array",
        )
        val classBody = extractClassBody(generated)

        val expected = """
            public class ContactPointType implements Serializable {
            
                @Size(min = 3, max = 20)
                @NotNull
                private String type;
            
                @Size(max = 200)
                @NotNull
                private String value;
            
                private Boolean preferred;
            
                public ContactPointType() {
                    // Default constructor
                }
            
                // All-args constructor
                public ContactPointType(
                    String type,
                    String value,
                    Boolean preferred
                ) {
                    this.type = type;
                    this.value = value;
                    this.preferred = preferred;
                }
            
                /**
                 * Get type.
                 * Type of the contact point. Supported values:
                 * * `email`
                 * * `mobile`
                 * * `landline`
                 * * `postal`
                 * @return String
                 */
                public String getType() {
                    return type;
                }
            
                /**
                 * Set type.
                 * @param type Type of the contact point. Supported values:
                 */
                public void setType(String type) {
                    this.type = type;
                }
            
                /**
                 * Get value.
                 * The actual contact detail – phone number, e-mail address, etc.
                 * @return String
                 */
                public String getValue() {
                    return value;
                }
            
                /**
                 * Set value.
                 * @param value The actual contact detail – phone number, e-mail address, etc.
                 */
                public void setValue(String value) {
                    this.value = value;
                }
            
                /**
                 * Get preferred.
                 * Indicates if this contact point is preferred.
                 * @return Boolean
                 */
                public Boolean getPreferred() {
                    return preferred;
                }
            
                /**
                 * Set preferred.
                 * @param preferred Indicates if this contact point is preferred.
                 */
                public void setPreferred(Boolean preferred) {
                    this.preferred = preferred;
                }
            
                @Override
                public boolean equals(Object o) {
                    if (this == o) return true;
                    if (o == null || getClass() != o.getClass()) return false;
                    ContactPointType that = (ContactPointType) o;
                    return
                        Objects.equals(type, that.type) &&
            
                        Objects.equals(value, that.value) &&
            
                        Objects.equals(preferred, that.preferred)
            ;
                }
            
                @Override
                public int hashCode() {
                    return Objects.hash(
            
                        type,
                        value,
                        preferred
                    );
                }
            
                @Override
                public String toString() {
                    StringBuilder sb = new StringBuilder();
                    sb.append("class ContactPointType {\n");
                    sb.append("    type: ").append(type).append("\n");
                    sb.append("    value: ").append(value).append("\n");
                    sb.append("    preferred: ").append(preferred).append("\n");
                    sb.append("}");
                    return sb.toString();
                }
            }
           """.trimIndent()
        assertEquals(expected.replace("\r\n", "\n"), classBody.replace("\r\n", "\n"))
    }

    @Test
    fun generate_asyncapi_array_primitive_object_CustomerWithContacts_dataClass() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_array_primitive_object.yaml"),
            generated = "CustomerWithContacts.java",
            modelPackage = "com.tietoevry.banking.asyncapi.generator.core.model.generated.array",
        )
        val classBody = extractClassBody(generated)

        val expected = """
            public class CustomerWithContacts implements Serializable {
            
                @NotNull
                private UUID customerId;
            
                @Size(max = 140)
                @NotNull
                private String fullName;
            
                private List<String> tags;
            
                @NotNull
                @Valid
                private List<ContactPointType> contactPoints;
            
                public CustomerWithContacts() {
                    // Default constructor
                }
            
                // All-args constructor
                public CustomerWithContacts(
                    UUID customerId,
                    String fullName,
                    List<String> tags,
                    List<ContactPointType> contactPoints
                ) {
                    this.customerId = customerId;
                    this.fullName = fullName;
                    this.tags = tags;
                    this.contactPoints = contactPoints;
                }
            
                /**
                 * Get customerId.
                 * Unique identifier for the customer.
                 * @return UUID
                 */
                public UUID getCustomerId() {
                    return customerId;
                }
            
                /**
                 * Set customerId.
                 * @param customerId Unique identifier for the customer.
                 */
                public void setCustomerId(UUID customerId) {
                    this.customerId = customerId;
                }
            
                /**
                 * Get fullName.
                 * Customer's full name as presented in UI and reports.
                 * @return String
                 */
                public String getFullName() {
                    return fullName;
                }
            
                /**
                 * Set fullName.
                 * @param fullName Customer's full name as presented in UI and reports.
                 */
                public void setFullName(String fullName) {
                    this.fullName = fullName;
                }
            
                /**
                 * Get tags.
                 * Free-form tags associated with the customer.
                 * @return List<String>
                 */
                public List<String> getTags() {
                    return tags;
                }
            
                /**
                 * Set tags.
                 * @param tags Free-form tags associated with the customer.
                 */
                public void setTags(List<String> tags) {
                    this.tags = tags;
                }
            
                /**
                 * Get contactPoints.
                 * List of contact points associated with the customer.
                 * @return List<ContactPointType>
                 */
                public List<ContactPointType> getContactPoints() {
                    return contactPoints;
                }
            
                /**
                 * Set contactPoints.
                 * @param contactPoints List of contact points associated with the customer.
                 */
                public void setContactPoints(List<ContactPointType> contactPoints) {
                    this.contactPoints = contactPoints;
                }
            
                @Override
                public boolean equals(Object o) {
                    if (this == o) return true;
                    if (o == null || getClass() != o.getClass()) return false;
                    CustomerWithContacts that = (CustomerWithContacts) o;
                    return
                        Objects.equals(customerId, that.customerId) &&
            
                        Objects.equals(fullName, that.fullName) &&
            
                        Objects.equals(tags, that.tags) &&
            
                        Objects.equals(contactPoints, that.contactPoints)
            ;
                }
            
                @Override
                public int hashCode() {
                    return Objects.hash(
            
                        customerId,
                        fullName,
                        tags,
                        contactPoints
                    );
                }
            
                @Override
                public String toString() {
                    StringBuilder sb = new StringBuilder();
                    sb.append("class CustomerWithContacts {\n");
                    sb.append("    customerId: ").append(customerId).append("\n");
                    sb.append("    fullName: ").append(fullName).append("\n");
                    sb.append("    tags: ").append(tags).append("\n");
                    sb.append("    contactPoints: ").append(contactPoints).append("\n");
                    sb.append("}");
                    return sb.toString();
                }
            }
           """.trimIndent()
        assertEquals(expected, classBody)
    }

    @Test
    fun generate_enum_from_array_items() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_array_inline_enum.yaml"),
            generated = "Priorities.java",
            modelPackage = "com.tietoevry.banking.asyncapi.generator.core.model.generated.arrayenum",
        )
        val classBody = extractClassBody(generated)

        val expected = """
               public enum Priorities implements Serializable {
                   LOW,
                   MEDIUM,
                   HIGH,
               }
           """.trimIndent()

        assertEquals(expected, classBody)
    }

    @Test
    fun generate_data_class_with_list_of_enums() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_array_inline_enum.yaml"),
            generated = "ObjectWithArrayOfEnums.java",
            modelPackage = "com.tietoevry.banking.asyncapi.generator.core.model.generated.arrayenum",
        )
        val classBody = extractClassBody(generated)
        val expected = """
               public class ObjectWithArrayOfEnums implements Serializable {
               
                   private List<Priorities> priorities;
               
                   public ObjectWithArrayOfEnums() {
                       // Default constructor
                   }
               
                   // All-args constructor
                   public ObjectWithArrayOfEnums(
                       List<Priorities> priorities
                   ) {
                       this.priorities = priorities;
                   }
               
                   /**
                    * Get priorities.
                    * @return List<Priorities>
                    */
                   public List<Priorities> getPriorities() {
                       return priorities;
                   }
               
                   /**
                    * Set priorities.
                    * @param priorities
                    */
                   public void setPriorities(List<Priorities> priorities) {
                       this.priorities = priorities;
                   }
               
                   @Override
                   public boolean equals(Object o) {
                       if (this == o) return true;
                       if (o == null || getClass() != o.getClass()) return false;
                       ObjectWithArrayOfEnums that = (ObjectWithArrayOfEnums) o;
                       return
                           Objects.equals(priorities, that.priorities)
               ;
                   }
               
                   @Override
                   public int hashCode() {
                       return Objects.hash(
               
                           priorities
                       );
                   }
               
                   @Override
                   public String toString() {
                       StringBuilder sb = new StringBuilder();
                       sb.append("class ObjectWithArrayOfEnums {\n");
                       sb.append("    priorities: ").append(priorities).append("\n");
                       sb.append("}");
                       return sb.toString();
                   }
               }
           """.trimIndent()
        assertEquals(expected, classBody)
    }
}
