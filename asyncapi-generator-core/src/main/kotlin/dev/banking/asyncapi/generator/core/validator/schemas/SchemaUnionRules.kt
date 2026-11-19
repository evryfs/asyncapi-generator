package dev.banking.asyncapi.generator.core.validator.schemas

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil.getPrimaryType
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.UNION_INDISTINGUISHABLE_MEMBERS

/**
 * Validates that the members of a `oneOf` or `anyOf` union can be told apart.
 *
 * @param asyncApiContext the load the schemas belong to
 */
internal class SchemaUnionRules(
    private val asyncApiContext: AsyncApiContext,
) {
    /**
     * Reports union members that declare the same property names while the schema has no discriminator.
     *
     * Without a discriminator, generated code tells union members apart by the property names they
     * declare, so two members with the same names can never be told apart.
     */
    fun checkMembers(schema: CheckedSchema) {
        val node = schema.node
        if (node.discriminator != null) return
        val members =
            (node.oneOf ?: node.anyOf)
                .orEmpty()
                .filterIsInstance<SchemaInterface.SchemaReference>()
                .mapNotNull { member ->
                    (member.reference.model as? Schema)
                        ?.takeIf { it.type.getPrimaryType() == "object" }
                        ?.let { member.reference.ref.substringAfterLast('/') to propertyNames(it) }
                }
        members
            .groupBy({ (_, propertyNames) -> propertyNames }, { (name, _) -> name })
            .values
            .firstOrNull { it.size > 1 }
            ?.let { names ->
                schema.results.warn(
                    UNION_INDISTINGUISHABLE_MEMBERS,
                    "${schema.path} union members ${names.joinToString { "'$it'" }} declare the same property " +
                        "names and have no discriminator, so a payload cannot be told apart between them. " +
                        "Add a 'discriminator' or give the members different properties.",
                    sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::oneOf),
                )
            }
    }

    private fun propertyNames(schema: Schema): Set<String> =
        schema.properties.orEmpty().keys +
            schema.allOf.orEmpty().flatMap { part ->
                when (part) {
                    is SchemaInterface.SchemaInline -> propertyNames(part.schema)
                    is SchemaInterface.SchemaReference ->
                        (part.reference.model as? Schema)
                            ?.let(
                                ::propertyNames,
                            ).orEmpty()
                    else -> emptySet()
                }
            }
}
