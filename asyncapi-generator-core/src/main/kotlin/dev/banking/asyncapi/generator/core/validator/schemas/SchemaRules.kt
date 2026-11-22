package dev.banking.asyncapi.generator.core.validator.schemas

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.EXTERNAL_DOC
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.SCHEMA
import dev.banking.asyncapi.generator.core.model.schemas.MultiFormatSchema
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import dev.banking.asyncapi.generator.core.validator.walk.ValidationPath
import dev.banking.asyncapi.generator.core.validator.walk.ValidationWalk

/**
 * Validates Schema Objects, boolean schemas and Multi Format Schema Objects, and walks the schemas nested in
 * them, their external documentation and their bindings.
 *
 * @param asyncApiContext the load the schemas belong to
 */
internal class SchemaRules(
    private val asyncApiContext: AsyncApiContext,
) {
    private val propertyDeclarations = SchemaPropertyDeclarations(asyncApiContext)
    private val keywords = SchemaKeywordRules(asyncApiContext)
    private val values = SchemaValueRules(asyncApiContext)
    private val constraints = SchemaConstraintRules(asyncApiContext)
    private val objects = SchemaObjectRules(asyncApiContext)
    private val unions = SchemaUnionRules(asyncApiContext)

    /** Checks a Schema Object, a boolean schema, a Multi Format Schema Object or a `$ref` to a schema. */
    fun checkInterface(
        schema: SchemaInterface,
        path: ValidationPath,
        walk: ValidationWalk,
    ) {
        checkInterface(schema, path, walk, emptySet())
    }

    /** Checks the schemas of a `schemas` map, each named `Schema 'name'` below [path]. */
    fun checkMap(
        schemas: Map<String, SchemaInterface>,
        path: ValidationPath,
        walk: ValidationWalk,
    ) {
        schemas.forEach { (name, schema) ->
            checkInterface(schema, path.named("Schema", name), walk, emptySet())
        }
    }

    /** Checks the schema [model] a reference led to. */
    fun checkModel(
        model: Any,
        path: ValidationPath,
        walk: ValidationWalk,
    ) {
        when (model) {
            is Schema -> check(model, path, walk, emptySet())
            is MultiFormatSchema -> checkInterface(SchemaInterface.MultiFormatSchemaInline(model), path, walk)
            is SchemaInterface.BooleanSchema -> checkInterface(model, path, walk)
        }
    }

    private fun checkInterface(
        schemaInterface: SchemaInterface,
        path: ValidationPath,
        walk: ValidationWalk,
        enclosingDeclarations: Set<String>,
    ) {
        when (schemaInterface) {
            is SchemaInterface.SchemaInline ->
                check(schemaInterface.schema, path, walk, enclosingDeclarations)

            is SchemaInterface.SchemaReference ->
                checkReference(schemaInterface.reference, path, walk)

            is SchemaInterface.MultiFormatSchemaInline -> walk.results.visit(schemaInterface.multiFormatSchema)
            is SchemaInterface.BooleanSchema -> walk.results.visit(schemaInterface)
        }
    }

    private fun check(
        node: Schema,
        path: ValidationPath,
        walk: ValidationWalk,
        enclosingDeclarations: Set<String>,
    ) {
        val results = walk.results
        if (!results.visit(node)) return
        val applicableDeclarations = enclosingDeclarations + propertyDeclarations.collect(node)
        val schema = CheckedSchema(node, path, results)
        checkKeywordsAndConstraints(schema, applicableDeclarations)
        node.externalDocs?.let { walk.member(it, EXTERNAL_DOC, path.child("ExternalDocs")) }
        walk.bindings(node.bindings, path)
        checkMemberSchemas(node, path, walk)
        checkComposedSchemas(node, path, walk, applicableDeclarations)
        // Members are resolved by the recursion above.
        unions.checkMembers(schema)
    }

    private fun checkKeywordsAndConstraints(
        schema: CheckedSchema,
        applicableDeclarations: Set<String>,
    ) {
        keywords.checkKeywords(schema.node, schema.path, schema.results)
        keywords.checkDialect(schema)
        values.checkType(schema)
        values.checkEnum(schema)
        values.checkConst(schema)
        constraints.checkNumeric(schema)
        constraints.checkStringLength(schema)
        constraints.checkArraySize(schema)
        constraints.checkObjectSize(schema)
        objects.checkRequired(schema, applicableDeclarations)
        objects.checkDependencies(schema)
        values.checkDefault(schema)
        objects.checkDiscriminator(schema)
    }

    /** Checks the schemas a schema declares for its own parts; their property declarations start afresh. */
    private fun checkMemberSchemas(
        node: Schema,
        path: ValidationPath,
        walk: ValidationWalk,
    ) {
        node.properties?.forEach { (name, subSchema) ->
            checkInterface(subSchema, path.named("Property", name), walk, emptySet())
        }
        node.patternProperties?.forEach { (pattern, subSchema) ->
            checkInterface(subSchema, path.named("Pattern property", pattern), walk, emptySet())
        }
        node.definitions?.forEach { (name, subSchema) ->
            checkInterface(subSchema, path.named("Definition", name), walk, emptySet())
        }
        node.items?.let { checkInterface(it, path, walk, emptySet()) }
        node.tupleItems?.forEachIndexed { index, subSchema ->
            checkInterface(subSchema, path.child("tuple item $index"), walk, emptySet())
        }
        listOfNotNull(node.additionalItems, node.additionalProperties, node.contains, node.propertyNames)
            .forEach { checkInterface(it, path, walk, emptySet()) }
    }

    /** Checks the schemas a schema composes, which share its property declarations. */
    private fun checkComposedSchemas(
        node: Schema,
        path: ValidationPath,
        walk: ValidationWalk,
        applicableDeclarations: Set<String>,
    ) {
        node.dependencies?.forEach { (name, dependency) ->
            if (dependency is SchemaInterface) {
                checkInterface(dependency, path.named("dependency", name), walk, applicableDeclarations)
            }
        }
        listOfNotNull(node.allOf, node.anyOf, node.oneOf).flatten().forEach { subSchema ->
            checkInterface(subSchema, path, walk, applicableDeclarations)
        }
        listOfNotNull(node.not, node.ifSchema, node.thenSchema, node.elseSchema).forEach { subSchema ->
            checkInterface(subSchema, path, walk, applicableDeclarations)
        }
    }

    private fun checkReference(
        reference: Reference,
        path: ValidationPath,
        walk: ValidationWalk,
    ) {
        if (!walk.results.visit(reference)) return
        keywords.checkKeywords(reference, path, walk.results)
        walk.reference(reference, SCHEMA, path)
    }
}
