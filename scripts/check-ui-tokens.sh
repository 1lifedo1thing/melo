#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
if python3 -c 'import sys' >/dev/null 2>&1; then
    python_cmd=(python3)
elif python -c 'import sys' >/dev/null 2>&1; then
    python_cmd=(python)
elif py -3 -c 'import sys' >/dev/null 2>&1; then
    python_cmd=(py -3)
else
    echo "Python 3 is required for the UI token check." >&2
    exit 1
fi

"${python_cmd[@]}" - "$repo_root" <<'PY'
from pathlib import Path
import re
import sys

root = Path(sys.argv[1])
allowlist_path = root / "scripts" / "check-ui-tokens.allowlist"

allowed = {}
for raw in allowlist_path.read_text(encoding="utf-8").splitlines():
    line = raw.strip()
    if not line or line.startswith("#"):
        continue
    check, path, reason = line.split("|", 2)
    allowed.setdefault((check, path), reason)


def without_comments(source: str) -> str:
    """Blank Kotlin comments while preserving offsets and line numbers."""
    output = list(source)
    state = "code"
    i = 0
    while i < len(source):
        char = source[i]
        nxt = source[i + 1] if i + 1 < len(source) else ""
        if state == "code":
            if char == '"':
                if source[i:i + 3] == '"""':
                    state = "raw_string"
                    i += 3
                    continue
                state = "string"
            elif char == "'":
                state = "char"
            elif char == "/" and nxt == "/":
                output[i] = output[i + 1] = " "
                state = "line_comment"
                i += 2
                continue
            elif char == "/" and nxt == "*":
                output[i] = output[i + 1] = " "
                state = "block_comment"
                i += 2
                continue
        elif state in ("string", "char"):
            if char == "\\":
                i += 2
                continue
            if (state == "string" and char == '"') or (state == "char" and char == "'"):
                state = "code"
        elif state == "raw_string":
            if source[i:i + 3] == '"""':
                state = "code"
                i += 3
                continue
        elif state == "line_comment":
            if char == "\n":
                state = "code"
            else:
                output[i] = " "
        elif state == "block_comment":
            if char == "*" and nxt == "/":
                output[i] = output[i + 1] = " "
                state = "code"
                i += 2
                continue
            if char != "\n":
                output[i] = " "
        i += 1
    return "".join(output)


rules = {
    "shape": re.compile(r"\bRoundedCornerShape\s*\(\s*\d+(?:\.\d+)?\.dp\b"),
    "font": re.compile(r"\bfontSize\s*=\s*\d+(?:\.\d+)?\.sp\b"),
    "tonal": re.compile(r"\btonalElevation\s*="),
}
source_roots = [
    root / "app" / "src" / "main" / "java" / "in" / "melo" / "presentation",
    root / "app" / "src" / "main" / "java" / "in" / "melo" / "ui",
]
failures = []

for source_root in source_roots:
    for path in sorted(source_root.rglob("*.kt")):
        relative = path.relative_to(root).as_posix()
        if "/ui/theme/" in relative:
            continue
        source = without_comments(path.read_text(encoding="utf-8"))
        for check, pattern in rules.items():
            if (check, relative) in allowed:
                continue
            for match in pattern.finditer(source):
                line = source.count("\n", 0, match.start()) + 1
                failures.append(f"{relative}:{line}: raw {check} token")

if failures:
    print("UI token check failed:", file=sys.stderr)
    print("\n".join(failures), file=sys.stderr)
    sys.exit(1)

print("UI token check passed.")
for (_, path), reason in sorted(allowed.items()):
    print(f"Allowed {path}: {reason}")
PY
