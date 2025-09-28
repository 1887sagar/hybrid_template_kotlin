#!/usr/bin/env python3
"""
zap_gradle.py - Deep clean ("panic button") for Gradle/Kotlin projects.
"""
from __future__ import annotations

import argparse
import os
import shutil
import subprocess
import sys
from pathlib import Path
from typing import Iterable, List

VERSION = "1.1.0"

def which(cmd: str) -> str | None:
    from shutil import which as _which
    return _which(cmd)


def run_cmd(cmd: List[str], cwd: Path, dry_run: bool, verbose: bool) -> int:
    if dry_run:
        print(f"DRY-RUN: would run: {' '.join(cmd)} (cwd={cwd})")
        return 0
    try:
        if verbose:
            print(f"RUN: {' '.join(cmd)} (cwd={cwd})")
        proc = subprocess.run(cmd, cwd=str(cwd), check=False)
        return proc.returncode
    except Exception as ex:
        print(f"WARN: command failed: {' '.join(cmd)} -> {ex}")
        return 1


def confirm_under_home(path: Path, home: Path) -> bool:
    try:
        path = path.resolve()
        home = home.resolve()
        return str(path).startswith(str(home) + os.sep)
    except Exception:
        return False


def safe_rm(path: Path, dry_run: bool, verbose: bool):
    if not path.exists():
        return
    if dry_run:
        print(f"DRY-RUN: rm -rf {path}")
        return
    try:
        if path.is_dir() and not path.is_symlink():
            shutil.rmtree(path, ignore_errors=True)
        else:
            try:
                path.unlink()
            except IsADirectoryError:
                shutil.rmtree(path, ignore_errors=True)
        if verbose:
            print(f"removed: {path}")
    except Exception as ex:
        print(f"WARN: failed to remove {path}: {ex}")


def glob_and_remove(base: Path, patterns, dry_run: bool, verbose: bool):
    import glob
    for pat in patterns:
        for hit in glob.glob(str(base / pat)):
            safe_rm(Path(hit), dry_run, verbose)


def find_project_dirs_to_remove(root: Path):
    targets = []
    for cur, dirs, files in os.walk(root, topdown=True):
        # Prune common dirs to speed up
        prune = {".git", ".idea", ".vscode", ".venv", "venv", "node_modules", "build", ".gradle"}
        # collect build/.gradle present in current level
        for d in list(dirs):
            if d in {"build", ".gradle"}:
                targets.append(Path(cur) / d)
        # actually prune
        dirs[:] = [d for d in dirs if d not in prune]
    return targets


def build_parser() -> argparse.ArgumentParser:
    epilog = (
        "Examples:\n"
        "  python3 zap_gradle.py --dry-run\n"
        "  python3 zap_gradle.py                         # normal deep clean\n"
        "  python3 zap_gradle.py --aggressive            # re-download all deps\n"
        "  python3 zap_gradle.py --include-kotlin-native # also delete ~/.konan\n"
        "  python3 zap_gradle.py --gradlew ./gradlew\n"
        "  python3 zap_gradle.py help\n"
    )
    parser = argparse.ArgumentParser(
        prog="zap_gradle.py",
        description=(
            "Deep clean a Gradle/Kotlin project when Gradle gets confused.\n"
            "Stops daemons, clears build cache, removes project build/ and .gradle/ directories,\n"
            "and prunes user caches. Optional aggressive mode also wipes dependency caches and wrapper dists."
        ),
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=epilog,
        add_help=True,
    )
    parser.add_argument("--version", action="version", version=f"%(prog)s {VERSION}")
    parser.add_argument("--gradlew", default="./gradlew", help="Path to Gradle wrapper (default: ./gradlew)")
    parser.add_argument(
        "--gradle-user-home",
        default=os.environ.get("GRADLE_USER_HOME", str(Path.home() / ".gradle")),
        help="Gradle user home (default: env.GRADLE_USER_HOME or ~/.gradle)",
    )
    parser.add_argument(
        "--xdg-cache-home",
        default=os.environ.get("XDG_CACHE_HOME", str(Path.home() / ".cache")),
        help="XDG cache home (default: env.XDG_CACHE_HOME or ~/.cache)",
    )
    parser.add_argument("--dry-run", action="store_true", help="Preview actions without making changes")
    parser.add_argument("--aggressive", action="store_true", help="Also remove dependency caches and wrapper dists")
    parser.add_argument("--include-kotlin-native", action="store_true", help="Also remove ~/.konan (Kotlin/Native)")
    parser.add_argument("--verbose", "-v", action="store_true", help="Verbose output")
    return parser


def main(argv: List[str]) -> int:
    parser = build_parser()

    # Support `help` as a subcommand for muscle memory: `zap_gradle.py help`
    if argv and argv[0] in {"help", "usage"}:
        parser.print_help()
        return 0

    args = parser.parse_args(argv)

    root = Path.cwd()
    home = Path.home()
    gradlew = Path(args.gradlew)
    gradle_user_home = Path(args.gradle_user_home)   # fixed minor typo
    xdg_cache_home = Path(args.xdg_cache_home)
    dry_run = args.dry_run
    aggressive = args.aggressive
    include_konan = args.include_kotlin_native
    verbose = args.verbose

    print(f"== zap_gradle ==")
    print(f" version: {VERSION}")
    print(f" root: {root}")
    print(f" gradlew: {gradlew}")
    print(f" GRADLE_USER_HOME: {gradle_user_home}")
    print(f" XDG_CACHE_HOME: {xdg_cache_home}")
    print(f" dry_run={dry_run} aggressive={aggressive} include_kotlin_native={include_konan} verbose={verbose}")

    # 1) Stop daemons
    if gradlew.exists():
        run_cmd([str(gradlew), "--stop"], cwd=root, dry_run=dry_run, verbose=verbose)
    elif which("gradle"):
        run_cmd(["gradle", "--stop"], cwd=root, dry_run=dry_run, verbose=verbose)
    else:
        print("WARN: gradle/gradlew not found to stop daemons; continuing")

    # 2) Clean build cache (best effort)
    if gradlew.exists():
        run_cmd([str(gradlew), "-q", "cleanBuildCache"], cwd=root, dry_run=dry_run, verbose=verbose)

    # 3) Project-local build artifacts
    print(">> Removing project-local `.gradle/` and `build/` directories")
    for pth in find_project_dirs_to_remove(root):
        safe_rm(pth, dry_run, verbose)

    # 4) User-level Gradle caches (safe subset)
    if not confirm_under_home(gradle_user_home, home):
        print(f"SAFETY: GRADLE_USER_HOME ({gradle_user_home}) is not under HOME ({home}); skipping user-level prune")
    else:
        print(">> Pruning user-level Gradle caches (safe subset)")
        # daemon, notifications
        for sub in ["daemon", "notifications"]:
            safe_rm(gradle_user_home / sub, dry_run, verbose)

        # caches: build-cache-*, vcs-*, */kotlin
        glob_and_remove(gradle_user_home, ["caches/build-cache-*", "vcs-*"], dry_run, verbose)
        # remove kotlin shards inside caches subdirs
        caches_dir = gradle_user_home / "caches"
        if caches_dir.exists():
            for subdir in caches_dir.glob("*"):
                if subdir.is_dir():
                    safe_rm(subdir / "kotlin", dry_run, verbose)

        # XDG Gradle cache
        xdg_gradle = xdg_cache_home / "gradle"
        if xdg_gradle.exists():
            safe_rm(xdg_gradle, dry_run, verbose)

        # 5) Aggressive
        if aggressive:
            print(">> AGGRESSIVE: removing dependency caches and wrapper dists")
            glob_and_remove(gradle_user_home, [
                "caches/modules-2",
                "caches/jars-*",
                "caches/transforms-*",
                "wrapper/dists"
            ], dry_run, verbose)

    # 6) Kotlin/Native toolchains
    if include_konan:
        konan = home / ".konan"
        if confirm_under_home(konan, home):
            print(">> Removing ~/.konan (Kotlin/Native)")
            safe_rm(konan, dry_run, verbose)
        else:
            print("SAFETY: refusing to remove ~/.konan outside HOME")

    print(">> zap complete")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
