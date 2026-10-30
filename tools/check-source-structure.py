#!/usr/bin/env python3
"""Check recovered Java type declarations without depending on Maven downloads."""
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
TYPE = re.compile(r"^(?:public\s+)?(?:(?:abstract|final|sealed)\s+)?(?:class|interface|enum|record)\s+(\w+)", re.MULTILINE)


def main():
    errors = []
    declared = {}
    for module in sorted(ROOT.glob("karate-*")):
        source = module / "src/main/java"
        if not source.is_dir():
            continue
        for path in sorted(source.rglob("*.java")):
            text = path.read_text(encoding="utf-8")
            package = re.search(r"^package\s+([\w.]+);", text, re.MULTILINE)
            top = TYPE.search(text)
            if not package or not top:
                continue
            label = path.relative_to(ROOT)
            if top[1] != path.stem:
                errors.append(f"{label}: declares {top[1]}, filename is {path.stem}")
            qualified = package[1] + "." + top[1]
            if qualified in declared:
                errors.append(f"{label}: duplicate {qualified} in {declared[qualified]}")
            declared[qualified] = label
            # At member indentation, a type cannot have an enclosing type's name.
            stack = []
            for line in text.splitlines():
                match = re.match(r"(\s*)(?:(?:public|private|protected|static|final|abstract|sealed)\s+)*(?:class|interface|enum|record)\s+(\w+)", line)
                if not match:
                    continue
                indent, name = len(match[1]), match[2]
                while stack and stack[-1][0] >= indent:
                    stack.pop()
                if any(outer == name for _, outer in stack):
                    errors.append(f"{label}: nested type {name} repeats enclosing type name")
                stack.append((indent, name))
    for error in errors:
        print(error)
    print(f"{len(declared)} top-level types checked; {len(errors)} structural errors")
    return bool(errors)


if __name__ == "__main__":
    sys.exit(main())
