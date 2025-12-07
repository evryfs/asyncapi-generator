@file:Suppress("UNCHECKED_CAST")

package dev.banking.asyncapi.generator.core.validator.bindings

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.bindings.Binding
import dev.banking.asyncapi.generator.core.validator.util.ValidationResults
import dev.banking.asyncapi.generator.core.validator.util.ValidatorUtility.sanitizeAny

class BindingValidator(
    val asyncApiContext: AsyncApiContext,
) {

    private val protocolValidators = mapOf(
        "kafka" to KafkaBindingValidator(asyncApiContext)
        // Add "http", "amqp", "mqtt" here
    )

    fun validate(bindingName: String, binding: Binding, results: ValidationResults) {
        if (binding.content.isEmpty()) {
            results.warn(
                "Binding '$bindingName' is empty — no protocol-specific binding properties are defined.",
                asyncApiContext.getLine(binding, binding::content)
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

    private fun validateProtocol(protocol: String, bindingData: Any?, binding: Binding, results: ValidationResults) {
        if (bindingData == null) {
            results.warn(
                "Binding for protocol '$protocol' is null — consider removing or defining a value.",
                asyncApiContext.getLine(binding, binding::content)
            )
            return
        }

        if (bindingData !is Map<*, *>) {
            results.error(
                "Binding for protocol '$protocol' must be an object (Map), but found ${bindingData::class.simpleName}.",
                asyncApiContext.getLine(binding, binding::content)
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
        results: ValidationResults,
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
            results: ValidationResults,
        ) {
            when (val mapValue = value?.let(::sanitizeAny)) {
                null -> results.warn(
                    "Property '$key' in '$protocol' binding is null — consider removing.",
                    asyncApiContext.getLine(binding, binding::content)
                )

                is Map<*, *> -> {}
                is List<*> -> {
                    results.warn(
                        "Property '$key' in '$protocol' binding has type List, which might be unsupported by this generator.",
                        asyncApiContext.getLine(binding, binding::content)
                    )
                }

                is String -> {
                    if (mapValue.isBlank()) {
                        results.warn(
                            "Property '$key' in '$protocol' binding is empty — consider removing or defining a value.",
                            asyncApiContext.getLine(binding, binding::content)
                        )
                    }
                }

                is Number, is Boolean -> {}
                else -> {
                    results.warn(
                        "Property '$key' in '$protocol' binding has unsupported type: ${value::class.simpleName}",
                        asyncApiContext.getLine(binding, binding::content)
                    )
                }
            }
        }
    }
}
