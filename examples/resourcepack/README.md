# Anchor UI Starter Resource Pack

A simple, no-Java example for **Minecraft 1.21.1 NeoForge** with Anchor UI installed.

## Installation

1. Copy the entire `AnchorUI-Starter` folder into `.minecraft/resourcepacks/` (or the `resourcepacks` folder of your launcher profile).
2. Enable **Anchor UI Starter** in Options → Resource Packs. This requires no new Java mod.
3. Enter a world. A small HUD with live health and food progress bars should appear automatically.
4. Run `/anchorui screen open mypack:screens/traveler` to try a two-screen dialogue with JSON buttons. This is a client command, not a server command.
5. Press **F3+T** after changing the JSON. For HUD layout editing, press **F8**.

The pack uses `pack_format: 34` for Minecraft 1.21.1 and files under `assets/mypack/anchorui/`.

## What the example provides

- `hud/status.json`: two `anchorui:progress` widgets binding to `anchorui:player.health`, `anchorui:player.max_health`, and `anchorui:player.food`.
- `screens/traveler.json`: a dialogue screen using `anchorui:text` and `anchorui:button` with `anchorui:open_screen` and `anchorui:close_screen` actions.
- `screens/traveler_next.json`: the next dialogue page, with a button returning to the previous screen.

There is no client-side arbitrary command execution or server-side quest logic in this starter pack. A server-controlled quests API will be developed separately.
