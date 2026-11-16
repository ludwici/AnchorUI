# Resource reload and diagnostics (NeoForge 1.21.1)

AnchorUI reads client resource-pack JSON under `assets/<namespace>/anchorui/`. A completed reload replaces the old screen, HUD, style and reusable-widget registries with **one compiled snapshot**. No partial registry is published while the reload compiler is still validating documents.

## Resource types

| Resource path | Loaded as | Error handling |
| --- | --- | --- |
| `anchorui/style/*.json` | Shared widget and option styles | A non-object JSON root or invalid `anchorui:option` value is rejected for that style only; missing styles fall back to the default during widget creation |
| `anchorui/widgets/*.json` | Reusable widget definitions | A definition requires a nonempty string `type` or a valid `ref`; malformed definitions are skipped |
| `anchorui/templates/*.json` | Reusable templates | Valid templates replace a matching `widgets/` definition by ID; invalid templates do **not** hide valid `widgets/` definitions |
| `anchorui/hud/*.json` | Shared HUD fragments | One broken HUD file is rejected without preventing other HUD documents from being loaded |
| Other `anchorui/*.json` documents | JSON screens / test scenes | One invalid document is rejected without preventing unrelated screens from loading |

Every error includes the affected resource ID and a reason in `latest.log`. Missing optional imports and widget references are **warnings**: only those missing widgets are skipped. Invalid required fields, circular template inheritance and malformed structures can reject their containing document.

JSON syntax parsing happens **before** the AnchorUI resource compiler, in Minecraft's resource loader. These isolation guarantees cover the resource entries delivered to AnchorUI, not arbitrary syntax errors that stop Minecraft from completing a resource reload.

## Diagnostics in the F8 editors

Both F8 inspectors include a collapsed **DIAGNOSTICS** category when issues are present. It combines widget/layout errors with resource-load problems:

- The screen editor shows issues for its current resource plus shared styles, templates and reusable widget definitions.
- The HUD editor shows HUD resource issues plus shared styles, templates and reusable widget definitions.
- Messages identify the file ID; errors and warnings have different labels. Duplicates are hidden, and the first 24 items are shown with a count of omitted issues.
- The full error details and exception stack traces remain in `latest.log`.

The `getResourceIssues()` method on `LoreLoader.INSTANCE` exposes immutable issue records with `severity()`, `resource()`, `message()`, and `cause()` for integration code and diagnostics. This is a client-side API.

If an already-open JSON screen's resource disappears or becomes invalid after F3+T, AnchorUI retains its previous screen instance rather than replacing it with a broken one. If corrected resources arrive on a subsequent reload, the screen can adopt the replacement, retaining compatible UI state. The HUD is rebuilt against the new valid set of documents.

A resource pack that is **removed** is not retained in the new global registries. The already-open screen's fallback is deliberately separate from global resource registration.

## Manual verification checklist

1. Open `/anchorui screen open anchorui:visual_demo` and enter F8. Confirm a valid screen draws normally before and after **F3+T**. Verify buttons and panels remain clickable outside the editor.
2. Add a separate test resource pack with a valid `assets/mytest/anchorui/widgets/shared.json` and an intentionally invalid `assets/mytest/anchorui/templates/shared.json` (`{"type":42}`). Reload. The valid widget definition should still resolve and an error should name the invalid template.
3. Put an invalid option style value (`{"anchorui:option":"bad"}`) in another style file. Reload. The style should be skipped and unrelated screens/HUD fragments should still load. F8 should show the resource ID in DIAGNOSTICS.
4. Use an unavailable `ref` in a screen that also has a valid widget. Verify the missing reference produces a warning, while the other widget remains functional.
5. Fix the pack and reload again. The errors should disappear from the current resource snapshot and the correct template should load. Verify repeated F3+T reloads do not duplicate entries.
6. Test with **two active resource packs**, reversing their priority. Confirm Minecraft's chosen JSON files win for identical resource paths and that valid `templates/` entries override matching `widgets/` IDs.
7. Repeat on different **GUI Scales** and window sizes, before and after a reload. Check positions, clipping, scrollbars, editor selection, keyboard focus and Cancel/Save.
8. Enter and leave a multiplayer server. Resource-pack UI evaluation is client-side; any action that changes authoritative gameplay state must still be validated by the integrating mod's server code.

**Automated coverage:** `LoreResourceCompilerTest` tests malformed resource isolation, template precedence, optional missing references and fresh snapshots. `EditorDiagnosticsTest` tests source scoping, severity, deduplication and bounded presentation. The Gradle build does not replace these Minecraft-client visual/manual checks. Profiling is deferred.
