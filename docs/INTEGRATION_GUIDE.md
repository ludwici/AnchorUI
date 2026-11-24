# AnchorUI integration reference — Minecraft 1.21.1

Start here when authoring an interface in another mod (for example an RPG HUD, phone, profile, settings or quest screen). This is an index of existing capabilities, not a new API. The implementation and tests in the target branch are authoritative. The development API has not yet been released as a stable ABI.

## 1. Choose the branch and dependency

| Loader | Branch | Client registration |
| --- | --- | --- |
| NeoForge | [1.21.1](https://github.com/ludwici/AnchorUI/tree/1.21.1) | `FMLClientSetupEvent.enqueueWork` |
| Fabric | [1.21.1-fabric](https://github.com/ludwici/AnchorUI/tree/1.21.1-fabric) | `ClientModInitializer.onInitializeClient` |

Both use Java 21, Mojang mappings in AnchorUI source, mod ID `anchorui`, and the same core JSON paths. A Fabric consumer using Yarn needs Loom's correctly remapped mod dependency, not copied Mojang-mapped Minecraft classes or an unremapped development JAR. Use the production artifact for the selected loader; inspect that branch's Gradle configuration and metadata for exact versions. Declare AnchorUI as a client dependency where required, and keep client-only types out of dedicated-server initialization. No published Maven coordinates are assumed here.

For registration signatures and lifecycle, read [Java extension API](JAVA_EXTENSION_API.md). For custom widgets and codecs, read [Custom widgets](CUSTOM_WIDGETS.md).

## 2. Reuse before implementing

| Need | Existing mechanism | Reference |
| --- | --- | --- |
| Text, rich formatting, wrapping, dynamic text | `anchorui:text`, text bindings and templates | [Data bindings](DATA_BINDINGS.md), [README text widgets](../README.md#text-widgets) |
| Clickable icon or labeled button | `anchorui:button`, optional label, textures and action | [UI controls](UI_CONTROLS.md) |
| Checkbox, toggle, selection, tab navigation, color | `anchorui:checkbox`, `switch`, `select`, `tabs`, `color_picker` | [UI controls](UI_CONTROLS.md) |
| Images, textures, rounded panels, nine-slice | `anchorui:image`, `panel` (also `container`), visual styles | [Visual widgets](VISUAL_WIDGETS.md) |
| Row, column, flex sizing, grid, padding, clipping, scrolling | Panel `children`, `layout`, `autoSize`, `scroll` | [Visual widgets](VISUAL_WIDGETS.md) |
| Health/XP/custom progress bars | `anchorui:progress`, bindings, skins, `sizeTransition` | [Visual widgets](VISUAL_WIDGETS.md), [Animations](ANIMATIONS.md) |
| Repeated data-driven rows/cards | `anchorui:template_list`, named templates, item context, registered layouts; `data_list` for simple data | [Template lists](TEMPLATE_LIST.md) |
| Item model / item lore | `anchorui:model`, `anchorui:list`; model renders an item, not an arbitrary player entity | [Custom widgets](CUSTOM_WIDGETS.md) |
| Show/hide, enable/disable, alternative components | `visibleWhen`, `enabledWhen`, `anchorui:cases`, action guards | [Conditions](CONDITIONS.md) |
| Switch screens / local tabs / expanded panels | `anchorui:open_screen`, `close_screen`, `set_state`, `toggle_state`, screen `state` and optional `stateHandoff` | [UI state](UI_STATE.md), [UI events](UI_EVENTS.md) |
| Reusable styles and components | `style`, `ref`, `imports`, widget/template definitions | [Widget references](WIDGET_REFERENCES.md), [README](../README.md) |
| Relative layout / stretching between elements | `position`, `position.anchors` | [Multiple anchors](MULTI_ANCHORS.md) |
| Animations, delays, custom triggers, smooth size changes | `animations` array, `sizeTransition`, trigger registry | [Animations](ANIMATIONS.md) |
| Client-side data and formatting | `DataValueRegistry`, `DataFormatterRegistry` | [Data bindings](DATA_BINDINGS.md), [Formatters](FORMATTERS.md) |
| HUD and F8 editor | existing HUD manager, screen editor, local overrides | [HUD](HUD.md), [Screen editor](SCREEN_EDITOR.md) |

Widget IDs above refer to registered types; `anchorui:cases` is a declarative resolver construct, not a factory to register. Do not implement a second layout engine, scrollbar, screen-navigation system, polling loop or JSON parser just to reproduce these features.

## 3. JSON defaults and short forms

| Field / context | Omitted value or behavior |
| --- | --- |
| Button `text` | Empty string; icon-only buttons need no label |
| Text object binding `fallback` | Empty string for text widgets |
| Other binding `fallback` | The consuming property's typed default, not universally zero; progress `value` defaults to 0, `max` to 100 |
| Text template expression | Its implicit missing-value marker is `?`, distinct from an object-valued text binding |
| Position `x`, `y` | Each independently defaults to 0; positive X right, positive Y down |
| Single position `point`, `anchor` | Missing side takes the other side; both absent means TOPLEFT |
| `relativeTo` | Screen for a standalone widget; nested children use the parent's content area when no explicit target is supplied |
| Text `shadow` / `wrap` / `align` | false / false / left |
| Widget `hidden` | false |
| Screen root `scale` | 1 |
| Animation `duration` / `delay` | 300 ms / 0 ms |
| Panel `scroll` | Off unless enabled |

Defaults apply after resolving applicable definitions/styles. An omitted overlay property can inherit a non-default value; do not delete such an explicit override just because it equals the parser default. In particular style/template position merging can preserve the original anchor. An explicitly provided invalid binding is still an error; omitting fallback does not register an unknown data source.

Minimal centered icon button (place in a screen's `entities`):
```json
{
  "type": "anchorui:button",
  "id": "close",
  "position": {
    "point": "CENTER"
  },
  "size": {
    "width": 24,
    "height": 24
  },
  "textures": {
    "background": {
      "normal": "mymod:textures/gui/close.png"
    }
  },
  "onClick": {
    "type": "anchorui:close_screen"
  }
}
```

A panel is already a parent. A Grid places every structural child into a cell; do not supply redundant child position fields or internal `__content` IDs. Row/column layouts allow opt-outs and local offsets as documented. Use `position.anchors` instead of single-anchor fields for stretch constraints; never mix both syntaxes in one position object.

## 4. Resource layout and opening screens

Put JSON in the consuming mod's client resources (or a resource pack):
```text
assets/mymod/anchorui/
  screens/phone.json
  hud/status.json
  style/phone.json
  widgets/app_icon.json
  templates/app_row.json
assets/mymod/textures/gui/
```

Use the public screen ID `mymod:phone` in commands and JSON navigation. Widget instance IDs are different from factory type IDs; short widget IDs resolve in the resource namespace. Refer to [Widget references](WIDGET_REFERENCES.md) for cross-mod references, reusable definitions and overrides. No old `lore/` path or extra game-root JSON loader is needed. The editor's user-created HUDs/screens and overrides are managed separately by AnchorUI.

Test with `/anchorui screen open mymod:phone`, `/anchorui hud list`, F8 and F3+T. To open a resource screen from Java without issuing a command, reuse the same path as the built-in client command (run on the Minecraft client thread):

```java
ResourceLocation id = ScreenIds.resolve("mymod:phone");
LoreEntity entity = LoreLoader.INSTANCE.getData().get(id);
if (entity != null) {
    Minecraft minecraft = Minecraft.getInstance();
    minecraft.setScreen(new ItemLoreScreen(minecraft.screen, ItemStack.EMPTY, entity, id));
}
```

Mojang-mapped imports: `com.ludwici.anchorui.client.ScreenIds`, `com.ludwici.anchorui.client.LoreLoader`, `com.ludwici.anchorui.api.LoreEntity`, `com.ludwici.anchorui.client.gui.ItemLoreScreen`, and Minecraft's `Minecraft`, `ResourceLocation`, `ItemStack`. Adapt the Minecraft imports to the consumer's mappings. The caller owns authorization and missing-screen handling. The internal resolved ID includes `screens/`; the public command/action ID does not need it.

## 5. Java only for missing capabilities or actual mod state

Register once at client startup; do not repeat registrations on opening a screen, joining a world, or reloading resources.

- Already available data sources (prefix `anchorui:`): `player.health`, `player.max_health`, `player.food`, `player.experience`, `player.level`, `player.name`, `player.alive`, `player.sneaking`, `player.creative`, `world.dimension`, `player.profile`, `player.effects`, `ui.state`. Do not register substitutes for these just to read the same values.
- Existing formatters (prefix `anchorui:`): `integer`, `percent`, `compact`, `uppercase`. Use them before adding another formatter.
- Register mod-specific state with `DataValueRegistry.registerNumber/String/Boolean/Object/List`. For packet-driven state use `registerMutable` and update the snapshot from the existing client packet callback; for expensive calculations use `registerRevisioned`. No extra polling thread is needed.
- Register domain actions via `UiActionRegistry.register` or `registerEvent` with a codec. Keep packet sending and server authorization in the integrating mod. JSON only selects a registered action and supplies validated arguments.
- Register missing visual primitives with `AnchorWidgetRegistry.register(id, codec, factory)`. Return an `AbstractWidget`; the framework handles placement. Register F8 properties separately with `WidgetEditorRegistry.register`. Use `WidgetTemplateRegistry` for reusable Java-defined JSON, not custom rendering for a component already expressible declaratively.
- Ordinary HUDs are display-only; `WidgetContext.screen()` can be null. UI local state is presentation state, not persistent gameplay state. Do not silently move server-side validation to a resource pack.

See [Java extension API](JAVA_EXTENSION_API.md) for the full extension table and examples.

## 6. Loader differences and validation

Both branches provide the core system and share the loader-independent scrollbar. Fabric additionally has `scaleFitScreen` / `scaleFitMargin` responsive scaling as described in its [Visual widgets](VISUAL_WIDGETS.md) page; verify before relying on it on NeoForge. Text shadow defaults to false on both; text clipping implementation can differ with loader rendering hooks. Do not assume every newer change in one branch already exists in the other.

The HUD uses NeoForge's GUI layer event or Fabric's HUD render callback; do not assume identical overlay ordering. API is development-stage: inspect the target branch's actual registries, tests and build metadata when implementing a new integration. Useful source anchors: `Events` (built-in widgets), `WidgetTypeIds`, `MinecraftDataSources`, `ClientUiActions`, `PositionElement`, `LoreStyle.Element`, `DataBindingSpec`, `OptionalJsonDefaultsTest`.

For integration work, compile against the target loader and test the actual screen in Minecraft. The framework's unit tests cannot validate the consumer's visual layout, packet authorization or runtime compatibility. For AnchorUI changes verify CI on affected branches and the generated JAR. See [Test scenes](TEST_SCENES.md), [Resource reload](RESOURCE_RELOAD.md) and [Performance](PERFORMANCE.md).
