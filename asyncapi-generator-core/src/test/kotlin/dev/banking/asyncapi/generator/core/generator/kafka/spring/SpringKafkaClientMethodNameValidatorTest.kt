package dev.banking.asyncapi.generator.core.generator.kafka.spring

import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedChannel
import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedMessage
import dev.banking.asyncapi.generator.core.generator.model.SourceLanguage
import dev.banking.asyncapi.generator.core.generator.plan.GenerationTask
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.SpringKafkaClientMethodNameCollision
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SpringKafkaClientMethodNameValidatorTest {
    @Test
    fun `allows messages with distinct generated method names`() {
        SpringKafkaClientMethodNameValidator.validate(
            channels =
                listOf(
                    channel(
                        message(messageId = "account-created", generatedName = "AccountCreated"),
                        message(messageId = "account-updated", generatedName = "AccountUpdated"),
                    ),
                ),
            task = task(),
        )
    }

    @Test
    fun `reports all generated methods for colliding message names`() {
        val error =
            assertFailsWith<SpringKafkaClientMethodNameCollision> {
                SpringKafkaClientMethodNameValidator.validate(
                    channels =
                        listOf(
                            channel(
                                message(messageId = "account-updated", generatedName = "AccountUpdated"),
                                message(messageId = "account_updated", generatedName = "AccountUpdated"),
                            ),
                        ),
                    task = task(),
                )
            }

        assertEquals(
            """

            Spring Kafka client generation failed for channel 'accountEvents'.
            Channel messages ['account-updated', 'account_updated'] resolve to generated message name 'AccountUpdated'.
            Each of these client methods would be generated more than once: ['sendAccountUpdated', 'listenAccountUpdated']
            Give each Message Object a unique 'name', or use unique channel message keys when 'name' is omitted.
            Names must remain unique after conversion to source-code identifiers.
            """.trimIndent(),
            error.message,
        )
    }

    private fun channel(vararg messages: AnalyzedMessage): AnalyzedChannel =
        AnalyzedChannel(
            channelName = "accountEvents",
            topic = "account.events",
            messages = messages.toList(),
        )

    private fun message(
        messageId: String,
        generatedName: String,
    ): AnalyzedMessage =
        AnalyzedMessage(
            messageId = messageId,
            messageName = generatedName,
            payloadTypeName = "${generatedName}Payload",
            schema = Schema(type = "object"),
        )

    private fun task(): GenerationTask.SpringKafkaClient =
        GenerationTask.SpringKafkaClient(
            language = SourceLanguage.KOTLIN,
            clientPackage = "com.example.client",
            modelPackage = "com.example.model",
        )
}
