# Stage 1 / Stage 2 — Runtime QA and hidden-space policy

The Stage 1 Conditions / `anchorui:cases` / guarded-action engine and Stage 2 automatic layouts have automated test coverage. The checks below verify the **actual Minecraft client**, not just mathematical layout calculations. Only mark in-client QA complete after carrying out these steps on the current `1.21.1` branch.

## Hidden-child layout policy

1. **Default = reserve**. `visibleWhen`, `cases` alternatives and editor Hide/Show determine visibility but do not by themselves change a widget's slot or the positions of visible siblings.
2. **Opt-in = collapse**. A structural container using automatic `layout.type: row`, `column`, or `grid` can enable `layout.collapseHidden: true`. When a direct child becomes hidden, it is excluded from the flow and from the container's scroll-overflow calculation. The positions of visible siblings, wrapping, Grid cells and any `autoSize` measurements update when visibility changes. Previous scroll offsets are clamped to the new limits.
3. **Manual child behavior**. In Row/Column, a child with `layoutItem: false` or an external/non-TopLeft anchor does not participate in automatic flow. If its parent has `autoSize`, it contributes to the measured bounds only while visible when `collapseHidden` is enabled. Grid always manages direct structural children unless hidden-and-collapsed; manually authored child positions do not replace Grid cells.
4. **Unmanaged/manual container**. A panel without an automatic layout uses its ordinary anchor coordinates and does not magically reflow other widgets when one is hidden. `collapseHidden` is a setting on automatic layouts, not a per-widget globally inherited visibility rule.
5. **Children retain identity**. Hidden widgets are not deleted, so their state, event bindings and F8 selection handles remain available. The feature changes layout measurement and scroll bounds, not UI State persistence.

Example:

```json
{
  "type": "anchorui:panel",
  "id": "mypack:entries",
  "layout": {
    "type": "column",
    "gap": 6,
    "collapseHidden": true
  },
  "scroll": {
    "axis": "vertical",
    "step": 18
  },
  "clipChildren": true,
  "children": [
    {
      "type": "anchorui:text",
      "id": "mypack:first",
      "text": "Always visible"
    },
    {
      "type": "anchorui:text",
      "id": "mypack:optional",
      "text": "Optional row",
      "visibleWhen": {
        "bind": "mypack:show_extra",
        "fallback": false
      }
    }
  ]
}
```

To reserve the second row's space instead, **omit** `collapseHidden` or set it to `false`. All hide sources share the same policy, including visible/hidden `cases` alternatives.

## Minecraft client acceptance checklist

- [ ] **Compare policies and scroll clamping:** `/anchorui screen open anchorui:hidden_space_policy_demo`. Toggle **Hide middle item**. The left **Reserve** panel retains the middle row's slot and its extra scroll range. The right **Collapse** panel moves the last row up and reduces its scroll extent. Scroll both panels before and after toggling to confirm there is no phantom empty scrolling and that previous offsets are clamped.
- [ ] **Responsive Row, Column and Flex:** `/anchorui screen open anchorui:stack_layout_demo`. Toggle middle rows, verify gaps close, the auto-sized column changes height and no unexpected flicker occurs. Open `anchorui:advanced_layout_demo` to check Flex grow/shrink and Row wrapping.
- [ ] **Grid and structural ownership:** In `anchorui:advanced_layout_demo`, toggle the optional Grid item. The remaining cells should reflow, with normal content still inside the actual parent Panel padding. In F8, move a widget into the Grid and back to the screen root; no `__content` or `container` wrapper is required.
- [ ] **F8 Save/Cancel and reset:** With F8 open on either demo, adjust `Layout`, `Gap`, `Wrap`, `Columns`, `Collapse` and a child's sizing limits. Save, reopen and verify the values persist. Change them again and Cancel; verify saved values remain. Use `R` to reset one setting without changing the rest. Confirm inspector scroll does not jump to the top during repeated adjustments.
- [ ] **Resize and GUI scale:** Resize the window and switch between at least two Minecraft GUI Scale settings. Wrapped rows, stretched Grid cells, scrollbars, the Widget list and the inspector should stay reachable, with no missing hitboxes.
- [ ] **Nested scroll and focus:** Scroll an inner panel while an outer screen is open, then click and Tab between enabled controls. The inner scroll should not unexpectedly scroll the editor, leak clicks through clipped areas, or trap the keyboard focus.
- [ ] **F3+T:** Reload resource packs while the demos are open and after returning from F8. Reopen the screen if necessary and check that anchors, visibility conditions, updated resources and saved overrides still produce consistent geometry.
- [ ] **Conditions and actions:** `/anchorui screen open anchorui:conditional_content_demo`. Change tabs and inspect `cases` fallback; verify the guarded button does nothing until its boolean guard becomes true.

### Regression coverage available without a client

`StageOneTwoLayoutIntegrationTest`, `AdvancedContainerLayoutTest`, `StackLayoutTest`, `ContainerScrollTest`, `EditorContainerParentingTest`, `LayoutInspectorControlsTest`, and `ConditionalWidgetCasesTest` cover core geometry, visibility policy, persistence, nested layouts and state-driven content. Gradle test success is **not** a substitute for the Minecraft checks above.

The future ideas of Grid spans, named tracks, advanced responsive layout and incremental layout caching are deliberately **outside the currently agreed general Grid scope**. They are not Stage 1/2 release blockers unless a new use case explicitly requires them.
