# AnchorUI: contributor and integration instructions

Applies to both Minecraft 1.21.1 branches: `1.21.1` (NeoForge) and `1.21.1-fabric` (Fabric). This is a development API, not a released ABI guarantee.

## Before writing UI code

- For AnchorUI itself **or any consuming Minecraft mod**, first read [Integration reference](docs/INTEGRATION_GUIDE.md), then the linked feature reference for the requested work. Select the actual target loader branch, not automatically the default branch.
- Check existing built-in widgets, JSON defaults, styles, actions, bindings, formatters, conditions, templates, layouts and animation tracks before creating a Java implementation. Prefer JSON and registered extensions over bespoke screen/render/tick code. Inspect the relevant source and tests if documentation disagrees or a feature is absent.
- Do not emit fields whose values are already defaults, unless an explicit value overrides a style/template/inherited setting or is necessary for clarity. Never add internal IDs such as `__content`.
- For a third-party mod, keep network/gameplay authority in that mod's server code. Register client UI extensions once, use namespaced IDs and typed codecs, and publish existing client state via data providers instead of duplicating it. Do not use client UI conditions as authorization.
- Preserve common JSON and Java API semantics across Fabric and NeoForge. Keep loader-specific initialization, mappings and rendering differences explicit. Do not port unrelated changes blindly.
- Follow [Java code style](docs/JAVA_STYLE.md). Update the feature reference and integration index when introducing or changing an API or default. For code changes run the relevant CI on each changed loader branch; inspect failures, fix and verify successful build/tests and JAR artifacts. Documentation-only commits intentionally do not trigger builds.

The repository's documentation is the persistent reference. When integrating AnchorUI from a different repository or conversation, explicitly reopen this file and the matching branch's integration guide; these instructions are not automatically loaded into unrelated projects.
