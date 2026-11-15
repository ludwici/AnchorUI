#!/usr/bin/env python3
"""Flatten wrapped Java method and constructor invocations in method bodies.

Method declarations, comments, and multiline string literals are unchanged.
"""
from __future__ import annotations

import argparse
import re
from pathlib import Path

EXCLUDED = frozenset(("if", "while", "for", "switch", "catch", "synchronized",
                      "try", "assert", "return", "throw", "case", "new",
                      "instanceof", "record", "default", "do"))
TYPE_RE = re.compile(r"\b(?:class|interface|enum|record)\s+[A-Za-z_$]")
ANON_RE = re.compile(r"\bnew\s+[\w.$<>?,\s]+\([^{};]*\)\s*$")
METHOD_RE = re.compile(r"\)\s*(?:throws\s+[\w$.,\s]+)?$")
CONTROL_RE = re.compile(r"\b(?:if|for|while|switch|catch|synchronized|try)\s*\([^()]*\)\s*$")


def lex(source: str) -> tuple[str, list[int]]:
    mask = list(source)
    kinds = [0] * len(source)
    n = len(source)

    def hide(start: int, end: int, kind: int) -> None:
        for index in range(start, end):
            kinds[index] = kind
            if source[index] not in "\r\n":
                mask[index] = " "

    i = 0
    while i < n:
        if source.startswith("//", i):
            end = source.find("\n", i)
            if end == -1:
                end = n
            hide(i, end, 1)
            i = end
        elif source.startswith("/*", i):
            end = source.find("*/", i + 2)
            end = n if end == -1 else end + 2
            hide(i, end, 1)
            i = end
        elif source.startswith('"""', i):
            end = i + 3
            while end < n:
                if source.startswith('"""', end) and source[end - 1] != "\\":
                    end += 3
                    break
                end += 1
            hide(i, min(n, end), 2)
            i = min(n, end)
        elif source[i] in "\"'":
            quote = source[i]
            end = i + 1
            while end < n:
                if source[end] == "\\":
                    end += 2
                elif source[end] == quote:
                    end += 1
                    break
                else:
                    end += 1
            end = min(n, end)
            hide(i, end, 2)
            i = end
        else:
            i += 1
    return "".join(mask), kinds


def canonical(source: str) -> str:
    _, kinds = lex(source)
    return "".join(character for i, character in enumerate(source)
                   if kinds[i] != 0 or not character.isspace())


def one_line_calls(source: str) -> tuple[str, int]:
    masked, kinds = lex(source)
    scopes: list[bool] = []
    parens: list[tuple[int, bool]] = []
    candidates: list[tuple[int, int, str]] = []
    delimiter = 0

    for index, char in enumerate(masked):
        if char == "{":
            prefix = masked[max(delimiter, index - 500):index].strip()
            is_type = bool(TYPE_RE.search(prefix) or ANON_RE.search(prefix))
            is_declaration = not is_type and bool(METHOD_RE.search(prefix)) and not CONTROL_RE.search(prefix)
            parent_method = bool(scopes and scopes[-1])
            in_method = not is_type and (parent_method or is_declaration or prefix.endswith("->"))
            scopes.append(in_method)
            delimiter = index + 1
        elif char == "}":
            if scopes:
                scopes.pop()
            delimiter = index + 1
        elif char == ";":
            delimiter = index + 1
        elif char == "(":
            previous = index - 1
            while previous >= 0 and masked[previous].isspace():
                previous -= 1
            end = previous
            while previous >= 0 and (masked[previous].isalnum() or masked[previous] in "_$"):
                previous -= 1
            name = masked[previous + 1:end + 1]
            is_call = (bool(scopes and scopes[-1]) and name not in EXCLUDED
                       and (bool(re.fullmatch(r"[A-Za-z_$][\w$]*", name))
                            or (end >= 0 and masked[end] == ">")))
            parens.append((index, is_call))
        elif char == ")":
            if not parens:
                continue
            start, is_call = parens.pop()
            if not is_call:
                continue
            raw = source[start + 1:index]
            if "\n" not in raw or any(ch in "{};" for ch in masked[start + 1:index]):
                continue
            if 1 in kinds[start + 1:index]:
                continue

            output = []
            pos = start + 1
            while pos < index:
                if kinds[pos] != 0 or not source[pos].isspace():
                    output.append(source[pos])
                    pos += 1
                    continue
                begin = pos
                while pos < index and kinds[pos] == 0 and source[pos].isspace():
                    pos += 1
                spacing = source[begin:pos]
                if "\n" not in spacing:
                    output.append(spacing)
                    continue
                before = source[begin - 1] if begin > start + 1 else "("
                after = source[pos] if pos < index else ")"
                output.append("" if before in "(." or after in ")." else " ")

            formatted = "".join(output)
            if formatted != raw:
                candidates.append((start + 1, index, formatted))

    candidates.sort(key=lambda item: (item[0], -item[1]))
    chosen = []
    last_end = -1
    for item in candidates:
        if item[0] < last_end:
            continue
        chosen.append(item)
        last_end = item[1]

    result = source
    for begin, end, replacement in reversed(chosen):
        result = result[:begin] + replacement + result[end:]

    if canonical(source) != canonical(result):
        raise ValueError("Formatter changed Java tokens; refusing to save file")
    return result, len(chosen)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="Check without changing files")
    parser.add_argument("--root", type=Path, default=Path("src"), help="Source root")
    options = parser.parse_args()
    changed = []
    total = 0
    for file in sorted(options.root.rglob("*.java")):
        original = file.read_text(encoding="utf-8")
        formatted, count = one_line_calls(original)
        if formatted == original:
            continue
        changed.append(str(file))
        total += count
        if not options.check:
            file.write_text(formatted, encoding="utf-8", newline="")
    print(f"Scanned Java under {options.root}: {len(changed)} files need changes, {total} wrapped calls")
    for file in changed:
        print(file)
    return 1 if options.check and changed else 0


if __name__ == "__main__":
    raise SystemExit(main())
