package dev.banking.asyncapi.generator.core.model.schemas

/** Every schema this schema holds directly, in declaration order: subschemas, properties, definitions and the like. */
internal fun Schema.childSchemas(): List<SchemaInterface> = childSchemasByPath().map { (_, child) -> child }

/**
 * [childSchemas], each with its JSON path relative to this schema, such as `.items[0]` or `.properties['id']`.
 */
internal fun Schema.childSchemasByPath(): List<Pair<String, SchemaInterface>> =
    buildList {
        single(".items", items)
        indexed(".items", tupleItems)
        single(".additionalItems", additionalItems)
        single(".contains", contains)
        named(".properties", properties)
        named(".patternProperties", patternProperties)
        single(".additionalProperties", additionalProperties)
        single(".propertyNames", propertyNames)
        named(
            ".dependencies",
            dependencies?.filterValues { it is SchemaInterface }?.mapValues { it.value as SchemaInterface },
        )
        named(".definitions", definitions)
        indexed(".allOf", allOf)
        indexed(".anyOf", anyOf)
        indexed(".oneOf", oneOf)
        single(".not", not)
        single(".if", ifSchema)
        single(".then", thenSchema)
        single(".else", elseSchema)
    }

private fun MutableList<Pair<String, SchemaInterface>>.single(
    path: String,
    child: SchemaInterface?,
) {
    if (child != null) add(path to child)
}

private fun MutableList<Pair<String, SchemaInterface>>.indexed(
    path: String,
    children: List<SchemaInterface>?,
) {
    children?.forEachIndexed { index, child -> add("$path[$index]" to child) }
}

private fun MutableList<Pair<String, SchemaInterface>>.named(
    path: String,
    children: Map<String, SchemaInterface>?,
) {
    children?.forEach { (name, child) -> add("$path['$name']" to child) }
}
