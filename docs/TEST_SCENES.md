# Anchor UI — interactive test scenes

The built-in scenes exercise the same JSON → `LoreEntity` → `ItemLoreScreen` → `AnchorLayout` rendering path used by normal item-lore screens.

## Starting

1. Run the NeoForge 1.21.1 client with Anchor UI installed (`./gradlew runClient`).
2. Enter a single-player world or connect to a server. The command is client-side and needs no operator permission or server-side Anchor UI installation.
3. Run `/anchorui test list` to see the available scenes, or `/anchorui test scene1` to open one.
4. Use the `<` and `>` buttons to navigate; use **G** for element outlines, IDs and center guidelines; **Esc** to close.
5. Change the window size or Minecraft GUI Scale while testing to trigger layout recalculation.

All scene descriptions are stored in `src/main/resources/assets/anchorui/lore/test/scenes/` and can be edited as JSON. Resource reload (F3+T) applies changes.

## Expected behaviour

| Command | Scenario | Pass condition |
| --- | --- | --- |
| `/anchorui test scene1` | Nine anchor points | Nine labelled blocks appear at the corresponding corners/edges/center with consistent margins. |
| `/anchorui test scene2` | Parent/child links | Blocks surround `PARENT`; `3. Child of RIGHT` follows `RIGHT` even though it appears before `RIGHT` in JSON. |
| `/anchorui test scene3` | Dimensions, padding, colors | Fixed-size boxes and an auto-sized text element display differing padding, text colors and alpha backgrounds. |
| `/anchorui test scene4` | Model and rich text | Item renders; dragging rotates it. List wraps text; bold, italic, underline and hover tooltip work; a hidden lore entry renders as question marks. |
| `/anchorui test scene5` | Responsive layout | Top and bottom frames follow screen edges, satellites follow center when window dimensions or GUI Scale change. |
| `/anchorui test scene6` | Invisible anchor | Red hidden frame does not render, but the visible child and its dependent frame remain correctly positioned. Toggle G to inspect layout bounds. |
| `/anchorui test scene7` | Missing target | Diagnostic says `Unknown relativeTo frame: missing_target` in a red panel. No client crash. |
| `/anchorui test scene8` | Cyclic references | Diagnostic says `Circular frame anchors` in a red panel. No client crash. |

The scene definitions are **visual test fixtures**. Automated JUnit tests additionally check that their JSON resources exist, positive scenes resolve at two window sizes, and invalid scenes produce expected errors.

A successful CI build cannot prove that a rendered GUI looks correct; perform the in-game checks above to verify visibility, input and interaction.
