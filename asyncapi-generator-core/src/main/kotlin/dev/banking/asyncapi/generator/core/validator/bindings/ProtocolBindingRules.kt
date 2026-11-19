package dev.banking.asyncapi.generator.core.validator.bindings

import dev.banking.asyncapi.generator.core.model.bindings.ProtocolBinding
import dev.banking.asyncapi.generator.core.validator.walk.ValidationPath
import dev.banking.asyncapi.generator.core.validator.walk.ValidationWalk

/** The rules of the bindings of one protocol. */
internal interface ProtocolBindingRules {
    /** The protocol the rules apply to, as written in a `bindings` object, such as `kafka`. */
    val protocol: String

    /**
     * Checks one binding of [protocol].
     *
     * @param binding the binding
     * @param properties the binding's members
     * @param path the binding as findings name it
     * @param walk the validation run
     */
    fun check(
        binding: ProtocolBinding,
        properties: Map<String, Any?>,
        path: ValidationPath,
        walk: ValidationWalk,
    )
}
