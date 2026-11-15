# Conditions (NeoForge 1.21.1)

Anchor UI supports **compiled, declarative conditions** for HUD widgets and resource-pack screens. Conditions use the existing namespaced typed Data Binding providers: resource-pack JSON cannot execute arbitrary Java, introduce tick loops, or change server-side gameplay rules. Trusted Java mods may register named operations that the JSON references.

## Widget properties

| Property | Behavior |
| --- | --- |
| `visibleWhen` | The widget is visible only when the condition is true. Hidden children reserve layout space by default; an automatic parent with `layout.collapseHidden: true` removes them from its layout flow. Visibility of parent containers also affects their children. |
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
        "left": {
          "bind": "anchorui:player.level",
          "fallback": 0
        },
        "op": "gte",
        "right": 10
      },
      {
        "not": {
          "bind": "anchorui:player.sneaking",
          "fallback": false
        }
      }
    ]
  },
  "enabledWhen": {
    "left": {
      "bind": "mypack:quest.status",
      "fallback": "locked"
    },
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
    {
      "bind": "anchorui:player.creative",
      "fallback": false
    },
    {
      "all": [
        {
          "left": {
            "bind": "anchorui:player.level",
            "fallback": 0
          },
          "op": "gte",
          "right": 10
        },
        {
          "not": {
            "bind": "anchorui:player.sneaking",
            "fallback": false
          }
        }
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
  "left": {
    "bind": "anchorui:player.profile",
    "path": "health",
    "fallback": 0
  },
  "op": "gt",
  "right": 10
}
```

An operand can be a literal JSON value or `{"bind": "namespace:source", "path": "optional.nested[0].field", "fallback": ...}`. Objects and arrays can also be used as literals. Unlike loose expression languages, a number `10` is **not** equal to a string `"10"`.

For example, check whether a list is empty:

```json
{
  "left": {
    "bind": "anchorui:player.effects",
    "fallback": []
  },
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
    "left": {
      "bind": "anchorui:player.name",
      "fallback": ""
    },
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
  "left": {
    "bind": "anchorui:player.health",
    "fallback": 0
  },
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

## Conditional widget content: `anchorui:cases`

Use the declarative `anchorui:cases` pseudo-widget when an interface should show **one of several complete widgets** based on existing Conditions. It is expanded during document loading into ordinary, individually anchored widgets. The first matching `when` wins, and an optional `otherwise` is shown only if no condition matches.

```json
{
  "type": "anchorui:cases",
  "id": "mypack:status",
  "position": {
    "point": "TOPLEFT",
    "anchor": "TOPLEFT",
    "relativeTo": "screen",
    "x": 20,
    "y": 60
  },
  "cases": [
    {
      "when": {
        "left": {"bind": "mypack:player.status", "fallback": ""},
        "op": "eq",
        "right": "ready"
      },
      "widget": {
        "type": "anchorui:text",
        "text": "Ready"
      }
    },
    {
      "when": true,
      "widget": {
        "type": "anchorui:text",
        "text": "Waiting"
      }
    }
  ]
}
```

The fallback `otherwise` is unnecessary above because the final case matches everything. To add a fallback, remove that unconditional case and add `"otherwise": {"type": "anchorui:text", "text": "Unknown"}`.

- Each entry requires a `when` condition and `widget` object. There must be **1–16 cases**. An optional `otherwise` must be a widget object.
- The `widget` object accepts `type` or `ref`, including registered templates, and can contain normal nested `children` when it is a panel/container.
- Common positioning, size and style fields can be declared on the `anchorui:cases` wrapper; each case widget can override them.
- Generated IDs are `mypack:status/case_0`, `mypack:status/case_1`, etc., and `mypack:status/otherwise`. The wrapper itself is not an instantiated widget or an anchor target.
- All alternatives are created once and use compiled `visibleWhen` conditions, so UI State or data changes can change the selected alternative without rebuilding the screen.
- Invisible alternatives retain their layout space, and child visibility follows their parent as usual. Collapsed-space layouts are a separate future feature.
- Conditional widgets work in resource-pack screens and HUDs, and within `template_list` rows (including row-level alternative templates). Nested case blocks are limited to 8 levels.

## Action-level conditions: `when`

The same Condition expression can be added to any object-form registered UI action:

```json
{
  "type": "anchorui:button",
  "text": "Run action when permitted",
  "onClick": {
    "type": "mymod:run_action",
    "when": {
      "bind": "mymod:can_act",
      "fallback": false
    }
  }
}
```

The action's `when` is compiled once but **evaluated when the event fires**. When false or unavailable, the action is skipped. It applies to click, change, hover, and screen lifecycle events. This is different from `enabledWhen`, which changes widget interactivity/appearance. The reserved `when` field is removed before passing arguments to the registered Java action or its Codec. Action guards do not bypass UI visibility or Minecraft permissions.

## Security and remaining extensions

Conditions and action guards are **client-side presentation decisions**, never authorization to grant items, spend currency or modify world/server data. The integrating server-side mod must validate all protected operations.

The current `cases` feature selects **whole widgets**, not individual text fragments or style properties within a single mutable widget instance. Their hidden alternatives follow the same `layout.collapseHidden` policy as `visibleWhen`: see [Stage 1/2 layout policy](QA_STAGE_1_2.md#hidden-child-layout-policy). Fine-grained condition invalidation and advanced CSS-like layout extensions are optional future work, not a requirement of `cases`. NPC dialogue/quest engines remain outside AnchorUI's framework scope.

## In-game demos

```text
/anchorui screen open anchorui:conditions
/anchorui screen open anchorui:conditional_content_demo
/anchorui test conditions on
/anchorui test conditions off
```

The example screen displays a live level, a level-gated button, a sneaking/creative-gated button, and a conditional text label. The HUD demo exercises `all`, `any`, `not`, numeric comparisons and list `empty`. Try changing experience level and crouching, then open F8 to inspect the widgets. Inactive widgets remain in the editor's Widgets list.

For registering data sources, see [Typed Data Binding](DATA_BINDINGS.md).
