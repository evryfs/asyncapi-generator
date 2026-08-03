package dev.banking.asyncapi.generator.core.validator.bindings

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.bindings.Binding
import dev.banking.asyncapi.generator.core.model.bindings.ProtocolBinding
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.BINDING_EMPTY
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.BINDING_PROTOCOL_TYPE
import dev.banking.asyncapi.generator.core.validator.schemas.SchemaValidator
import dev.banking.asyncapi.generator.core.validator.util.ValidationCollector

internal class BindingValidator(
    private val asyncApiContext: AsyncApiContext,
) {
    private val schemaValidator by lazy { SchemaValidator(asyncApiContext) }
    private val protocolValidators: Map<String, ProtocolValidator> = mapOf(
        "kafka" to KafkaBindingValidator(asyncApiContext),
    )

    fun validate(binding: Binding, bindingName: String, results: ValidationCollector) {
        if (!results.visit(binding)) return

        if (binding.content.isEmpty()) {
            results.warn(
                BINDING_EMPTY,
                "$bindingName is empty — no protocol-specific binding properties are defined.",
                sourceLocation = asyncApiContext.getSourceLocation(binding),
            )
        }

        if (binding.protocolBindings.isEmpty()) {
            binding.kafkaKeySchema?.let { schema ->
                schemaValidator.validateInterface(schema, "$bindingName Kafka key", results)
            }
            return
        }

        binding.protocolBindings.forEach { protocolBinding ->
            validateProtocol(protocolBinding, bindingName, results)
        }
    }

    private fun validateProtocol(
        binding: ProtocolBinding,
        bindingName: String,
        results: ValidationCollector,
    ) {
        val properties = binding.content as? Map<*, *>
        if (properties == null || properties.keys.any { it !is String }) {
            results.error(
                BINDING_PROTOCOL_TYPE,
                "$bindingName '${binding.protocol}' binding must be an object with string property names.",
                sourceLocation = asyncApiContext.getSourceLocation(binding),
            )
            return
        }

        @Suppress("UNCHECKED_CAST")
        val stringProperties = properties as Map<String, Any?>
        protocolValidators[binding.protocol]?.validate(binding, stringProperties, results)
    }
}
