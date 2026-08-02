@file:Suppress("UNCHECKED_CAST")

package dev.banking.asyncapi.generator.core.validator.bindings

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.bindings.Binding
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.BINDING_EMPTY
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.BINDING_PROPERTY_EMPTY
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.BINDING_PROPERTY_LIST
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.BINDING_PROPERTY_NULL
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.BINDING_PROPERTY_TYPE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.BINDING_PROTOCOL_NULL
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.BINDING_PROTOCOL_TYPE
import dev.banking.asyncapi.generator.core.validator.schemas.SchemaValidator
import dev.banking.asyncapi.generator.core.validator.util.ValidationCollector
import dev.banking.asyncapi.generator.core.validator.util.ValidatorUtility.sanitizeAny

class BindingValidator(
    val asyncApiContext: AsyncApiContext,
) {
    private val schemaValidator by lazy { SchemaValidator(asyncApiContext) }

    private val protocolValidators = mapOf(
        "kafka" to KafkaBindingValidator(asyncApiContext)
        // Add "http", "amqp", "mqtt" here
    )

    fun validate(binding: Binding, bindingName: String, results: ValidationCollector) {
        if (!results.visit(binding)) return
        binding.kafkaKeySchema?.let { keySchema ->
            schemaValidator.validateInterface(keySchema, "$bindingName Kafka key", results)
        }

        if (binding.content.isEmpty()) {
            results.warn(
                BINDING_EMPTY,
                "$bindingName is empty — no protocol-specific binding properties are defined.",
                sourceLocation = asyncApiContext.getSourceLocation(binding, binding::content),
            )
            return
        }

        // Heuristic: Check if unwrapped (direct properties) or wrapped (protocol map)
        val isUnwrapped = binding.content.values.any { it !is Map<*, *> && it != null }

        if (isUnwrapped) {
            // If unwrapped, we don't know the protocol key. Fallback to generic validation.
            validateBindingProperties("unknown-protocol", binding.content, binding, results)
        } else {
            // Standard Wrapped Format: { "kafka": { ... }, "http": { ... } }
            binding.content.forEach { (protocol, bindingData) ->
                validateProtocol(protocol, bindingData, binding, results)
            }
        }
    }

    private fun validateProtocol(protocol: String, bindingData: Any?, binding: Binding, results: ValidationCollector) {
        if (bindingData == null) {
            results.warn(
                BINDING_PROTOCOL_NULL,
                "Binding for protocol '$protocol' is null — consider removing or defining a value.",
                sourceLocation = asyncApiContext.getSourceLocation(binding, binding::content),
            )
            return
        }

        if (bindingData !is Map<*, *>) {
            results.error(
                BINDING_PROTOCOL_TYPE,
                "Binding for protocol '$protocol' must be an object (Map), but found ${bindingData::class.simpleName}.",
                sourceLocation = asyncApiContext.getSourceLocation(binding, binding::content),
            )
            return
        }

        val properties = bindingData as Map<String, Any?>

        // Strategy Dispatch: Use specific validator if available, otherwise generic.
        val validator = protocolValidators[protocol]
        if (validator != null) {
            validator.validate(protocol, properties, binding, results)
        } else {
            validateBindingProperties(protocol, properties, binding, results)
        }
    }

    private fun validateBindingProperties(
        protocol: String,
        properties: Map<String, Any?>,
        binding: Binding,
        results: ValidationCollector,
    ) {
        properties.forEach { (key, value) ->
            validateGenericProperty(asyncApiContext, protocol, key, value, binding, results)
        }
    }

    companion object {

        fun validateGenericProperty(
            asyncApiContext: AsyncApiContext,
            protocol: String,
            key: String,
            value: Any?,
            binding: Binding,
            results: ValidationCollector,
        ) {
            when (val mapValue = value?.let(::sanitizeAny)) {
                null -> results.warn(
                    BINDING_PROPERTY_NULL,
                    "Property '$key' in '$protocol' binding is null — consider removing.",
                    sourceLocation = asyncApiContext.getSourceLocation(binding, binding::content),
                )

                is Map<*, *> -> {}
                is List<*> -> {
                    results.warn(
                        BINDING_PROPERTY_LIST,
                        "Property '$key' in '$protocol' binding has type List, which might be unsupported by this generator.",
                        sourceLocation = asyncApiContext.getSourceLocation(binding, binding::content),
                    )
                }

                is String -> {
                    if (mapValue.isBlank()) {
                        results.warn(
                            BINDING_PROPERTY_EMPTY,
                            "Property '$key' in '$protocol' binding is empty — consider removing or defining a value.",
                            sourceLocation = asyncApiContext.getSourceLocation(binding, binding::content),
                        )
                    }
                }

                is Number, is Boolean -> {}
                else -> {
                    results.warn(
                        BINDING_PROPERTY_TYPE,
                        "Property '$key' in '$protocol' binding has unsupported type: ${value::class.simpleName}",
                        sourceLocation = asyncApiContext.getSourceLocation(binding, binding::content),
                    )
                }
            }
        }
    }
}
