#!/usr/bin/env python3

import argparse
import hashlib
import subprocess
import sys
from pathlib import Path

EXPECTED_PUBLISHER_SHA256 = "b03c03eedb9e4e1dfa374f56c13dcd2bdc3333eb682f29a6cf239a2eb3872c89"
PROTECTED_FEED_PREFIX = "announcements/"


def publisher_digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def changed_paths(base: str, head: str) -> list[str]:
    result = subprocess.run(
        ["git", "diff", "--name-only", "--no-renames", base, head, "--"],
        check=True,
        capture_output=True,
        text=True,
    )
    return [line.strip().replace("\\", "/") for line in result.stdout.splitlines() if line.strip()]


def validate(publisher: Path, paths: list[str]) -> list[str]:
    errors = []
    actual_digest = publisher_digest(publisher)
    if actual_digest != EXPECTED_PUBLISHER_SHA256:
        errors.append(
            "legacy publisher is not the reviewed inert tombstone "
            f"(expected {EXPECTED_PUBLISHER_SHA256}, found {actual_digest})"
        )

    protected_changes = sorted(path for path in paths if path.startswith(PROTECTED_FEED_PREFIX))
    if protected_changes:
        errors.append(
            "live announcement feed changes require the future dedicated Feed Publisher identity: "
            + ", ".join(protected_changes)
        )

    return errors


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Verify protected AHP release surfaces.")
    parser.add_argument("--publisher", type=Path, default=Path(".github/workflows/publish.yml"))
    parser.add_argument("--base")
    parser.add_argument("--head")
    parser.add_argument("--changed-path", action="append", default=[])
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    paths = [path.replace("\\", "/") for path in args.changed_path]
    if args.base or args.head:
        if not args.base or not args.head:
            raise SystemExit("--base and --head must be supplied together")
        paths.extend(changed_paths(args.base, args.head))

    errors = validate(args.publisher, paths)
    if errors:
        for error in errors:
            print(f"ERROR: {error}", file=sys.stderr)
        return 1

    print("Release surfaces satisfy the quarantine policy.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
