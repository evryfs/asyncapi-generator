package dev.banking.asyncapi.generator.core.generator.kafka.spring

import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.AmbiguousKafkaHandlerPayloadTypes
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class KafkaHandlerPayloadTypeValidatorTest {
    @Test
    fun `accepts distinct handler payload types`() {
        KafkaHandlerPayloadTypeValidator.validate(
            channelName = "myAccountEvents",
            payloads =
                listOf(
                    KafkaPayload(
                        messageName = "MyAccountCreated",
                        payloadType = "MyAccountCreatedPayload",
                    ),
                    KafkaPayload(
                        messageName = "MyAccountUpdated",
                        payloadType = "MyAccountUpdatedPayload",
                    ),
                ),
        )
    }

    @Test
    fun `rejects messages with the same handler payload type`() {
        val exception =
            assertFailsWith<AmbiguousKafkaHandlerPayloadTypes> {
                KafkaHandlerPayloadTypeValidator.validate(
                    channelName = "myAccountEvents",
                    payloads =
                        listOf(
                            KafkaPayload(
                                messageName = "MyAccountCreated",
                                payloadType = "MyAccountPayload",
                            ),
                            KafkaPayload(
                                messageName = "MyAccountUpdated",
                                payloadType = "MyAccountPayload",
                            ),
                        ),
                )
            }

        assertTrue(
            exception.message.orEmpty().contains(
                "'MyAccountPayload': ['MyAccountCreated', 'MyAccountUpdated']",
            ),
        )
        assertTrue(
            exception.message.orEmpty().contains(
                "Class-level @KafkaListener dispatch selects one @KafkaHandler",
            ),
        )
    }
}
