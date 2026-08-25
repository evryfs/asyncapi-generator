package dev.banking.asyncapi.generator.core.validator.bindings

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.bindings.Binding
import dev.banking.asyncapi.generator.core.model.bindings.BindingInterface
import dev.banking.asyncapi.generator.core.model.bindings.ProtocolBinding
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.BINDING_PROTOCOL_TYPE
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.BINDING
import dev.banking.asyncapi.generator.core.resolver.ReferenceResolver
import dev.banking.asyncapi.generator.core.validator.schemas.SchemaValidator
import dev.banking.asyncapi.generator.core.validator.util.ValidationCollector

internal class BindingValidator(
    private val asyncApiContext: AsyncApiContext,
) {
    private val referenceResolver = ReferenceResolver(asyncApiContext)
    private val schemaValidator by lazy { SchemaValidator(asyncApiContext) }
    private val kafkaBindingValidator = KafkaBindingValidator(asyncApiContext)

    fun validateInterface(node: BindingInterface, contextString: String, results: ValidationCollector) {
        when (node) {
            is BindingInterface.BindingInline ->
                validate(node.binding, contextString, results)

            is BindingInterface.BindingReference ->
                referenceResolver.resolve(node.reference, BINDING, contextString, results)
        }
    }

    fun validateMap(bindings: Map<String, BindingInterface>?, contextString: String, results: ValidationCollector) {
        bindings?.forEach { (bindingName, bindingInterface) ->
            validateInterface(bindingInterface, "$contextString Binding '$bindingName'", results)
        }
    }

    fun validate(binding: Binding, bindingName: String, results: ValidationCollector) {
        if (!results.visit(binding)) return

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
                sourceLocation = asyncApiContext.modelTracking.getSourceLocation(binding),
            )
            return
        }

        @Suppress("UNCHECKED_CAST")
        val stringProperties = properties as Map<String, Any?>
        if (binding.protocol == "kafka") {
            kafkaBindingValidator.validate(binding, stringProperties, results)
        }
    }
}
