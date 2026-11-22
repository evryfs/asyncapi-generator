package dev.banking.asyncapi.generator.core.validator.schemas

/** Checks an instance value against any schema form, so that rules for one value kind can descend into children. */
internal fun interface InstanceEvaluator {
    /**
     * Checks [value] against [schema].
     *
     * @param schema the schema, schema reference or boolean schema to check against
     * @param value the instance value
     * @param path where the value sits in the instance
     * @param visited the schema and path pairs already being checked, which stops cyclic references
     * @return what the value violates
     */
    fun evaluate(
        schema: Any,
        value: Any?,
        path: String,
        visited: MutableSet<InstanceVisit>,
    ): InstanceEvaluation
}
