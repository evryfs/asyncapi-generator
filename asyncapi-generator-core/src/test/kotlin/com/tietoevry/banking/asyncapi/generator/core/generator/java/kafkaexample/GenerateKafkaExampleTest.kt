package com.tietoevry.banking.asyncapi.generator.core.generator.java.kafkaexample

import com.tietoevry.banking.asyncapi.generator.core.generator.AbstractJavaGeneratorClass
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals

class GenerateKafkaExampleTest : AbstractJavaGeneratorClass() {

    @Test
    fun generate_AMPKafkaExampleEvent_v1_yaml_CustomerReadPayload_dataClass() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_spring_kafka_example.yaml"),
            generated = "CustomerReadPayload.java",
            modelPackage = "com.tietoevry.banking.asyncapi.generator.core.model.generated.kafkaexample",
        )
        val classBody = extractClassBody(generated)
        val expectedDataClass = """
               public class CustomerReadPayload implements Serializable {

                   @NotNull
                   private String customerId;

                   @Min(0L)
                   @Max(9999L)
                   private Integer orgId;

                   private String customerName;

                   private OffsetDateTime updatedDate;

                   private CustomerType customerType;

                   private String phoneNumber;

                   @Email
                   private String email;

                   public CustomerReadPayload() {
                       // Default constructor
                   }

                   // All-args constructor
                   public CustomerReadPayload(
                       String customerId,
                       Integer orgId,
                       String customerName,
                       OffsetDateTime updatedDate,
                       CustomerType customerType,
                       String phoneNumber,
                       String email
                   ) {
                       this.customerId = customerId;
                       this.orgId = orgId;
                       this.customerName = customerName;
                       this.updatedDate = updatedDate;
                       this.customerType = customerType;
                       this.phoneNumber = phoneNumber;
                       this.email = email;
                   }

                   /**
                    * Get customerId.
                    * The ID of the customer.
                    * @return String
                    */
                   public String getCustomerId() {
                       return customerId;
                   }

                   /**
                    * Set customerId.
                    * @param customerId The ID of the customer.
                    */
                   public void setCustomerId(String customerId) {
                       this.customerId = customerId;
                   }

                   /**
                    * Get orgId.
                    * The organization ID of the customer.
                    * @return Integer
                    */
                   public Integer getOrgId() {
                       return orgId;
                   }

                   /**
                    * Set orgId.
                    * @param orgId The organization ID of the customer.
                    */
                   public void setOrgId(Integer orgId) {
                       this.orgId = orgId;
                   }

                   /**
                    * Get customerName.
                    * The name of the customer.
                    * @return String
                    */
                   public String getCustomerName() {
                       return customerName;
                   }

                   /**
                    * Set customerName.
                    * @param customerName The name of the customer.
                    */
                   public void setCustomerName(String customerName) {
                       this.customerName = customerName;
                   }

                   /**
                    * Get updatedDate.
                    * The date when the customer data was last updated.
                    * @return OffsetDateTime
                    */
                   public OffsetDateTime getUpdatedDate() {
                       return updatedDate;
                   }

                   /**
                    * Set updatedDate.
                    * @param updatedDate The date when the customer data was last updated.
                    */
                   public void setUpdatedDate(OffsetDateTime updatedDate) {
                       this.updatedDate = updatedDate;
                   }

                   /**
                    * Get customerType.
                    * The type of the customer.
                    * @return CustomerType
                    */
                   public CustomerType getCustomerType() {
                       return customerType;
                   }

                   /**
                    * Set customerType.
                    * @param customerType The type of the customer.
                    */
                   public void setCustomerType(CustomerType customerType) {
                       this.customerType = customerType;
                   }

                   /**
                    * Get phoneNumber.
                    * The phone number of the customer.
                    * @return String
                    */
                   public String getPhoneNumber() {
                       return phoneNumber;
                   }

                   /**
                    * Set phoneNumber.
                    * @param phoneNumber The phone number of the customer.
                    */
                   public void setPhoneNumber(String phoneNumber) {
                       this.phoneNumber = phoneNumber;
                   }

                   /**
                    * Get email.
                    * The email address of the customer.
                    * @return String
                    */
                   public String getEmail() {
                       return email;
                   }

                   /**
                    * Set email.
                    * @param email The email address of the customer.
                    */
                   public void setEmail(String email) {
                       this.email = email;
                   }

                   @Override
                   public boolean equals(Object o) {
                       if (this == o) return true;
                       if (o == null || getClass() != o.getClass()) return false;
                       CustomerReadPayload that = (CustomerReadPayload) o;
                       return
                           Objects.equals(customerId, that.customerId) &&

                           Objects.equals(orgId, that.orgId) &&

                           Objects.equals(customerName, that.customerName) &&

                           Objects.equals(updatedDate, that.updatedDate) &&

                           Objects.equals(customerType, that.customerType) &&

                           Objects.equals(phoneNumber, that.phoneNumber) &&

                           Objects.equals(email, that.email)
               ;
                   }

                   @Override
                   public int hashCode() {
                       return Objects.hash(
               
                           customerId,
                           orgId,
                           customerName,
                           updatedDate,
                           customerType,
                           phoneNumber,
                           email
                       );
                   }

                   @Override
                   public String toString() {
                       StringBuilder sb = new StringBuilder();
                       sb.append("class CustomerReadPayload {\n");
                       sb.append("    customerId: ").append(customerId).append("\n");
                       sb.append("    orgId: ").append(orgId).append("\n");
                       sb.append("    customerName: ").append(customerName).append("\n");
                       sb.append("    updatedDate: ").append(updatedDate).append("\n");
                       sb.append("    customerType: ").append(customerType).append("\n");
                       sb.append("    phoneNumber: ").append(phoneNumber).append("\n");
                       sb.append("    email: ").append(email).append("\n");
                       sb.append("}");
                       return sb.toString();
                   }
               }
           """.trimIndent()
        assertEquals(expectedDataClass, classBody)
    }


    @Test
    fun generate_AMPKafkaExampleEvent_v1_yaml_CustomerType_enumClass() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_spring_kafka_example.yaml"),
            generated = "CustomerType.java",
            modelPackage = "com.tietoevry.banking.asyncapi.generator.core.model.generated.kafkaexample",
        )
        val enumClass = extractClassBody(generated)
        val expected = """
              public enum CustomerType implements Serializable {
                  PRIVATE,
                  CORPORATE,
              }
              """.trimIndent()
        assertEquals(expected, enumClass)
    }

    @Test
    fun generate_AMPKafkaExampleEvent_v1_yaml_CustomerEmailPayload_dataClass() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_spring_kafka_example.yaml"),
            generated = "CustomerEmailPayload.java",
            modelPackage = "com.tietoevry.banking.asyncapi.generator.core.model.generated.kafkaexample",
        )
        val classBody = extractClassBody(generated)
        val expected = """
               public class CustomerEmailPayload implements Serializable {

                   private String customerId;

                   @Email
                   private String email;

                   public CustomerEmailPayload() {
                       // Default constructor
                   }

                   // All-args constructor
                   public CustomerEmailPayload(
                       String customerId,
                       String email
                   ) {
                       this.customerId = customerId;
                       this.email = email;
                   }

                   /**
                    * Get customerId.
                    * The ID of the customer.
                    * @return String
                    */
                   public String getCustomerId() {
                       return customerId;
                   }

                   /**
                    * Set customerId.
                    * @param customerId The ID of the customer.
                    */
                   public void setCustomerId(String customerId) {
                       this.customerId = customerId;
                   }

                   /**
                    * Get email.
                    * The new email address of the customer.
                    * @return String
                    */
                   public String getEmail() {
                       return email;
                   }

                   /**
                    * Set email.
                    * @param email The new email address of the customer.
                    */
                   public void setEmail(String email) {
                       this.email = email;
                   }

                   @Override
                   public boolean equals(Object o) {
                       if (this == o) return true;
                       if (o == null || getClass() != o.getClass()) return false;
                       CustomerEmailPayload that = (CustomerEmailPayload) o;
                       return
                           Objects.equals(customerId, that.customerId) &&

                           Objects.equals(email, that.email)
               ;
                   }

                   @Override
                   public int hashCode() {
                       return Objects.hash(
               
                           customerId,
                           email
                       );
                   }

                   @Override
                   public String toString() {
                       StringBuilder sb = new StringBuilder();
                       sb.append("class CustomerEmailPayload {\n");
                       sb.append("    customerId: ").append(customerId).append("\n");
                       sb.append("    email: ").append(email).append("\n");
                       sb.append("}");
                       return sb.toString();
                   }
               }
           """.trimIndent()
        assertEquals(expected, classBody)
    }

    @Test
    fun generate_AMPKafkaExampleEvent_v1_yaml_CustomerPhoneNumberPayload_dataClass() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_spring_kafka_example.yaml"),
            generated = "CustomerPhoneNumberPayload.java",
            modelPackage = "com.tietoevry.banking.asyncapi.generator.core.model.generated.kafkaexample",
        )
        val classBody = extractClassBody(generated)
        val expected = """
               public class CustomerPhoneNumberPayload implements Serializable {

                   private String customerId;

                   private String phoneNumber;

                   public CustomerPhoneNumberPayload() {
                       // Default constructor
                   }

                   // All-args constructor
                   public CustomerPhoneNumberPayload(
                       String customerId,
                       String phoneNumber
                   ) {
                       this.customerId = customerId;
                       this.phoneNumber = phoneNumber;
                   }

                   /**
                    * Get customerId.
                    * The ID of the customer.
                    * @return String
                    */
                   public String getCustomerId() {
                       return customerId;
                   }

                   /**
                    * Set customerId.
                    * @param customerId The ID of the customer.
                    */
                   public void setCustomerId(String customerId) {
                       this.customerId = customerId;
                   }

                   /**
                    * Get phoneNumber.
                    * The new phone number of the customer.
                    * @return String
                    */
                   public String getPhoneNumber() {
                       return phoneNumber;
                   }

                   /**
                    * Set phoneNumber.
                    * @param phoneNumber The new phone number of the customer.
                    */
                   public void setPhoneNumber(String phoneNumber) {
                       this.phoneNumber = phoneNumber;
                   }

                   @Override
                   public boolean equals(Object o) {
                       if (this == o) return true;
                       if (o == null || getClass() != o.getClass()) return false;
                       CustomerPhoneNumberPayload that = (CustomerPhoneNumberPayload) o;
                       return
                           Objects.equals(customerId, that.customerId) &&

                           Objects.equals(phoneNumber, that.phoneNumber)
               ;
                   }

                   @Override
                   public int hashCode() {
                       return Objects.hash(
               
                           customerId,
                           phoneNumber
                       );
                   }

                   @Override
                   public String toString() {
                       StringBuilder sb = new StringBuilder();
                       sb.append("class CustomerPhoneNumberPayload {\n");
                       sb.append("    customerId: ").append(customerId).append("\n");
                       sb.append("    phoneNumber: ").append(phoneNumber).append("\n");
                       sb.append("}");
                       return sb.toString();
                   }
               }
           """.trimIndent()
        assertEquals(expected, classBody)
    }
}
