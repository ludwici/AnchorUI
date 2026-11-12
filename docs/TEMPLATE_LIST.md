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
| template | Single widget JSON template, optionally with nested children |
| key | Path to unique string/number within every entry; recommended for live updates |
| rowHeight | Height/placement step of a row; default 22 |
| gap | Vertical spacing; default 2 |
| maxEntries | Maximum generated rows; default 64 and hard limit 256 |
| size, padding, scroll, styles | Standard panel/container options |

Rows are positioned vertically relative to the list's content area, with step rowHeight + gap. If the root template does not specify size.height, it defaults to rowHeight. Duplicate keys and invalid configuration cause a descriptive error on screen initialization. If **key** is absent, the row index is used.

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

The screen contains quest buttons and a **Reorder / add quest** button that updates the registered mutable data source. Clicking a generated button prints the selected quest in chat; the dragon quest is disabled by a row condition. Add, remove and reorder rows without closing the screen. Ordinary HUD overlays remain noninteractive; the interactive template-list implementation currently targets **screens**.
