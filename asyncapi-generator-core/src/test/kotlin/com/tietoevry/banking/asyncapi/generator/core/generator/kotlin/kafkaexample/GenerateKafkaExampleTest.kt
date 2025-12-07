package com.tietoevry.banking.asyncapi.generator.core.generator.kotlin.kafkaexample

import com.tietoevry.banking.asyncapi.generator.core.generator.AbstractKotlinGeneratorClass
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals

class GenerateKafkaExampleTest : AbstractKotlinGeneratorClass() {

    @Test
    fun generate_AMPKafkaExampleEvent_v1_yaml_CustomerReadPayload_dataClass() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_spring_kafka_example.yaml"),
            generated = "CustomerReadPayload.kt",
            modelPackage = "com.tietoevry.banking.asyncapi.generator.core.model.generated.kafkaexample",
        )
        val dataClass = extractElement(generated)
        val expectedDataClass = """
        data class CustomerReadPayload(

            val customerId: String,

            @field:Min(0L)
            @field:Max(9999L)
            val orgId: Int? = null,

            val customerName: String? = null,

            val updatedDate: OffsetDateTime? = null,

            val customerType: CustomerType? = null,

            val phoneNumber: String? = null,

            @field:Email
            val email: String? = null
        ) {
        }
    """.trimIndent()
        assertEquals(expectedDataClass, dataClass)
    }


    @Test
    fun generate_AMPKafkaExampleEvent_v1_yaml_AccountType_enumClass() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_spring_kafka_example.yaml"),
            generated = "CustomerType.kt",
            modelPackage = "com.tietoevry.banking.asyncapi.generator.core.model.generated.kafkaexample",
        )
        val enumClass = extractElement(generated)
        val expected = """
           enum class CustomerType {
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
            generated = "CustomerEmailPayload.kt",
            modelPackage = "com.tietoevry.banking.asyncapi.generator.core.model.generated.kafkaexample",
        )
        val dataClass = extractElement(generated)
        val expected = """
        data class CustomerEmailPayload(

            val customerId: String? = null,

            @field:Email
            val email: String? = null
        ) {
        }
    """.trimIndent()
        assertEquals(expected, dataClass)
    }

    @Test
    fun generate_AMPKafkaExampleEvent_v1_yaml_CustomerPhoneNumberPayload_dataClass() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_spring_kafka_example.yaml"),
            generated = "CustomerPhoneNumberPayload.kt",
            modelPackage = "com.tietoevry.banking.asyncapi.generator.core.model.generated.kafkaexample",
        )
        val dataClass = extractElement(generated)
        val expected = """
        data class CustomerPhoneNumberPayload(

            val customerId: String? = null,

            val phoneNumber: String? = null
        ) {
        }
    """.trimIndent()
        assertEquals(expected, dataClass)
    }
}
