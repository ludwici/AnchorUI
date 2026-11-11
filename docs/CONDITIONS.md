# Conditions (NeoForge 1.21.1)

Anchor UI supports **compiled, declarative conditions** for HUD widgets and resource-pack screens. Conditions use the existing namespaced typed Data Binding providers: resource-pack JSON cannot execute arbitrary Java, introduce tick loops, or change server-side gameplay rules. Trusted Java mods may register named operations that the JSON references.

## Widget properties

| Property | Behavior |
| --- | --- |
| `visibleWhen` | The widget is visible only when the condition is true. Its reserved layout bounds remain unchanged when hidden. Visibility of parent containers also affects their children. |
| `enabledWhen` | The widget stays visible but `AbstractWidget.active` becomes false when the condition is false. Vanilla-style buttons then render as disabled and reject clicks. Custom widget implementations should respect `active` when drawing or handling interactions. |

**Both fields default to `true` when omitted.** A resource-pack's existing `hidden: true` or local HUD visibility override cannot be reversed by conditions. Condition results never bypass those controls.

### Example: require multiple conditions

```json
{
  "type": "anchorui:button",
  "id": "mypack:accept",
  "text": "Accept quest",
  "visibleWhen": {
    "all": [
      {
        "left": {"bind": "anchorui:player.level", "fallback": 0},
        "op": "gte",
        "right": 10
      },
      {
        "not": {"bind": "anchorui:player.sneaking", "fallback": false}
      }
    ]
  },
  "enabledWhen": {
    "left": {"bind": "mypack:quest.status", "fallback": "locked"},
    "op": "eq",
    "right": "available"
  },
  "onClick": {
    "type": "anchorui:close_screen"
  }
}
```

The `mypack:quest.status` source is illustrative: another Java mod must register it using `DataValueRegistry` before the screen is constructed. Anchor UI supplies `anchorui:player.level` and `anchorui:player.sneaking` already.

## Expression syntax

Each condition is either a JSON boolean, an existing boolean data binding, a logical expression, or a comparison. Logical groups can be nested:

```json
{
  "any": [
    {"bind": "anchorui:player.creative", "fallback": false},
    {
      "all": [
        {"left": {"bind": "anchorui:player.level", "fallback": 0}, "op": "gte", "right": 10},
        {"not": {"bind": "anchorui:player.sneaking", "fallback": false}}
      ]
    }
  ]
}
```

`all` means all child conditions must pass, `any` means at least one must pass, and `not` negates its child. Both logical arrays require 1–32 elements. The parser enforces a maximum nesting depth and node budget to avoid unbounded JSON complexity.

### Supported comparisons

| `op` | Meaning | Operands |
| --- | --- | --- |
| `eq`, `ne` | Equality / inequality | Same JSON types; numbers are compared numerically |
| `gt`, `gte`, `lt`, `lte` | Greater/less than (including equal) | Finite numbers only |
| `contains` | Substring or array membership | String + string, or array + JSON value |
| `startsWith`, `endsWith` | String prefix/suffix | Strings only |
| `in`, `notIn` | Membership / non-membership | Any value on left; JSON array on right |
| `exists` | Original binding path is available | `left` only |
| `isNull` | Original binding value is missing/null | `left` only |
| `empty` | Array, object, or string is empty | `left` only |

Binary comparisons use `{"left": ..., "op": "...", "right": ...}`. Unary comparisons use `{"left": ..., "op": "..."}` **without** a `right` field.

```json
{
  "left": {"bind": "anchorui:player.profile", "path": "health", "fallback": 0},
  "op": "gt",
  "right": 10
}
```

An operand can be a literal JSON value or `{"bind": "namespace:source", "path": "optional.nested[0].field", "fallback": ...}`. Objects and arrays can also be used as literals. Unlike loose expression languages, a number `10` is **not** equal to a string `"10"`.

For example, check whether a list is empty:

```json
{
  "left": {"bind": "anchorui:player.effects", "fallback": []},
  "op": "empty"
}
```

## Registering custom operations from Java

Other mods can register their own namespaced operations and then use the same `op` field as any built-in comparison. Registration must happen **on the client**, before Anchor UI creates the HUD or screen (for NeoForge, use client setup and `event.enqueueWork`). A resource pack cannot register Java code; it may only reference an operation supplied by an installed mod.

Example: register `mymod:custom_op`, a case-insensitive string-prefix check:

```java
import com.ludwici.anchorui.api.condition.ConditionOperationRegistry;
import net.minecraft.resources.ResourceLocation;

// Run on the client during mod initialization, before UI JSON is loaded.
ConditionOperationRegistry.register(ResourceLocation.fromNamespaceAndPath("mymod", "custom_op"), (left, right) -> {
    if (!left.isJsonPrimitive() || !right.isJsonPrimitive()
            || !left.getAsJsonPrimitive().isString() || !right.getAsJsonPrimitive().isString()) {
        return false;
    }
    String text = left.getAsString();
    String prefix = right.getAsString();
    return text.regionMatches(true, 0, prefix, 0, prefix.length());
});
```

The resource-pack JSON can use it without a custom widget:

```json
{
  "type": "anchorui:button",
  "id": "mymod:custom_button",
  "text": "Available to matching players",
  "enabledWhen": {
    "left": {"bind": "anchorui:player.name", "fallback": ""},
    "op": "mymod:custom_op",
    "right": "dev"
  }
}
```

Binary operations are registered using `ConditionOperationRegistry.register(ResourceLocation id, BiPredicate<JsonElement, JsonElement> operation)`. The callback receives the **resolved** JSON values of `left` and `right` (including explicit fallbacks). These are `JsonElement` values, not raw binding specifications; treat the supplied JSON as read-only.

Unary operations use `registerUnary(ResourceLocation id, Predicate<JsonElement> operation)` and omit `right`:

```java
ConditionOperationRegistry.registerUnary(ResourceLocation.fromNamespaceAndPath("mymod", "is_positive"), value -> {
    return value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber() && value.getAsDouble() > 0;
});
```

```json
{
  "left": {"bind": "anchorui:player.health", "fallback": 0},
  "op": "mymod:is_positive"
}
```

Both methods also accept namespaced strings such as `"mymod:custom_op"`, but `ResourceLocation` is recommended for Java integration.

Custom operations work anywhere Conditions are supported, including `visibleWhen`, `enabledWhen` and nested `all` / `any` / `not` expressions. Existing built-in operations (`eq`, `gte`, etc.) keep their **unchanged, unqualified names**.

- IDs must be namespaced and valid (e.g. `mymod:custom_op`). Duplicate registration throws `IllegalStateException`.
- Missing/JSON-null operands without a fallback make a custom comparison **false**, and the operation is not called. This holds for both unary and binary custom operations.
- Wrong operand types are the Java operation's responsibility; return `false` when values are unsupported. For complicated access checks, prefer explicit typed data providers and fail-safe defaults.
- Unknown `op` IDs, wrong operand arity, and malformed expressions fail during UI construction; only the affected widget is skipped with an error.
- The operation is resolved **once when the JSON expression is compiled**. If it throws at runtime, the enclosing `BoundCondition` logs the exception once and evaluates to `false`; it does not crash the entire screen.
- Operations should be lightweight and without side effects: Conditions are reevaluated during rendering and before relevant input. They are **client-side UI checks**, not server authorization.

## Fallbacks and failure behavior

- Legacy `visibleWhen: true` and `visibleWhen: {"bind":"namespace:boolean","fallback":false}` continue to work.
- A bare legacy `visibleWhen` binding **without a fallback** retains its historical default of `true` when the source returns no usable boolean value.
- An `enabledWhen` boolean binding **without a fallback** instead defaults to **`false`** when the value is unavailable or has the wrong type. This prevents enabling actions based on missing client data.
- Missing comparison operands without a fallback **do not pass** binary comparisons, including `ne` and `notIn`. Specify a typed fallback explicitly if a missing value should participate in a comparison.
- `exists` checks the original bound value (not a fallback), so it remains false when the source/path is missing even if `fallback` is configured. `isNull` checks the original missing/null state.
- Unknown binding IDs or malformed condition syntax are reported when the widget is constructed, and the malformed widget is skipped rather than crashing other UI widgets. Provider exceptions at runtime are logged once per source, and their operands use fallbacks.
- If an entire expression unexpectedly throws at evaluation time, it is treated as false and logged once.

The compiled condition **does not reparse JSON on each frame**. Data providers are looked up once when a widget is initialized, a given source is read at most once per condition evaluation, and `all` / `any` short-circuit. Existing render-driven live updates and the push-style `DataValueRegistry.registerMutable` providers work without a new tick loop.

## Security and planned extensions

A condition is a **client-side presentation decision**, never authorization to grant items, accept a quest, spend currency or modify server data. Any gameplay-modifying action must be validated by the responsible server-side mod.

This milestone implements **conditions on widget visibility and enabled state**. Conditional text/style branches (`cases`), action-level guards (`when`), local UI State and flow-based dialogue choices are **not yet implemented**. They can be built on the compiled condition model in subsequent stages.

## In-game demos

```text
/anchorui screen open anchorui:conditions
/anchorui test conditions on
/anchorui test conditions off
```

The example screen displays a live level, a level-gated button, a sneaking/creative-gated button, and a conditional text label. The HUD demo exercises `all`, `any`, `not`, numeric comparisons and list `empty`. Try changing experience level and crouching, then open F8 to inspect the widgets. Inactive widgets remain in the editor's Widgets list.

For registering data sources, see [Typed Data Binding](DATA_BINDINGS.md).
