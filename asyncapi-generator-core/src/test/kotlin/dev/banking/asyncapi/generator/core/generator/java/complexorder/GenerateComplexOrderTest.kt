package dev.banking.asyncapi.generator.core.generator.java.complexorder

import dev.banking.asyncapi.generator.core.generator.AbstractJavaGeneratorClass
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals

class GenerateComplexOrderTest : AbstractJavaGeneratorClass() {

    @Test
    fun `generate OrderLineType data class`() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_complex_order_payload_type.yaml"),
            generated = "OrderLineType.java",
            modelPackage = "dev.banking.asyncapi.generator.core.model.generated.complexorder",
        )
        val classBody = extractClassBody(generated)
        val expected = """
               public class OrderLineType implements Serializable {

                   @Size(min = 1, max = 50)
                   @NotNull
                   private String productCode;

                   @Min(1L)
                   @Max(100000L)
                   @NotNull
                   private Integer quantity;

                   @DecimalMin(value = "0", inclusive = false)
                   @NotNull
                   private BigDecimal unitPrice;

                   public OrderLineType() {
                       // Default constructor
                   }

                   // All-args constructor
                   public OrderLineType(
                       String productCode,
                       Integer quantity,
                       BigDecimal unitPrice
                   ) {
                       this.productCode = productCode;
                       this.quantity = quantity;
                       this.unitPrice = unitPrice;
                   }

                   /**
                    * Get productCode.
                    * Internal product identifier or SKU.
                    * @return String
                    */
                   public String getProductCode() {
                       return productCode;
                   }

                   /**
                    * Set productCode.
                    * @param productCode Internal product identifier or SKU.
                    */
                   public void setProductCode(String productCode) {
                       this.productCode = productCode;
                   }

                   /**
                    * Get quantity.
                    * Ordered quantity.
                    * @return Integer
                    */
                   public Integer getQuantity() {
                       return quantity;
                   }

                   /**
                    * Set quantity.
                    * @param quantity Ordered quantity.
                    */
                   public void setQuantity(Integer quantity) {
                       this.quantity = quantity;
                   }

                   /**
                    * Get unitPrice.
                    * Price per unit in the order currency.
                    * @return BigDecimal
                    */
                   public BigDecimal getUnitPrice() {
                       return unitPrice;
                   }

                   /**
                    * Set unitPrice.
                    * @param unitPrice Price per unit in the order currency.
                    */
                   public void setUnitPrice(BigDecimal unitPrice) {
                       this.unitPrice = unitPrice;
                   }

                   @Override
                   public boolean equals(Object o) {
                       if (this == o) return true;
                       if (o == null || getClass() != o.getClass()) return false;
                       OrderLineType that = (OrderLineType) o;
                       return
                           Objects.equals(productCode, that.productCode) &&

                           Objects.equals(quantity, that.quantity) &&

                           Objects.equals(unitPrice, that.unitPrice)
               ;
                   }

                   @Override
                   public int hashCode() {
                       return Objects.hash(
               
                           productCode,
                           quantity,
                           unitPrice
                       );
                   }

                   @Override
                   public String toString() {
                       StringBuilder sb = new StringBuilder();
                       sb.append("class OrderLineType {\n");
                       sb.append("    productCode: ").append(productCode).append("\n");
                       sb.append("    quantity: ").append(quantity).append("\n");
                       sb.append("    unitPrice: ").append(unitPrice).append("\n");
                       sb.append("}");
                       return sb.toString();
                   }
               }
           """.trimIndent()
        assertEquals(expected, classBody)
    }

    @Test
    fun `generate ComplexOrderPayloadType data class`() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_complex_order_payload_type.yaml"),
            generated = "ComplexOrderPayloadType.java",
            modelPackage = "dev.banking.asyncapi.generator.core.model.generated.complexorder",
        )
        val classBody = extractClassBody(generated)
        val expected = """
               public class ComplexOrderPayloadType implements Serializable {

                   @NotNull
                   private UUID orderId;

                   @NotNull
                   private OffsetDateTime createdAt;

                   @Size(min = 3, max = 30)
                   @NotNull
                   private String status;

                   @NotNull
                   @Valid
                   private CustomerWithContacts customer;

                   @NotNull
                   @Valid
                   private List<OrderLineType> orderLines;

                   @Size(max = 2000)
                   private String notes;

                   public ComplexOrderPayloadType() {
                       // Default constructor
                   }

                   // All-args constructor
                   public ComplexOrderPayloadType(
                       UUID orderId,
                       OffsetDateTime createdAt,
                       String status,
                       CustomerWithContacts customer,
                       List<OrderLineType> orderLines,
                       String notes
                   ) {
                       this.orderId = orderId;
                       this.createdAt = createdAt;
                       this.status = status;
                       this.customer = customer;
                       this.orderLines = orderLines;
                       this.notes = notes;
                   }

                   /**
                    * Get orderId.
                    * Unique identifier for the order.
                    * @return UUID
                    */
                   public UUID getOrderId() {
                       return orderId;
                   }

                   /**
                    * Set orderId.
                    * @param orderId Unique identifier for the order.
                    */
                   public void setOrderId(UUID orderId) {
                       this.orderId = orderId;
                   }

                   /**
                    * Get createdAt.
                    * Timestamp for when the order was created.
                    * @return OffsetDateTime
                    */
                   public OffsetDateTime getCreatedAt() {
                       return createdAt;
                   }

                   /**
                    * Set createdAt.
                    * @param createdAt Timestamp for when the order was created.
                    */
                   public void setCreatedAt(OffsetDateTime createdAt) {
                       this.createdAt = createdAt;
                   }

                   /**
                    * Get status.
                    * Current status of the order. Valid values include:
                    * * `NEW`
                    * * `PROCESSING`
                    * * `COMPLETED`
                    * * `CANCELLED`
                    * @return String
                    */
                   public String getStatus() {
                       return status;
                   }

                   /**
                    * Set status.
                    * @param status Current status of the order. Valid values include:
                    */
                   public void setStatus(String status) {
                       this.status = status;
                   }

                   /**
                    * Get customer.
                    * Customer object with nested contact points and primitive lists.
                    * @return CustomerWithContacts
                    */
                   public CustomerWithContacts getCustomer() {
                       return customer;
                   }

                   /**
                    * Set customer.
                    * @param customer Customer object with nested contact points and primitive lists.
                    */
                   public void setCustomer(CustomerWithContacts customer) {
                       this.customer = customer;
                   }

                   /**
                    * Get orderLines.
                    * One or more order lines included in the order.
                    * @return List<OrderLineType>
                    */
                   public List<OrderLineType> getOrderLines() {
                       return orderLines;
                   }

                   /**
                    * Set orderLines.
                    * @param orderLines One or more order lines included in the order.
                    */
                   public void setOrderLines(List<OrderLineType> orderLines) {
                       this.orderLines = orderLines;
                   }

                   /**
                    * Get notes.
                    * Optional free-text notes attached to the order.
                    * @return String
                    */
                   public String getNotes() {
                       return notes;
                   }

                   /**
                    * Set notes.
                    * @param notes Optional free-text notes attached to the order.
                    */
                   public void setNotes(String notes) {
                       this.notes = notes;
                   }

                   @Override
                   public boolean equals(Object o) {
                       if (this == o) return true;
                       if (o == null || getClass() != o.getClass()) return false;
                       ComplexOrderPayloadType that = (ComplexOrderPayloadType) o;
                       return
                           Objects.equals(orderId, that.orderId) &&

                           Objects.equals(createdAt, that.createdAt) &&

                           Objects.equals(status, that.status) &&

                           Objects.equals(customer, that.customer) &&

                           Objects.equals(orderLines, that.orderLines) &&

                           Objects.equals(notes, that.notes)
               ;
                   }

                   @Override
                   public int hashCode() {
                       return Objects.hash(
               
                           orderId,
                           createdAt,
                           status,
                           customer,
                           orderLines,
                           notes
                       );
                   }

                   @Override
                   public String toString() {
                       StringBuilder sb = new StringBuilder();
                       sb.append("class ComplexOrderPayloadType {\n");
                       sb.append("    orderId: ").append(orderId).append("\n");
                       sb.append("    createdAt: ").append(createdAt).append("\n");
                       sb.append("    status: ").append(status).append("\n");
                       sb.append("    customer: ").append(customer).append("\n");
                       sb.append("    orderLines: ").append(orderLines).append("\n");
                       sb.append("    notes: ").append(notes).append("\n");
                       sb.append("}");
                       return sb.toString();
                   }
               }
           """.trimIndent()
        assertEquals(expected, classBody)
    }


    @Test
    fun `generate CustomerWithContacts data class from referenced file`() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_complex_order_payload_type.yaml"),
            generated = "CustomerWithContacts.java",
            modelPackage = "dev.banking.asyncapi.generator.core.model.generated.complexorder",
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
}
