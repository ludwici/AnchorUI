# Modpack authoring without Java

Anchor UI is designed to let modpack creators configure RPG HUDs and simple dialogue screens using JSON. You only need the Anchor UI mod and an enabled Minecraft **1.21.1** resource pack. There is no requirement to write or compile a separate Java mod.

## Install the example pack

Copy [AnchorUI-Starter](../examples/resourcepack/AnchorUI-Starter) into the `resourcepacks` folder of your Minecraft instance. The pack includes `pack.mcmeta` with `pack_format: 34` for Minecraft 1.21.1. Enable it in **Options → Resource Packs** and join a world.

The included HUD automatically displays live health and hunger. Test the example dialogue using:

```text
/anchorui screen open mypack:traveler
```

To reload changes, use **F3+T**; use **F8** to adjust your HUD position.

## File structure

```text
resourcepacks/
└── AnchorUI-Starter/
    ├── pack.mcmeta
    └── assets/
        └── mypack/
            └── anchorui/
                ├── hud/
                │   └── status.json
                └── screens/
                    ├── traveler.json
                    └── traveler_next.json
```

Use `assets/<namespace>/anchorui/` for **all** Anchor UI JSON data. The old `assets/<namespace>/lore/` path is no longer read. There is currently **no need** to create a separate `anchorui/` folder in your game root. The root-level directory is reserved for future optional direct-file loading, but this feature is not implemented.

As before, widget IDs and references use namespaced identifiers; `id: "health"` within `assets/mypack/anchorui/hud/status.json` resolves to `mypack:health`.

## Live RPG HUD widgets

The new built-in `anchorui:progress` widget displays a live value and maximum:

```json
{
  "type": "anchorui:progress",
  "id": "mypack:health",
  "value": {"bind": "anchorui:player.health", "fallback": 0},
  "max": {"bind": "anchorui:player.max_health", "fallback": 20},
  "fillColor": "#C94454",
  "showText": true,
  "position": {
    "point": "BOTTOMLEFT",
    "anchor": "BOTTOMLEFT",
    "relativeTo": "screen",
    "x": 18,
    "y": -50
  },
  "size": {"width": 164, "height": 16},
  "background": {"color": "BB17171B"}
}
```

`value` and `max` each accept either a literal number or an object with `bind` and optional numeric `fallback`. Registered built-in numeric sources are:

| Binding ID | Value |
| --- | --- |
| `anchorui:player.health` | Current player health |
| `anchorui:player.max_health` | Player maximum health |
| `anchorui:player.food` | Food level (typically 0–20) |
| `anchorui:player.experience` | Experience progress (0.0–1.0) |
| `anchorui:player.level` | Experience level |

The bar updates during rendering without recreating the widget or recalculating its anchor layout. Set `showText: true` to display the numeric current and maximum values over the bar. The `fillColor` accepts `#RRGGBB` and `#AARRGGBB`.

The same `anchorui:progress` widget can be placed in a normal `Screen` JSON document.

## Screen navigation and simple dialogue

A JSON screen can be stored at `assets/mypack/anchorui/screens/traveler.json` and opened by the client command `/anchorui screen open mypack:traveler`.

Use `anchorui:button` to create an interactive button in a normal screen:

```json
{
  "type": "anchorui:button",
  "id": "mypack:continue",
  "text": "Continue",
  "position": {
    "point": "CENTER",
    "anchor": "CENTER",
    "relativeTo": "screen",
    "x": 0,
    "y": 40
  },
  "size": {"width": 120, "height": 20},
  "onClick": {
    "type": "anchorui:open_screen",
    "screen": "mypack:traveler_next"
  }
}
```

Use `{"type": "anchorui:close_screen"}` to return to the previous screen. Invalid action types or unknown target screens are rejected or reported without running arbitrary commands.

**Important:** ordinary in-game HUD widgets stay non-interactive. Buttons are intended for `Screen` interfaces (such as dialogues). This milestone provides **client-side navigation only**; it does not create quests, grant items, execute commands or change server state. Actual quest actions and rewards will require a server-authorized API.

## Extending data and actions

A Java mod can provide additional numeric values by registering them in `NumericDataRegistry`, making its client data available to resource-pack authors. See [Custom Widgets and Data API](CUSTOM_WIDGETS.md) for examples. Screen actions similarly use an explicit registry, with only pre-registered action types accepted.

For the next milestones, Anchor UI will add conditional visibility and dialogue state, then connect server-authorized quest data and actions. Character/skill-tree screens and fully custom game systems are deferred.
