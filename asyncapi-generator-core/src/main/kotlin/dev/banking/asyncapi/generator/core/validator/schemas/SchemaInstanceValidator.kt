package dev.banking.asyncapi.generator.core.validator.schemas

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.schemas.MultiFormatSchema
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import dev.banking.asyncapi.generator.core.validator.schemas.InstanceEvaluation.Companion.unsupported
import dev.banking.asyncapi.generator.core.validator.schemas.InstanceEvaluation.Companion.violation

/**
 * Validates JSON-compatible instance values against parsed Draft 7 Schema Objects.
 *
 * @param asyncApiContext the load the schemas belong to
 */
internal class SchemaInstanceValidator(
    private val asyncApiContext: AsyncApiContext,
) {
    private val evaluator = InstanceEvaluator(::evaluate)
    private val arrays = ArrayInstanceRules(evaluator)
    private val objects = ObjectInstanceRules(evaluator)
    private val compositions = CompositionInstanceRules(evaluator)

    /** Checks [value] at [path] against [schema] and reports what it violates. */
    fun validate(
        schema: SchemaInterface,
        value: Any?,
        path: String,
    ): InstanceEvaluation = evaluate(schema, value, path, mutableSetOf())

    private fun evaluate(
        schema: Any,
        value: Any?,
        path: String,
        visited: MutableSet<InstanceVisit>,
    ): InstanceEvaluation {
        if (!visited.add(InstanceVisit(schema, path))) return InstanceEvaluation()

        return when (schema) {
            is SchemaInterface.SchemaInline -> evaluate(schema.schema, value, path, visited)
            is SchemaInterface.SchemaReference -> evaluateReference(schema.reference, value, path, visited)
            is SchemaInterface.BooleanSchema ->
                if (schema.value) InstanceEvaluation() else violation(path, "is rejected by the boolean schema 'false'")
            is SchemaInterface.MultiFormatSchemaInline -> unsupported(path, schema.multiFormatSchema)
            is Schema -> evaluateSchema(schema, value, path, visited)
            is Reference -> evaluateReference(schema, value, path, visited)
            is MultiFormatSchema -> unsupported(path, schema)
            else -> InstanceEvaluation()
        }
    }

    private fun evaluateReference(
        reference: Reference,
        value: Any?,
        path: String,
        visited: MutableSet<InstanceVisit>,
    ): InstanceEvaluation =
        asyncApiContext.modelTracking.findReference(reference)?.let { target ->
            evaluate(target, value, path, visited)
        } ?: InstanceEvaluation()

    private fun evaluateSchema(
        schema: Schema,
        value: Any?,
        path: String,
        visited: MutableSet<InstanceVisit>,
    ): InstanceEvaluation =
        evaluateDeclaredValues(schema, value, path) +
            evaluateByKind(schema, value, path, visited) +
            compositions.evaluate(schema, value, path, visited)

    private fun evaluateDeclaredValues(
        schema: Schema,
        value: Any?,
        path: String,
    ): InstanceEvaluation {
        var result = InstanceEvaluation()
        if (!SchemaValueSemantics.isCompatible(value, schema.type)) {
            result += violation(path, "does not match declared type '${schema.type}'")
        }
        schema.enum?.takeIf { values -> values.none { SchemaValueSemantics.equal(it, value) } }?.let {
            result += violation(path, "is not one of the schema's enum values")
        }
        if (schema.constSet && !SchemaValueSemantics.equal(schema.const, value)) {
            result += violation(path, "does not equal the schema's const value")
        }
        return result
    }

    private fun evaluateByKind(
        schema: Schema,
        value: Any?,
        path: String,
        visited: MutableSet<InstanceVisit>,
    ): InstanceEvaluation =
        when (value) {
            is Number -> ScalarInstanceRules.evaluateNumber(schema, value, path)
            is String -> ScalarInstanceRules.evaluateString(schema, value, path)
            is List<*> -> arrays.evaluate(schema, value, path, visited)
            is Map<*, *> -> objects.evaluate(schema, value, path, visited)
            else -> InstanceEvaluation()
        }
}
