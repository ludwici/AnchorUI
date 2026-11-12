# Template List and Item Context (NeoForge 1.21.1)

AnchorUI generates interactive widgets from list-valued Data Bindings. A resource pack describes one row template, and AnchorUI creates ordinary widgets for every entry. Java mods register the data source and event handlers as usual.

## Java data source

    DataValueRegistry.registerList(
        ResourceLocation.fromNamespaceAndPath("mymod", "quests.available"),
        context -> getAvailableQuestsAsJsonArray()
    );

The method shown is supplied by your mod. It should return an array such as:

    [
      {"id": "mymod:find_sword", "title": "Find the lost sword", "level": 1},
      {"id": "mymod:kill_dragon", "title": "Defeat the dragon", "level": 10}
    ]

## JSON example

    {
      "type": "anchorui:template_list",
      "id": "mymod:quest_list",
      "items": {
        "bind": "mymod:quests.available",
        "fallback": []
      },
      "key": "id",
      "rowHeight": 26,
      "gap": 4,
      "maxEntries": 64,
      "scroll": {
        "axis": "vertical",
        "step": 26
      },
      "size": {
        "width": 220,
        "height": 120
      },
      "template": {
        "type": "anchorui:button",
        "text": {
          "item": "title"
        },
        "enabledWhen": {
          "left": {
            "item": "level"
          },
          "op": "lte",
          "right": 10
        },
        "onClick": {
          "type": "mymod:accept_quest",
          "quest": {
            "item": "id"
          }
        },
        "size": {
          "width": 200,
          "height": 24
        }
      }
    }

Each row becomes a regular widget. The two buttons share one registered Java action but have their own text and quest ID. The condition also receives the current row's level.

## Referencing a template by ID

Instead of repeating a row widget object inside each list, set `template` to a **namespaced template ID**:

```json
{
  "type": "anchorui:template_list",
  "id": "mymod:quests",
  "items": {
    "bind": "mymod:quests.available",
    "fallback": []
  },
  "key": "id",
  "template": "mymod:quest_row",
  "size": {
    "width": 220,
    "height": 120
  }
}
```

There are two ways to provide the referenced `mymod:quest_row` definition.

### Resource-pack file

Create **`assets/mymod/anchorui/templates/quest_row.json`**:

```json
{
  "type": "anchorui:button",
  "text": {
    "item": "title"
  },
  "onClick": {
    "type": "mymod:accept_quest",
    "quest": {
      "item": "id"
    }
  },
  "size": {
    "width": 200,
    "height": 24
  }
}
```

The template can be shared between screens and between mods. Relative IDs in nested child definitions are resolved using the definition namespace; the current list still gives every generated row a unique instance ID. A template can contain nested `children`, inherit another definition using `ref`, and use data from the current row in text, Conditions, and action arguments. Item substitutions work **inside the external definition**, including nested children and inherited fields.

The pre-existing `assets/<namespace>/anchorui/widgets/<name>.json` definitions are also available by ID as templates. The dedicated `templates/` directory is recommended for list row templates. If `widgets/` and `templates/` declare the same ID, `templates/` wins with a warning.

### Java registration

Other mods can register a named template directly from Java using `WidgetTemplateRegistry`, without supplying a JSON template file:

```java
ResourceLocation id = ResourceLocation.fromNamespaceAndPath("mymod", "quest_row");
JsonObject definition = new JsonObject();
definition.addProperty("type", "anchorui:button");

JsonObject title = new JsonObject();
title.addProperty("item", "title");
definition.add("text", title);

JsonObject action = new JsonObject();
action.addProperty("type", "mymod:accept_quest");
JsonObject quest = new JsonObject();
quest.addProperty("item", "id");
action.add("quest", quest);
definition.add("onClick", action);

WidgetTemplateRegistry.register(id, definition);
```

Register on the client during `FMLClientSetupEvent.enqueueWork` before the screen is opened. AnchorUI takes a copy of the supplied `JsonObject`, so subsequent changes to your original object cannot mutate the registered definition.

For Java code that builds the definition on demand, use `WidgetTemplateRegistry.register(id, () -> createQuestRowTemplate())`. Your method must return a `JsonObject` containing `type` or `ref`. The factory is invoked when a list expands or rebuilds, not every render frame. The Java provider must be installed for the template to be available; JSON cannot execute arbitrary Java.

**Resource-pack definitions take precedence** over Java registrations with the same ID, allowing packs to replace a Java-provided default. Re-registering a Java template with the same ID is rejected.

### Compatibility

All three forms are supported:

```json
{
  "template": "mymod:quest_row"
}
```

```json
{
  "template": {
    "ref": "mymod:quest_row"
  }
}
```

```json
{
  "template": {
    "type": "anchorui:button",
    "text": "Inline row"
  }
}
```

A missing template ID is reported as an error when building the screen. As before, a template must ultimately resolve to a registered widget type, and the existing per-widget error handling applies to invalid widget configuration.

## Grid layout and Java-defined layouts

By default `template_list` keeps its existing vertical row layout. To create a grid, set `layout.type` to `grid` (or `anchorui:grid`):

```json
{
  "type": "anchorui:template_list",
  "id": "mymod:skills",
  "items": {
    "bind": "mymod:skills.available",
    "fallback": []
  },
  "key": "id",
  "template": "mymod:skill_card",
  "rowHeight": 48,
  "layout": {
    "type": "grid",
    "columns": 4,
    "columnGap": 6,
    "rowGap": 6
  },
  "padding": {
    "left": 6,
    "right": 6
  },
  "scroll": {
    "axis": "vertical",
    "step": 54
  },
  "size": {
    "width": 240,
    "height": 180
  }
}
```

The grid fills rows from left to right and wraps after `columns` entries. The **cell width** is derived from `size.width`, left/right padding, gaps and the column count. The **cell height** defaults to `rowHeight` (22 when omitted). A grid cell overrides the generated root widget's `size.width` and `size.height`; nested child widgets retain their own sizes. The root widget's optional `position.x`/`position.y` offsets are added to the grid position. Scroll and clipping work like a vertical template list.

For fixed-size cells or a grid without an explicit container width, set `layout.cellWidth` (1–16384) and optionally `layout.cellHeight` (1–4096). Both are measured in GUI pixels. `columns` must be 1–256, and `columnGap` and `rowGap` must be 0–4096. `layout` may also be `"grid"` (defaults to one column), or `"vertical"`, but the object form is more useful for configuration.

The old `rowHeight` and `gap` properties still work for vertical lists, and a list without `layout` preserves existing root widget dimensions. For a grid, `rowHeight` and `gap` provide fallback cell height and gap values.

### Custom layout algorithms from Java

A client-side mod can register a layout under any namespaced ID. Its factory receives the options and list metrics; its layout function returns an `x`/`y` coordinate and cell size for each index:

```java
TemplateListLayoutRegistry.register(
    ResourceLocation.fromNamespaceAndPath("mymod", "diagonal"),
    (options, metrics) -> {
        int step = options.get("step").getAsInt();
        return index -> new TemplateListLayoutRegistry.Cell(index * step, index * step, 42, 24);
    }
);
```

```json
{
  "layout": {
    "type": "mymod:diagonal",
    "step": 12
  }
}
```

For **typed configuration**, register `TemplateListLayoutRegistry.register(id, Codec<T>, (config, metrics) -> layout)`. Use a record and `Codec<T>` to specify required options, defaults and validation. `metrics.rowHeight()`, `metrics.gap()` and `metrics.availableWidth()` describe the list settings. The framework creates the layout once when generating list rows, not per frame. It executes your registered Java code only; the JSON itself cannot run scripts. Returning a cell with width 0 preserves the template's width.

### Try the grid demo

```text
/anchorui screen open anchorui:template_grid_demo
```

This demo uses the same reusable file-backed quest template and mutable data source as the normal list, but arranges quest buttons in three columns. Click **Reorder / add quest** to populate and reorder the grid without leaving the screen.

## Item references inside the template

A JSON object with only the **item** path substitutes that field as a value of its original type (string, number, boolean, object, or list). Optional **fallback** supplies a replacement for a missing path.

    {"item": "title"}
    {"item": "stats.level", "fallback": 0}
    {"item": "rewards[0].name"}

A string can interpolate the current entry with expressions like:

    "Quest: {{item#title}}"
    "Selected: {{item}}"

The fixed text remains unchanged; dynamically inserted markup brackets are escaped. These substitutions work in text, nested action arguments, Conditions, and other widget JSON fields. Ordinary global Data Bindings remain available.

The **template** may be a panel/container with nested **children**, or it can reference another mod's widget definition through **ref**. Expanded widgets use the existing registry, anchors, layout, input and event systems.

## Template List settings

| Property | Meaning |
| --- | --- |
| items | A list Data Binding or literal JSON array |
| template | Inline widget JSON object or named template ID (resource pack or Java) |
| key | Path to unique string/number within every entry; recommended for live updates |
| rowHeight | Vertical row height or grid cell-height default; default 22 |
| gap | Vertical row spacing or default grid gap; default 2 |
| maxEntries | Maximum generated rows; default 64 and hard limit 256 |\n| layout | Optional vertical/grid layout or registered Java layout by ID; default vertical |
| size, padding, scroll, styles | Standard panel/container options |

Without a custom layout, rows are positioned vertically relative to the list's content area, with step rowHeight + gap. If the root template does not specify size.height, it defaults to rowHeight. Duplicate keys and invalid configuration cause a descriptive error on screen initialization. If **key** is absent, the row index is used.

The list source is checked for changes during screen rendering. Only when the array changes is the screen's generated widget tree rebuilt; scroll offsets are clamped and preserved, and focus is restored by ID where possible. Keys give stable widget **IDs**, but a changed list still recreates widgets. Custom widgets holding local internal state should store important state outside themselves if it must survive data updates. Rebuilding the list does not repeat the screen's onOpen event.

## Java Item Context and event payloads

Any widget factory or event handler inside a repeated template can read:

    ItemContext item = context.itemContext();
    if (item != null) {
        String questId = item.get("id").getAsString();
        int index = item.index();
        String stableKey = item.key();
    }

For generated row buttons, UiEvent.value() contains the full selected row object on click and hover. UiEvent.item() contains the row in **all** event types, including onChange. For onChange, UiEvent.value() remains the actual changed value (such as scroll offsets). Outside repeated rows, UiEvent.item() is JSON null. All returned JSON values are defensively copied.

An event-aware Java action can use the selected row without explicit JSON arguments:

    UiActionRegistry.registerEvent(
        ResourceLocation.fromNamespaceAndPath("mymod", "accept_quest"),
        (context, settings) -> event -> {
            if ("onClick".equals(event.name()) && event.item().isJsonObject()) {
                String questId = event.item().getAsJsonObject().get("id").getAsString();
                MyQuestNetworking.requestAccept(questId);
            }
        }
    );

The networking helper is an example you implement in your mod. All quest updates, rewards and eligibility must be checked **on the server**; client-side UI events do not authorize gameplay changes.

## In-game demo

    /anchorui screen open anchorui:template_list_demo

The first screen uses a file-backed template ID and the second uses a Java-registered template ID. Both contain quest buttons and a **Reorder / add quest** button that updates the registered mutable data source. Clicking a generated button prints the selected quest in chat; the dragon quest is disabled by a row condition. Add, remove and reorder rows without closing the screen. Ordinary HUD overlays remain noninteractive; the interactive template-list implementation currently targets **screens**.
