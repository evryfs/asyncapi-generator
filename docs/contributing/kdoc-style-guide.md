# KDoc style guide

This guide defines the KDoc conventions for the core, CLI, Maven plugin, and
Gradle plugin modules. It is part of the [code conventions](code-conventions.md).

## Template

### Data classes

```kotlin
/**
 * One-sentence summary of what this class represents.
 *
 * Optional paragraph explaining behavior, invariants, or boundaries.
 *
 * @property fieldName description of this field
 * @property otherField description of this field
 */
data class Example(
    val fieldName: String,
    val otherField: Int,
)
```

### Regular classes and objects

```kotlin
/**
 * One-sentence summary of what this class does.
 *
 * Optional paragraph on how it does it, or what it doesn't do.
 */
class Example {
    /**
     * One-sentence summary of what this method does.
     *
     * @param paramName description of this parameter
     * @return description of what is returned
     */
    fun doSomething(paramName: String): Result
}
```

### Interfaces

```kotlin
/**
 * One-sentence summary of the contract this interface defines.
 */
interface Example {
    fun doSomething()
}
```

### Enums

```kotlin
/**
 * One-sentence summary of what this enum represents.
 */
enum class Example {
    /** Description of this variant. */
    FIRST,
    /** Description of this variant. */
    SECOND,
}
```

## Rules

1. **First sentence** is always a complete sentence ending with a period. It appears in IDE hover and generated docs.

2. **Data classes** get `@property` for every property. The description should explain what the property holds, not restate its type.

3. **Non-data classes** get `@param` for every constructor parameter, regardless of visibility, including private ones that look self-explanatory. This keeps the style consistent across the codebase, and detekt (`OutdatedDocumentation`) fails when the `@param` list no longer matches the parameters.

4. **Methods** get KDoc only when the name alone doesn't explain the behavior. If the code is self-documenting, don't add KDoc to fill space. A good method name is better than a redundant comment.

5. **Multi-paragraph** only when the class has meaningful invariants, boundaries, or "does not do" statements. Don't use multiple paragraphs just to fill space.

6. **Don't document the obvious.** `ParserNode` doesn't need "This class represents a node in the parser" — the name says that. Focus on what's not obvious: what it carries, how it's used, what it guarantees.

7. **Don't document implementation.** KDoc is for users of the API, not maintainers of the implementation. "This uses SnakeYAML internally" doesn't belong in KDoc.

8. **Internal classes** follow the same rules but can be more concise. They don't need to explain API design decisions.

9. **No references to tests, callers, or history.** Do not list the tests that cover a type, the classes that use it, or how it changed. That information goes stale with every change; test class names and IDE navigation provide it.

10. **Link related types** with `[ClassName]` syntax. This creates clickable references in IDE and generated docs.
