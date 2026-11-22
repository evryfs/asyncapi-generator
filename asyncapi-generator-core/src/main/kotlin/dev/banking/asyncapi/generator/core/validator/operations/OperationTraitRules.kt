package dev.banking.asyncapi.generator.core.validator.operations

import dev.banking.asyncapi.generator.core.model.operations.OperationTrait
import dev.banking.asyncapi.generator.core.validator.walk.ObjectRules
import dev.banking.asyncapi.generator.core.validator.walk.ValidationPath
import dev.banking.asyncapi.generator.core.validator.walk.ValidationWalk

/** Validates the Operation Trait Object and walks its security, tags, external documentation and bindings. */
internal class OperationTraitRules : ObjectRules<OperationTrait> {
    override fun check(
        node: OperationTrait,
        path: ValidationPath,
        walk: ValidationWalk,
    ) {
        walk.securitySchemes(node.security, path)
        walk.tags(node.tags, path)
        walk.externalDocs(node.externalDocs, path)
        walk.bindings(node.bindings, path)
    }
}
