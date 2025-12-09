# YAML Serialization Customization

This document explains the customization applied to the YAML generation process in `asyncapi-generator-core`. We employ 
custom Jackson serializers to produce fine-tuned YAML that standard Jackson configuration cannot achieve on its own.

## The Problem: Jackson's "Safe" Defaults

By default, Jackson's `YAMLGenerator` prioritizes safety and uniformity. This results in:
1.  **Verbose Arrays:** All lists are written in "Block Style" (bullet points), even simple lists of numbers.
    ```yaml
    # Jackson Default
    point:
      - 1
      - 2
    ```
2.  **Unreadable Strings:** Multi-line strings (like documentation descriptions) are often escaped into a single line.
    ```yaml
    # Jackson Default
    description: "Line 1\nLine 2\nLine 3"
    ```

## The Solution: Bridging Jackson and SnakeYAML

Under the hood, Jackson uses **SnakeYAML** to write the actual output. SnakeYAML has powerful formatting options 
(`DumperOptions`), but Jackson hides most of them behind its high-level abstraction.

To bypass this limitation, we use **Java Reflection** in our custom serializers to directly manipulate the underlying 
SnakeYAML engine.

> **Implementation Note:** 
>
> These serializers access private fields (`_outputOptions`) and methods (`_writeScalar`) of 
> Jackson's `YAMLGenerator`. This creates a coupling to the internal implementation of Jackson. Upgrading the 
> `jackson-dataformat-yaml` dependency should always be done with regression testing of these serializers.
> 
> We will consider switching to kotlinx in the future.

---

## 1. Smart List Serialization (`AsyncApiListSerializer`)

### Goal

We want **Flow Style** (inline `[...]`) for simple lists to save vertical space, but **Block Style** 
(bullet points `- ...`) for complex objects to maintain readability.

### Heuristic

The serializer inspects the list content:
*   **Simple:** If *all* items are primitives (Numbers or Strings) -> Use **Flow Style**.
*   **Complex:** If *any* item is an Object, Map, or List -> Use **Block Style**.

### Mechanism

1.  **Reflection:** Access the private `_outputOptions` field (type `DumperOptions`) from the `YAMLGenerator` instance.
2.  **State Change:** Temporarily set `options.defaultFlowStyle = FlowStyle.FLOW`.
3.  **Write:** Call standard Jackson `gen.writeStartArray()` / `gen.writeEndArray()`. SnakeYAML reads the modified flag and writes `[...]`.
4.  **Restore:** Immediately reset `defaultFlowStyle` to its previous value in a `finally` block.

### Example Output

```yaml
# Simple List (Flow Style)
tags: ["user", "signup", "experimental"]

# Complex List (Block Style)
servers:
  - url: localhost
    protocol: http
  - url: example.com
    protocol: https
```

---

## 2. Scalar Style Control (`AsyncApiStringSerializer`)

### Goal
Allow the input data to "hint" at the desired YAML string style. This is crucial for preserving the readability of Markdown descriptions in the generated AsyncAPI definition.

### Mechanism: Prefix Hinting
The serializer checks the **first character** of the string value. If it matches a known YAML indicator, it uses that style and strips the indicator from the final output.

| Prefix | Style | SnakeYAML Enum | Description |
| :--- | :--- | :--- | :--- |
| `|` | **Literal** | `LITERAL` | Preserves newlines. Great for code blocks/Markdown. |
| `>` | **Folded** | `FOLDED` | Folds newlines into spaces. Good for long text paragraphs. |
| `'` | **Single Quoted** | `SINGLE_QUOTED` | strict quoting. |
| `"` | **Double Quoted** | `DOUBLE_QUOTED` | Allows escape sequences. |
| *(none)* | **Plain** | `PLAIN` | Standard YAML behavior. |

### Mechanism
1.  **Detection:** Identify the prefix char.
2.  **Cleaning:** Remove the prefix (and the immediate newline for block styles) to get the raw content.
3.  **Reflection:** Invoke the private `_writeScalar(value, type, style)` method on `YAMLGenerator`. This method allows passing the specific `ScalarStyle` enum, which the public `writeString()` API does not support.

### Example Output

**Input Data (Kotlin String):**
```kotlin
val desc = """
This is a
multi-line
description.
"""
```

**Generated YAML:**
```yaml
description: |
  This is a
  multi-line
  description.
```
