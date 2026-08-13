import os
import glob
import shutil
import argparse

# ──────────────────────────────────────────────────────────────────────────────
# Constants & Configuration
# ──────────────────────────────────────────────────────────────────────────────

# The repository root, resolved from this script's location.
REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

# The destination directory
DEST_DIR = r"C:\Users\tweek\Downloads"

# The Minecraft version currently on 'develop' branch
# Update this string when upgrading the mod to a new version
DEVELOP_VERSION = "1.21.1"

# The Minecraft version for legacy/maintenance branch
LEGACY_VERSION = "1.20.1"

# ──────────────────────────────────────────────────────────────────────────────
# File Patterns
# ──────────────────────────────────────────────────────────────────────────────

# Files belonging to the 'develop' branch (Fabric & NeoForge)
FILES_DEVELOP = [
    (
        os.path.join("fabric", "build", "libs"),
        f"adorablehamsterpets-*-{DEVELOP_VERSION}+fabric.jar"
    ),
    (
        os.path.join("neoforge", "build", "libs"),
        f"adorablehamsterpets-*-{DEVELOP_VERSION}+neoforge.jar"
    )
]

# Files belonging to the 'legacy' branch (Fabric & Forge)
FILES_LEGACY = [
    (
        os.path.join("fabric", "build", "libs"),
        f"adorablehamsterpets-*-{LEGACY_VERSION}+fabric.jar"
    ),
    (
        os.path.join("forge", "build", "libs"),
        f"adorablehamsterpets-*-{LEGACY_VERSION}+forge.jar"
    )
]

# ──────────────────────────────────────────────────────────────────────────────
# Logic
# ──────────────────────────────────────────────────────────────────────────────

def get_preview_prefix(filename):
    """Returns the specific prefix based on the platform found in the filename."""
    lower_name = filename.lower()

    if "neoforge" in lower_name:
        return "NeoForge-PREVIEW-"
    elif "fabric" in lower_name:
        return "Fabric-PREVIEW-"
    elif "forge" in lower_name:
        return "Forge-PREVIEW-"
    else:
        return "PREVIEW-"

def copy_files(file_patterns):
    print(f"--- Scanning for jars in {REPO_ROOT} ---")
    files_copied = 0

    if not os.path.exists(DEST_DIR):
        print(f"Error: Destination directory does not exist: {DEST_DIR}")
        return

    for subdir, pattern in file_patterns:
        # Construct the full search path
        search_path = os.path.join(REPO_ROOT, subdir, pattern)

        # Find files matching the pattern (handling the dynamic version number)
        found_files = glob.glob(search_path)

        # Filter out dev and sources jars first
        valid_files =[
            f for f in found_files
            if "sources" not in os.path.basename(f).lower() and "dev" not in os.path.basename(f).lower()
        ]

        if not valid_files:
            print(f"MISSING: No valid file found matching: {pattern}")
            continue

        # Sort the valid files by modification time (newest first)
        valid_files.sort(key=os.path.getmtime, reverse=True)

        # Grab ONLY the most recently modified file
        source_path = valid_files[0]
        filename = os.path.basename(source_path)

        prefix = get_preview_prefix(filename)
        new_filename = prefix + filename
        dest_path = os.path.join(DEST_DIR, new_filename)

        # --- PRE-CLEANUP LOGIC ---
        # Check if the file already exists in Downloads and delete it first
        if os.path.exists(dest_path):
            try:
                os.remove(dest_path)
                print(f"CLEANUP: Removed existing file {new_filename}")
            except OSError as e:
                print(f"WARNING: Could not remove old file {new_filename}. Reason: {e}")
                # Continue anyway; shutil.copy2 might still succeed in overwriting it

        # --- COPY LOGIC ---
        try:
            shutil.copy2(source_path, dest_path)
            print(f"SUCCESS: Copied to {new_filename}")
            files_copied += 1
        except Exception as e:
            print(f"ERROR: Failed to copy {filename}. Reason: {e}")

    print(f"\nDone. {files_copied} files copied to {DEST_DIR}")

def main():
    parser = argparse.ArgumentParser(description="Copy and rename mod preview jars.")
    parser.add_argument("--mode", type=str, choices=["develop", "all"], required=True,
                        help="Mode 'develop' for current dev version, 'all' for dev + legacy")

    args = parser.parse_args()

    # Determine which lists to process based on mode
    targets = []

    # Always include develop files
    targets.extend(FILES_DEVELOP)

    # If mode is all, include legacy files also
    if args.mode == "all":
        targets.extend(FILES_LEGACY)

    print(f"Mode: {args.mode.upper()} (Targeting Develop: {DEVELOP_VERSION})")
    copy_files(targets)

if __name__ == "__main__":
    main()
