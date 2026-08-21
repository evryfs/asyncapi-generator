package dev.banking.asyncapi.generator.core.generator.kafka.spring

import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.InvalidKafkaHeaderName
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.KafkaHeaderParameterNameCollision
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class KafkaHeaderParameterNamesTest {
    @Test
    fun `converts wire names to camelCase`() {
        val parameterNames =
            KafkaHeaderParameterNames.resolve(
                headerContractName = "ExampleHeaders",
                wireNames =
                    listOf(
                        "X-EXAMPLE-DATA-OWNER-ID",
                        "Signature",
                        "trace.parent value",
                    ),
            )

        assertEquals(
            mapOf(
                "X-EXAMPLE-DATA-OWNER-ID" to "xExampleDataOwnerId",
                "Signature" to "signature",
                "trace.parent value" to "traceParentValue",
            ),
            parameterNames,
        )
    }

    @Test
    fun `creates identifiers valid in both Java and Kotlin`() {
        val parameterNames =
            KafkaHeaderParameterNames.resolve(
                headerContractName = "ExampleHeaders",
                wireNames = listOf("1ST-HEADER", "class", "when"),
            )

        assertEquals(
            mapOf(
                "1ST-HEADER" to "_1STHeader",
                "class" to "class_",
                "when" to "when_",
            ),
            parameterNames,
        )
    }

    @Test
    fun `rejects a wire name without source identifier characters`() {
        val exception =
            assertFailsWith<InvalidKafkaHeaderName> {
                KafkaHeaderParameterNames.resolve(
                    headerContractName = "ExampleHeaders",
                    wireNames = listOf("---"),
                )
            }

        assertTrue(exception.message.orEmpty().contains("Header '---' cannot be represented"))
    }

    @Test
    fun `rejects source parameter name collisions`() {
        val exception =
            assertFailsWith<KafkaHeaderParameterNameCollision> {
                KafkaHeaderParameterNames.resolve(
                    headerContractName = "ExampleHeaders",
                    wireNames = listOf("X-EXAMPLE-ID", "X_EXAMPLE_ID"),
                )
            }

        assertTrue(
            exception.message.orEmpty().contains(
                "['X-EXAMPLE-ID', 'X_EXAMPLE_ID'] -> 'xExampleId'",
            ),
        )
    }

    @Test
    fun `converts kebab-case wire names to camelCase`() {
        val parameterNames =
            KafkaHeaderParameterNames.resolve(
                headerContractName = "ExampleHeaders",
                wireNames = listOf("x-my-kafka-header"),
            )

        assertEquals(
            mapOf("x-my-kafka-header" to "xMyKafkaHeader"),
            parameterNames,
        )
    }

    @Test
    fun `rejects collision when different wire names produce the same camelCase`() {
        val exception =
            assertFailsWith<KafkaHeaderParameterNameCollision> {
                KafkaHeaderParameterNames.resolve(
                    headerContractName = "ExampleHeaders",
                    wireNames = listOf("Content-Type", "content_type"),
                )
            }

        assertTrue(
            exception.message.orEmpty().contains("contentType"),
        )
    }

    @Test
    fun `converts underscored wire names to camelCase`() {
        val parameterNames =
            KafkaHeaderParameterNames.resolve(
                headerContractName = "ExampleHeaders",
                wireNames = listOf("x_my_header"),
            )

        assertEquals(
            mapOf("x_my_header" to "xMyHeader"),
            parameterNames,
        )
    }
}
