package dev.banking.asyncapi.generator.core.generator.jsonschema

import dev.banking.asyncapi.generator.core.model.schemas.Schema

/**
 * A Draft 7 keyword whose value holds sub-schemas.
 *
 * @property name the keyword
 * @property shape how the keyword holds its sub-schemas
 * @property value the keyword's value in a parsed schema
 */
internal class SubschemaKeyword(
    val name: String,
    val shape: Shape,
    val value: (Schema) -> Any?,
) {
    /** How a keyword holds sub-schemas: one schema, a map of named schemas, or a list of schemas. */
    enum class Shape { SINGLE, MAP, LIST }

    companion object {
        /**
         * The Draft 7 keywords that hold sub-schemas, which both passes over a schema visit. Tuple-form
         * `items` is an array of schemas, so a single `items` keyword visits it too.
         */
        val ALL =
            listOf(
                SubschemaKeyword("items", Shape.SINGLE) { it.items },
                SubschemaKeyword("additionalItems", Shape.SINGLE) { it.additionalItems },
                SubschemaKeyword("contains", Shape.SINGLE) { it.contains },
                SubschemaKeyword("properties", Shape.MAP) { it.properties },
                SubschemaKeyword("patternProperties", Shape.MAP) { it.patternProperties },
                SubschemaKeyword("additionalProperties", Shape.SINGLE) { it.additionalProperties },
                SubschemaKeyword("propertyNames", Shape.SINGLE) { it.propertyNames },
                SubschemaKeyword("dependencies", Shape.MAP) { it.dependencies },
                SubschemaKeyword("definitions", Shape.MAP) { it.definitions },
                SubschemaKeyword("allOf", Shape.LIST) { it.allOf },
                SubschemaKeyword("anyOf", Shape.LIST) { it.anyOf },
                SubschemaKeyword("oneOf", Shape.LIST) { it.oneOf },
                SubschemaKeyword("not", Shape.SINGLE) { it.not },
                SubschemaKeyword("if", Shape.SINGLE) { it.ifSchema },
                SubschemaKeyword("then", Shape.SINGLE) { it.thenSchema },
                SubschemaKeyword("else", Shape.SINGLE) { it.elseSchema },
            )
    }
}
