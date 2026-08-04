package dev.banking.asyncapi.generator.core.validator.bindings

import dev.banking.asyncapi.generator.core.model.bindings.Binding
import dev.banking.asyncapi.generator.core.model.bindings.BindingLocation.CHANNEL
import dev.banking.asyncapi.generator.core.model.bindings.ProtocolBinding
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.BINDING_PROTOCOL_TYPE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.BINDING_EMPTY
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.KAFKA_BINDING_FIELD
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.KAFKA_BINDING_FIELD_TYPE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.KAFKA_BINDING_VERSION_TYPE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.KAFKA_BINDING_VERSION_UNSUPPORTED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.KAFKA_CHANNEL_POSITIVE_INTEGER
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.KAFKA_CLEANUP_POLICY
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.KAFKA_SCHEMA_REGISTRY_URL_FORMAT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.KAFKA_SCHEMA_REGISTRY_VENDOR_REQUIRES_URL
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.KAFKA_MESSAGE_SCHEMA_REGISTRY_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_NUMERIC_RANGE
import dev.banking.asyncapi.generator.core.validator.AbstractValidatorTest
import dev.banking.asyncapi.generator.core.validator.AsyncApiValidator
import dev.banking.asyncapi.generator.core.validator.util.ValidationCollector
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class BindingValidatorTest : AbstractValidatorTest() {

    private val asyncApiValidator = AsyncApiValidator(asyncApiContext)

    @Test
    fun `Kafka binding versions 0_4 and 0_5 pass at every supported location`() {
        val document = parse("validator/bindings/asyncapi_validator_binding_valid.yaml")
        val results = asyncApiValidator.validate(document)

        assertNoFindings(results)
    }

    @Test
    fun `Kafka schema id fields pass when every selected Kafka server has a schema registry`() {
        val results = validate("validator/bindings/asyncapi_validator_kafka_registry_relationship_valid.yaml")

        assertNoFindings(results)
    }

    @Test
    fun `Kafka schema id fields require a registry on selected and default Kafka servers`() {
        val results = validate("validator/bindings/asyncapi_validator_kafka_registry_relationship_invalid.yaml")

        assertEquals(5, results.errors.size, "Unexpected findings: ${results.findings}")
        assertRule(
            results,
            KAFKA_MESSAGE_SCHEMA_REGISTRY_REQUIRED,
            path = "asyncapi_validator_kafka_registry_relationship_invalid.root.channels." +
                "selectedWithoutRegistry.messages.inline.bindings.kafka.schemaIdLocation",
            line = 25,
        )
        assertRule(
            results,
            KAFKA_MESSAGE_SCHEMA_REGISTRY_REQUIRED,
            path = "asyncapi_validator_kafka_registry_relationship_invalid.root.channels." +
                "selectedWithoutRegistry.messages.inline.bindings.kafka.schemaIdPayloadEncoding",
            line = 26,
        )
        assertRule(
            results,
            KAFKA_MESSAGE_SCHEMA_REGISTRY_REQUIRED,
            path = "asyncapi_validator_kafka_registry_relationship_invalid.root.channels." +
                "allRootServers.messages.inline.bindings.kafka.schemaLookupStrategy",
            line = 39,
        )
        assertRule(
            results,
            KAFKA_MESSAGE_SCHEMA_REGISTRY_REQUIRED,
            path = "asyncapi_validator_kafka_registry_relationship_invalid.root.components.messages." +
                "ReusableEvent.bindings.kafka.schemaIdLocation",
            line = 53,
        )
        assertRule(
            results,
            KAFKA_MESSAGE_SCHEMA_REGISTRY_REQUIRED,
            path = "asyncapi_validator_kafka_registry_relationship_invalid.root.components.messageTraits." +
                "RegistryTrait.bindings.kafka.schemaLookupStrategy",
            line = 59,
        )
    }

    @Test
    fun `Kafka schema registry relationship follows an external message fragment`() {
        val results = validate("validator/bindings/asyncapi_validator_kafka_registry_external.yaml")

        assertEquals(1, results.errors.size, "Unexpected findings: ${results.findings}")
        assertRule(
            results,
            KAFKA_MESSAGE_SCHEMA_REGISTRY_REQUIRED,
            sourceFile = "kafka_registry_external_message.yaml",
            path = "kafka_registry_external_message.root.bindings.kafka.schemaIdLocation",
            line = 3,
        )
    }

    @Test
    fun `Kafka rules report exact fields for each binding location`() {
        val results = validate("validator/bindings/asyncapi_validator_binding_invalid.yaml")

        assertEquals(17, results.errors.size, "Unexpected findings: ${results.findings}")
        assertRule(
            results,
            BINDING_EMPTY,
            path = "asyncapi_validator_binding_invalid.root.components.channelBindings.EmptyBinding",
            line = 18,
        )
        assertRule(
            results,
            KAFKA_SCHEMA_REGISTRY_URL_FORMAT,
            path = "asyncapi_validator_binding_invalid.root.components.serverBindings.InvalidServer.kafka.schemaRegistryUrl",
            line = 9,
        )
        assertRule(
            results,
            KAFKA_SCHEMA_REGISTRY_VENDOR_REQUIRES_URL,
            path = "asyncapi_validator_binding_invalid.root.components.serverBindings.MissingRegistryUrl.kafka.schemaRegistryVendor",
            line = 15,
        )
        assertRule(
            results,
            KAFKA_CHANNEL_POSITIVE_INTEGER,
            path = "asyncapi_validator_binding_invalid.root.components.channelBindings.InvalidChannel.kafka.partitions",
            line = 22,
        )
        assertRule(
            results,
            KAFKA_CLEANUP_POLICY,
            path =
                "asyncapi_validator_binding_invalid.root.components.channelBindings.InvalidChannel.kafka." +
                    "topicConfiguration[\"cleanup.policy\"]",
            line = 25,
        )
        assertRule(
            results,
            KAFKA_BINDING_VERSION_TYPE,
            path = "asyncapi_validator_binding_invalid.root.components.channelBindings.InvalidVersionType.kafka.bindingVersion",
            line = 31,
        )
        assertRule(
            results,
            KAFKA_BINDING_VERSION_TYPE,
            path = "asyncapi_validator_binding_invalid.root.components.channelBindings.NullVersion.kafka.bindingVersion",
            line = 34,
        )
        assertRule(
            results,
            KAFKA_BINDING_FIELD_TYPE,
            path = "asyncapi_validator_binding_invalid.root.components.operationBindings.InvalidOperation.kafka.groupId",
            line = 38,
        )
        assertRule(
            results,
            KAFKA_BINDING_VERSION_UNSUPPORTED,
            path = "asyncapi_validator_binding_invalid.root.components.messageBindings.InvalidMessage.kafka.bindingVersion",
            line = 48,
        )
        assertEquals(4, results.findings.count { it.code == KAFKA_BINDING_FIELD.code })
        assertEquals(5, results.findings.count { it.code == KAFKA_BINDING_FIELD_TYPE.code })
    }

    @Test
    fun `malformed programmatic protocol content produces a finding instead of an exception`() {
        val binding = Binding(
            content = mapOf("kafka" to listOf("not", "an", "object")),
            protocolBindings = listOf(
                ProtocolBinding(
                    protocol = "kafka",
                    location = CHANNEL,
                    content = listOf("not", "an", "object"),
                    bindingVersion = null,
                ),
            ),
        )
        val collector = ValidationCollector()

        BindingValidator(asyncApiContext).validate(binding, "Programmatic binding", collector)

        assertRule(collector.report(), BINDING_PROTOCOL_TYPE)
    }

    @Test
    fun `invalid Kafka key schema fails validation`() {
        val results = validate("validator/bindings/asyncapi_validator_kafka_key_invalid.yaml")

        assertRule(
            results,
            rule = SCHEMA_NUMERIC_RANGE,
            sourceFile = "asyncapi_validator_kafka_key_invalid.yaml",
            path = "asyncapi_validator_kafka_key_invalid.root.components.messageBindings.InvalidKafkaKey.kafka.key.minimum",
            line = 11,
        )
    }
}
