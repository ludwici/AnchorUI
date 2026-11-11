# Vanilla Minecraft HUD anchors (NeoForge 1.21.1)

Anchor UI exposes **25 read-only anchor IDs** matching the official [NeoForge 1.21.1 `VanillaGuiLayers` identifiers](https://github.com/neoforged/NeoForge/blob/1.21.1/src/main/java/net/neoforged/neoforge/client/gui/VanillaGuiLayers.java). These references are **not editable widgets** and do not modify, intercept or replace Minecraft's HUD rendering.

Use them in any Anchor UI HUD widget exactly like any other `position.relativeTo`. All references exist in the layout graph even when the corresponding vanilla layer is not currently visible (for example, armor when unarmored, or the boss bar outside a boss fight). For a regular resourcepack HUD, no external API or registration is required.

## IDs

| Read-only anchor ID | Vanilla layer | Default logical area |
| --- | --- | --- |
| `minecraft:camera_overlays` | Vignette, fire/pumpkin/spyglass overlays | Full viewport |
| `minecraft:crosshair` | Crosshair | 16×16 around screen center |
| `minecraft:hotbar` | Main hotbar | 182×22, bottom center |
| `minecraft:jump_meter` | Mount jump charge | 182×5, above hotbar |
| `minecraft:experience_bar` | Experience bar | 182×5, above hotbar |
| `minecraft:player_health` | Player hearts | Left of center, expands for multiple heart rows |
| `minecraft:armor_level` | Armor icons | Above health area |
| `minecraft:food_level` | Hunger icons | Right of center |
| `minecraft:vehicle_health` | Mount health | Right of center |
| `minecraft:air_level` | Breath bubbles | Above hunger |
| `minecraft:selected_item_name` | Selected item name | Bottom center, above bars |
| `minecraft:spectator_tooltip` | Spectator toolbar text | Bottom center |
| `minecraft:experience_level` | Green XP level number | Bottom center, above hotbar |
| `minecraft:effects` | Status-effect icons | Upper right |
| `minecraft:boss_overlay` | Boss health bars | Top center |
| `minecraft:sleep_overlay` | Sleep fade | Full viewport |
| `minecraft:demo_overlay` | Demo notice | Upper right |
| `minecraft:debug_overlay` | F3 information | Whole viewport reference |
| `minecraft:scoreboard_sidebar` | Scoreboard sidebar | Right-hand column |
| `minecraft:overlay_message` | Action-bar message | Lower center |
| `minecraft:title` | Title and subtitle | Center |
| `minecraft:chat` | Chat messages | Bottom left |
| `minecraft:tab_list` | Player list (Tab) | Top center |
| `minecraft:subtitle_overlay` | Sound subtitles | Bottom right |
| `minecraft:saving_indicator` | World-saving indicator | Lower right |

All IDs are reserved, so a mod cannot declare an Anchor UI widget using the exact same ID. Other `minecraft:...` widget IDs are not automatically reserved.

## Example

```json
{
  "entities": [
    {
      "type": "anchorui:text",
      "id": "mypack:above_hotbar",
      "text": "Aligned with the vanilla hotbar",
      "position": {
        "relativeTo": "minecraft:hotbar",
        "point": "BOTTOM",
        "anchor": "TOP",
        "x": 0,
        "y": -5
      },
      "size": {"width": 150, "height": 16}
    }
  ]
}
```

The custom widget follows the vanilla hotbar when the GUI scale or viewport dimensions change. Attaching to an inactive vanilla layer still uses its **last-known logical placement computed from the current screen geometry**, rather than silently falling back to `screen`.

## F8 editor

Open F8 while in the game world. Vanilla reference rectangles use **purple**, distinct from the editor's cyan custom widgets, and can be inspected as read-only reference areas. The top-right `IDs` control cycles among:

- **IDs ON** (default): show references estimated active in the current game state.
- **IDs ALL**: also show inactive vanilla reference rectangles.
- **IDs OFF**: hide reference rectangles; existing custom anchors continue working.

Select a **custom** widget, press **Pick parent** and choose a vanilla reference box. For temporary HUD elements that are not currently visible, you can instead scroll the inspector's **VANILLA ANCHORS** list and click any of the 25 IDs. This also supports full-screen overlay IDs that are deliberately excluded from direct mouse picking.

Vanilla references are not draggable. The editor does not expose hide/show, reset, scale or anchor changes for them. Saved `hud-layout.json` contains overrides for custom widgets only.

## Accuracy and lifecycle

The ID list is authoritative for NeoForge 1.21.1. Its rectangle geometries are **logical anchor approximations**, not instrumented render bounds: the vanilla renderer does not publish the precise pixel bounding box of each layer. The hotbar, crosshair, standard experience bar and ordinary survival status rows use known vanilla GUI dimensions; dynamic text, chat width, camera effects, effect rows, scoreboard heights and boss bar counts can differ from the logical rectangles shown here.

The editor's active/inactive indication is best effort. Conditional transient layers (such as titles, a fading selected-item label, subtitles or the saving icon) may appear inactive even when Minecraft temporarily draws them. Their ID and anchor frame remain available via the complete inspector list.

Anchor UI updates its reference geometry during the existing client HUD render, and re-resolves custom anchor positions only if geometry or status changes. **No server tick, vanilla screen patch or vanilla HUD replacement is added.**

## Test

Enable the bundled standalone test HUD:

```text
/anchorui test vanilla on
```

Open F8, move the window/UI scale, compare the test text above the hotbar and near the crosshair, and switch the `IDs` visibility filter. Then disable the demo:

```text
/anchorui test vanilla off
```
