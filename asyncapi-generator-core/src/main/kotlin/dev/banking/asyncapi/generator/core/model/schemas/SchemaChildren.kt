package dev.banking.asyncapi.generator.core.model.schemas

/** Every schema this schema holds directly, in declaration order: subschemas, properties, definitions and the like. */
internal fun Schema.childSchemas(): List<SchemaInterface> =
    buildList {
        items?.let(::add)
        tupleItems?.let(::addAll)
        additionalItems?.let(::add)
        contains?.let(::add)
        properties?.values?.let(::addAll)
        patternProperties?.values?.let(::addAll)
        additionalProperties?.let(::add)
        propertyNames?.let(::add)
        dependencies?.values?.filterIsInstance<SchemaInterface>()?.let(::addAll)
        definitions?.values?.let(::addAll)
        allOf?.let(::addAll)
        anyOf?.let(::addAll)
        oneOf?.let(::addAll)
        not?.let(::add)
        ifSchema?.let(::add)
        thenSchema?.let(::add)
        elseSchema?.let(::add)
    }
