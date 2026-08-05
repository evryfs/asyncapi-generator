package dev.banking.asyncapi.generator.core.validator.bindings

import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.BINDING_PROTOCOL_TYPE
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
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class BindingValidatorTest : AbstractValidatorTest() {

    private val asyncApiValidator = AsyncApiValidator(asyncApiContext)

    @Test
    fun `Kafka binding versions 0_4 and 0_5 pass at every supported location`() {
        val document = parse("validator/bindings/asyncapi_validator_binding_valid.yaml")
        val results = asyncApiValidator.validate(document)

        assertEquals(emptyList(), results.findings)
    }

    @Test
    fun `Kafka schema id fields pass when every selected Kafka server has a schema registry`() {
        val results = validate("validator/bindings/asyncapi_validator_kafka_registry_relationship_valid.yaml")

        assertEquals(emptyList(), results.findings)
    }

    @Test
    fun `Kafka schema id fields require a registry on selected and default Kafka servers`() {
        val results = validate("validator/bindings/asyncapi_validator_kafka_registry_relationship_invalid.yaml")

        assertEquals(5, results.errors.size)
        assertEquals(5, results.findings.count { it.code == KAFKA_MESSAGE_SCHEMA_REGISTRY_REQUIRED.code })

        val directChannel = results.findings.single {
            it.path ==
                "asyncapi_validator_kafka_registry_relationship_invalid.root.channels.selectedWithoutRegistry.messages.inline.bindings.kafka.schemaIdLocation"
        }
        assertEquals(25, directChannel.line)

        val directPayloadEncoding = results.findings.single {
            it.path ==
                "asyncapi_validator_kafka_registry_relationship_invalid.root.channels.selectedWithoutRegistry.messages.inline.bindings.kafka.schemaIdPayloadEncoding"
        }
        assertEquals(26, directPayloadEncoding.line)

        val defaultChannel = results.findings.single {
            it.path ==
                "asyncapi_validator_kafka_registry_relationship_invalid.root.channels.allRootServers.messages.inline.bindings.kafka.schemaLookupStrategy"
        }
        assertEquals(39, defaultChannel.line)

        val messageBinding = results.findings.single {
            it.path ==
                "asyncapi_validator_kafka_registry_relationship_invalid.root.components.messages.ReusableEvent.bindings.kafka.schemaIdLocation"
        }
        assertEquals(53, messageBinding.line)

        val traitBinding = results.findings.single {
            it.path ==
                "asyncapi_validator_kafka_registry_relationship_invalid.root.components.messageTraits.RegistryTrait.bindings.kafka.schemaLookupStrategy"
        }
        assertEquals(59, traitBinding.line)
    }

    @Test
    fun `Kafka schema registry relationship follows an external message fragment`() {
        val results = validate("validator/bindings/asyncapi_validator_kafka_registry_external.yaml")

        assertEquals(1, results.errors.size)
        val external = results.findings.single()
        assertEquals(KAFKA_MESSAGE_SCHEMA_REGISTRY_REQUIRED.code, external.code)
        assertEquals(KAFKA_MESSAGE_SCHEMA_REGISTRY_REQUIRED.severity, external.severity)
        assertEquals(KAFKA_MESSAGE_SCHEMA_REGISTRY_REQUIRED.concern, external.concern)
        assertEquals("kafka_registry_external_message.yaml", external.sourceLocation?.file?.name)
        assertEquals("kafka_registry_external_message.root.bindings.kafka.schemaIdLocation", external.path)
        assertEquals(3, external.line)
    }

    @Test
    fun `Kafka rules report exact fields for each binding location`() {
        val results = validate("validator/bindings/asyncapi_validator_binding_invalid.yaml")

        assertEquals(18, results.errors.size)

        val protocolType = results.findings.single { it.code == BINDING_PROTOCOL_TYPE.code }
        assertEquals(BINDING_PROTOCOL_TYPE.severity, protocolType.severity)
        assertEquals(BINDING_PROTOCOL_TYPE.concern, protocolType.concern)
        assertEquals("asyncapi_validator_binding_invalid.yaml", protocolType.sourceLocation?.file?.name)
        assertEquals(
            "asyncapi_validator_binding_invalid.root.components.channelBindings.InvalidProtocol.kafka",
            protocolType.path,
        )
        assertEquals(19, protocolType.line)

        val protocolRegistryUrl = results.findings.single {
            it.code == KAFKA_SCHEMA_REGISTRY_URL_FORMAT.code
        }
        assertEquals(KAFKA_SCHEMA_REGISTRY_URL_FORMAT.severity, protocolRegistryUrl.severity)
        assertEquals(KAFKA_SCHEMA_REGISTRY_URL_FORMAT.concern, protocolRegistryUrl.concern)
        assertEquals(
            "asyncapi_validator_binding_invalid.root.components.serverBindings.InvalidServer.kafka.schemaRegistryUrl",
            protocolRegistryUrl.path,
        )
        assertEquals(9, protocolRegistryUrl.line)

        val vendorRequiresUrl = results.findings.single {
            it.code == KAFKA_SCHEMA_REGISTRY_VENDOR_REQUIRES_URL.code
        }
        assertEquals(KAFKA_SCHEMA_REGISTRY_VENDOR_REQUIRES_URL.severity, vendorRequiresUrl.severity)
        assertEquals(KAFKA_SCHEMA_REGISTRY_VENDOR_REQUIRES_URL.concern, vendorRequiresUrl.concern)
        assertEquals(
            "asyncapi_validator_binding_invalid.root.components.serverBindings.MissingRegistryUrl.kafka.schemaRegistryVendor",
            vendorRequiresUrl.path,
        )
        assertEquals(15, vendorRequiresUrl.line)

        val positiveIntegerPartitions = results.findings.single {
            it.code == KAFKA_CHANNEL_POSITIVE_INTEGER.code &&
                it.path ==
                    "asyncapi_validator_binding_invalid.root.components.channelBindings.InvalidChannel.kafka.partitions"
        }
        assertEquals(KAFKA_CHANNEL_POSITIVE_INTEGER.severity, positiveIntegerPartitions.severity)
        assertEquals(KAFKA_CHANNEL_POSITIVE_INTEGER.concern, positiveIntegerPartitions.concern)
        assertEquals(
            "asyncapi_validator_binding_invalid.root.components.channelBindings.InvalidChannel.kafka.partitions",
            positiveIntegerPartitions.path,
        )
        assertEquals(23, positiveIntegerPartitions.line)

        val positiveIntegerReplicas = results.findings.single {
            it.code == KAFKA_CHANNEL_POSITIVE_INTEGER.code &&
                it.path ==
                    "asyncapi_validator_binding_invalid.root.components.channelBindings.InvalidChannel.kafka.replicas"
        }
        assertEquals(KAFKA_CHANNEL_POSITIVE_INTEGER.severity, positiveIntegerReplicas.severity)
        assertEquals(KAFKA_CHANNEL_POSITIVE_INTEGER.concern, positiveIntegerReplicas.concern)
        assertEquals(
            "asyncapi_validator_binding_invalid.root.components.channelBindings.InvalidChannel.kafka.replicas",
            positiveIntegerReplicas.path,
        )
        assertEquals(24, positiveIntegerReplicas.line)

        val cleanupPolicy = results.findings.single { it.code == KAFKA_CLEANUP_POLICY.code }
        assertEquals(KAFKA_CLEANUP_POLICY.severity, cleanupPolicy.severity)
        assertEquals(KAFKA_CLEANUP_POLICY.concern, cleanupPolicy.concern)
        assertEquals(
            "asyncapi_validator_binding_invalid.root.components.channelBindings.InvalidChannel.kafka.topicConfiguration[\"cleanup.policy\"]",
            cleanupPolicy.path,
        )
        assertEquals(26, cleanupPolicy.line)

        val invalidVersionType = results.findings.single {
            it.code == KAFKA_BINDING_VERSION_TYPE.code &&
                it.path ==
                    "asyncapi_validator_binding_invalid.root.components.channelBindings.InvalidVersionType.kafka.bindingVersion"
        }
        assertEquals(KAFKA_BINDING_VERSION_TYPE.severity, invalidVersionType.severity)
        assertEquals(KAFKA_BINDING_VERSION_TYPE.concern, invalidVersionType.concern)
        assertEquals(32, invalidVersionType.line)

        val nullVersion = results.findings.single {
            it.code == KAFKA_BINDING_VERSION_TYPE.code &&
                it.path ==
                    "asyncapi_validator_binding_invalid.root.components.channelBindings.NullVersion.kafka.bindingVersion"
        }
        assertEquals(KAFKA_BINDING_VERSION_TYPE.severity, nullVersion.severity)
        assertEquals(KAFKA_BINDING_VERSION_TYPE.concern, nullVersion.concern)
        assertEquals(35, nullVersion.line)

        val unexpectedOperationField = results.findings.single {
            it.code == KAFKA_BINDING_FIELD_TYPE.code &&
                it.path ==
                    "asyncapi_validator_binding_invalid.root.components.operationBindings.InvalidOperation.kafka.groupId"
        }
        assertEquals(KAFKA_BINDING_FIELD_TYPE.severity, unexpectedOperationField.severity)
        assertEquals(KAFKA_BINDING_FIELD_TYPE.concern, unexpectedOperationField.concern)
        assertEquals(39, unexpectedOperationField.line)

        val unsupportedVersion = results.findings.single {
            it.code == KAFKA_BINDING_VERSION_UNSUPPORTED.code
        }
        assertEquals(KAFKA_BINDING_VERSION_UNSUPPORTED.severity, unsupportedVersion.severity)
        assertEquals(KAFKA_BINDING_VERSION_UNSUPPORTED.concern, unsupportedVersion.concern)
        assertEquals(
            "asyncapi_validator_binding_invalid.root.components.messageBindings.InvalidMessage.kafka.bindingVersion",
            unsupportedVersion.path,
        )
        assertEquals(49, unsupportedVersion.line)

        assertEquals(4, results.findings.count { it.code == KAFKA_BINDING_FIELD.code })
        assertEquals(5, results.findings.count { it.code == KAFKA_BINDING_FIELD_TYPE.code })
    }

    @Test
    fun `invalid Kafka key schema fails validation`() {
        val results = validate("validator/bindings/asyncapi_validator_kafka_key_invalid.yaml")

        val mismatch = results.findings.single { it.code == SCHEMA_NUMERIC_RANGE.code }
        assertEquals(SCHEMA_NUMERIC_RANGE.severity, mismatch.severity)
        assertEquals(SCHEMA_NUMERIC_RANGE.concern, mismatch.concern)
        assertEquals(
            "asyncapi_validator_kafka_key_invalid.root.components.messageBindings.InvalidKafkaKey.kafka.key.minimum",
            mismatch.path,
        )
        assertEquals(11, mismatch.line)
    }
}
