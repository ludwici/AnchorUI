# Java source formatting

Within Java method and function bodies, write complete method and constructor
**invocations on one physical line**, including calls with several arguments
and nested calls. Long invocation lines are intentional.

For example:

```java
WidgetAnimation animation = WidgetAnimation.parse(placed.style().resolvedSettings, loreEntity.animation, placed.context());
```

This convention applies to invocation expressions **inside bodies only**.
It does not require method or constructor **declarations**, class headers,
record declarations, or other declarations outside method bodies to occupy
a single line. Preserve Java comments and text block (`"""`) contents.

Run the formatter after editing Java files:

```bash
python3 tools/format_java_calls.py
```

Check formatting without modifying source files:

```bash
python3 tools/format_java_calls.py --check
```

The build workflow runs the same check before Gradle. Commits containing
only documentation (`docs/**`, `*.md`, `*.mdx`, `*.adoc`, `*.rst`)
do not start automatic Build workflows. Mixed Java and documentation
commits still start CI. You can always run CI manually from GitHub Actions.
