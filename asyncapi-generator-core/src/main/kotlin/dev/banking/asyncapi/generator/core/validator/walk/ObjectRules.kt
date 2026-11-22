package dev.banking.asyncapi.generator.core.validator.walk

/** The rules of one kind of AsyncAPI object. */
internal fun interface ObjectRules<in T : Any> {
    /**
     * Checks [node], reports its findings to [walk] and walks its children through [walk].
     *
     * @param node the object
     * @param path the object as findings name it
     * @param walk the validation run
     */
    fun check(
        node: T,
        path: ValidationPath,
        walk: ValidationWalk,
    )
}
