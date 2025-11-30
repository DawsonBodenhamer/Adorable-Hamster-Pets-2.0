import os
import sys
import json
import subprocess
import fnmatch
import re
import argparse
import time

# --- Constants ---
CONFIG_FILENAME = "techspec_config.json"
MARKER_START = "## AHP Provided Code"
MARKER_END = "End AHP Provided Code"

# --- Globals ---
VERBOSE = False

def print_error(msg):
    print(f"ERROR: {msg}")

def print_info(msg):
    print(f"INFO: {msg}")

def print_verbose(msg):
    if VERBOSE:
        print(f"  {msg}")

def load_config():
    """Load and validate configuration."""
    print_verbose("Loading configuration...")
    script_dir = os.path.dirname(os.path.abspath(__file__))
    config_path = os.path.join(script_dir, CONFIG_FILENAME)

    if not os.path.exists(config_path):
        print_error(f"Config file not found at {config_path}")
        sys.exit(1)

    try:
        with open(config_path, 'r', encoding='utf-8') as f:
            config = json.load(f)
    except json.JSONDecodeError as e:
        print_error(f"Failed to parse config JSON: {e}")
        sys.exit(1)

    # Validate required fields
    required = ["root_dir", "techspec_pattern", "backup_pattern",
                "include_extensions", "exclude_patterns", "force_include_files"]

    for req in required:
        if req not in config:
            print_error(f"Missing required config field: {req}")
            sys.exit(1)

    # Normalize root_dir
    config["root_dir"] = os.path.abspath(config["root_dir"])
    if not os.path.isdir(config["root_dir"]):
        print_error(f"root_dir does not exist: {config['root_dir']}")
        sys.exit(1)

    return config

def get_git_branch(root_dir):
    """Get current git branch name."""
    print_verbose("Detecting Git branch...")
    try:
        result = subprocess.run(
            ["git", "rev-parse", "--abbrev-ref", "HEAD"],
            cwd=root_dir,
            capture_output=True,
            text=True,
            check=True
        )
        branch = result.stdout.strip()
        if not branch:
            raise ValueError("Empty branch name")
        print_verbose(f"Current Branch: {branch}")
        return branch
    except Exception as e:
        print_error(f"Failed to determine Git branch. {e}")
        sys.exit(1)

def sanitize_branch_name(branch_name):
    return branch_name.replace("/", "_")

def load_gitignore(root_dir):
    """Load .gitignore patterns."""
    gitignore_path = os.path.join(root_dir, ".gitignore")
    patterns = []
    if os.path.exists(gitignore_path):
        try:
            with open(gitignore_path, 'r', encoding='utf-8') as f:
                for line in f:
                    line = line.strip()
                    if line and not line.startswith('#'):
                        patterns.append(line)
        except Exception:
            pass
    return patterns

def is_ignored(rel_path, ignore_patterns):
    """Check if a path matches ignore patterns."""
    for pattern in ignore_patterns:
        # Directory pattern
        if pattern.endswith('/'):
            if rel_path.startswith(pattern): return True
            if ("/" + pattern) in rel_path: return True
        # File pattern
        else:
            if fnmatch.fnmatch(os.path.basename(rel_path), pattern): return True
            if fnmatch.fnmatch(rel_path, pattern): return True
    return False

def get_language_id(filename):
    """Map extension to markdown language identifier."""
    ext = os.path.splitext(filename)[1].lower()
    mapping = {
        ".java": "java", ".kt": "kotlin", ".kts": "kotlin",
        ".json": "json", ".yml": "yaml", ".yaml": "yaml",
        ".md": "markdown", ".txt": "text", ".toml": "toml",
        ".cfg": "ini", ".gradle": "groovy", ".properties": "properties",
        ".mcmeta": "json", ".mcfunction": "mcfunction"
    }
    return mapping.get(ext, "")

def parse_changelog(content):
    """
    Extracts the top 3 release sections from CHANGELOG.md.
    Release section: Starts with '## [' and ends at next '---' or EOF.
    """
    lines = content.splitlines()
    output_lines = []
    section_count = 0
    in_section = False

    # Regex to identify release header
    release_header_re = re.compile(r'^##\s+\[.*')

    for line in lines:
        if release_header_re.match(line):
            section_count += 1
            if section_count > 3: break
            in_section = True
            output_lines.append(line)
            continue
        if in_section:
            # Check for separator which might end a section within the file flow
            # (Though usually separators are between versions, include them
            # as part of the block until we hit the 4th header)
            output_lines.append(line)

    # Trim trailing newlines
    return "\n".join(output_lines).strip()

def get_semantic_sort_key(rel_path):
    """
    Returns a sorting key: (Loader, RootType, FeaturePriority, FeatureRoot, SubPath, Filename).
    This keeps directory trees (like 'block/' and 'block/custom/') TOGETHER,
    while sorting the high-level trees by importance.
    """
    lower = rel_path.lower()
    filename = os.path.basename(lower)

    # 1. Loader Priority
    loader_prio = 9
    if lower in ['changelog.md', 'readme.md', 'readme_curseforge_style.md']: loader_prio = 0
    elif '/' not in lower: loader_prio = 1
    elif lower.startswith('common/'): loader_prio = 2
    elif lower.startswith('fabric/'): loader_prio = 3
    elif lower.startswith('neoforge/') or lower.startswith('forge/'): loader_prio = 4

    # 2. Root Type (Code vs Resources)
    # Prioritize Java/Source files before Resources
    is_resource = 'src/main/resources' in lower or 'src/main/generated' in lower
    root_type_prio = 1 if is_resource else 0

    # 3. Identify "Feature Root"
    # The feature root is the folder immediately following the main namespace.
    # e.g. "net/dawson/adorablehamsterpets/block/custom/X.java" -> Feature Root is "block"
    # e.g. "assets/adorablehamsterpets/models/item/X.json" -> Feature Root is "models"

    feature_root = ""

    # Heuristic to find the segment after the namespace
    keywords = ['net/dawson/adorablehamsterpets/', 'assets/adorablehamsterpets/', 'data/adorablehamsterpets/']
    for kw in keywords:
        if kw in lower:
            idx = lower.find(kw) + len(kw)
            remainder = lower[idx:]
            if '/' in remainder:
                feature_root = remainder.split('/')[0]
            else:
                feature_root = "root_package" # Files directly in the main package
            break

    if not feature_root and 'mixin' in lower: feature_root = "mixin"

    # 4. Feature Priority (The "Group Weight")
    # Prioritize the FEATURE ROOT, not the specific file.
    # This keeps 'block', 'block/client', and 'block/custom' together.

    feat_prio = 50 # Default (Alphabetical Middle)

    # Priority 0: Entrypoints & Configs
    if feature_root in ['root_package', 'config', 'registry', 'init', 'main', 'datagen']:
        feat_prio = 0
    # Priority 1: Core Game Objects
    elif feature_root in ['block', 'item', 'entity', 'fluid', 'effect', 'enchantment', 'potion', 'sound']:
        feat_prio = 10
    # Priority 2: Gameplay Systems
    elif feature_root in ['advancement', 'recipe', 'loot', 'tag', 'data', 'networking', 'component', 'command', 'event']:
        feat_prio = 20
    # Priority 3: World Gen
    elif feature_root in ['world', 'biome', 'structure', 'dimension']:
        feat_prio = 30
    # Priority 4: Client/Visuals
    elif feature_root in ['client', 'screen', 'gui', 'render', 'model', 'texture', 'particle', 'animation', 'geo', 'blockstates']:
        feat_prio = 40
    # Priority 90: Tech/Mixins/Compat (Bottom of code)
    elif feature_root in ['mixin', 'integration', 'compat', 'util', 'access', 'accessor']:
        feat_prio = 90
    # Priority 99: Lang (Bottom of assets)
    elif feature_root in ['lang']:
        feat_prio = 99

    # 5. Filename Weight (Specific overrides inside a folder)
    file_prio = 10
    if any(x in filename for x in ['registry', 'registries', 'modblocks', 'moditems', 'modentities']):
        file_prio = 0 # Main registry files top of list

    return loader_prio, root_type_prio, feat_prio, lower, file_prio, filename

def remove_java_imports(content):
    """
    Removes all import statements and swallows subsequent blank lines,
    then enforces exactly one blank line before the class definition starts.
    """
    lines = content.splitlines()
    new_lines = []

    in_import_block = False
    placeholder_added = False

    for line in lines:
        stripped = line.strip()

        # Check for import statement
        if stripped.startswith("import "):
            in_import_block = True
            if not placeholder_added:
                new_lines.append("// (Imports removed to save token count)")
                placeholder_added = True
            continue # Skip the actual import line

        # If we are currently in the "after-import" zone...
        if in_import_block:
            if stripped == "":
                # Swallow existing blank lines to avoid double/triple spacing
                continue
            else:
                # Found the first line of code (Annotation, Class, Comment, etc.)
                # Force insert exactly one blank line here to separate from placeholder
                new_lines.append("")
                in_import_block = False

        new_lines.append(line)

    return "\n".join(new_lines)

def main():
    start_time = time.time()

    # Parse Args
    parser = argparse.ArgumentParser()
    parser.add_argument("--structure-only", action="store_true", help="Omit all content.")
    parser.add_argument("-v", "--verbose", action="store_true", help="Verbose logging.")
    args = parser.parse_args()

    global VERBOSE
    VERBOSE = args.verbose

    print_info("--- AHP Code Injector Started ---")

    # Load Config
    config = load_config()
    root_dir = config["root_dir"]

    # Determine Git Branch
    raw_branch = get_git_branch(root_dir)
    sanitized_branch = sanitize_branch_name(raw_branch)
    techspec_filename = config["techspec_pattern"].replace("{branch}", sanitized_branch)
    backup_filename = config["backup_pattern"].replace("{branch}", sanitized_branch)
    techspec_path = os.path.join(root_dir, techspec_filename)
    backup_path = os.path.join(root_dir, backup_filename)

    if not os.path.exists(techspec_path):
        print_error(f"Tech spec file not found: {techspec_filename}")
        sys.exit(1)

    # Discovery
    print_info("Scanning files...")
    gitignore = load_gitignore(root_dir)
    exclude_patterns = config["exclude_patterns"]
    omit_content_patterns = config.get("omit_content_patterns", [])
    include_exts = set(config["include_extensions"])
    force_include = set(config["force_include_files"])
    # Always exclude the spec and backup files relative to root
    forbidden = {techspec_filename, backup_filename}

    included_files = []
    scanned_count = 0

    for root, dirs, files in os.walk(root_dir):
        if ".git" in dirs: dirs.remove(".git")

        for file in files:
            scanned_count += 1
            abs_path = os.path.join(root, file)
            rel_path = os.path.relpath(abs_path, root_dir).replace("\\", "/")

            if rel_path in forbidden: continue

            should_include = False

            if rel_path in force_include:
                should_include = True
            else:
                ext = os.path.splitext(file)[1].lower()
                if ext in include_exts:
                    if not is_ignored(rel_path, gitignore) and not is_ignored(rel_path, exclude_patterns):
                        should_include = True

            if should_include:
                included_files.append(rel_path)
                print_verbose(f"[+] {rel_path}")

    # Sort
    print_info("Sorting files (Semantic Priority)...")
    included_files.sort(key=get_semantic_sort_key)

    # Generation
    print_info("Generating content blocks...")
    generated_output = []

    # State for folder grouping
    last_directory = None

    for rel_path in included_files:
        current_directory = os.path.dirname(rel_path)
        filename = os.path.basename(rel_path)

        # 1. Directory Header
        if current_directory != last_directory:
            display_dir = current_directory if current_directory else "Repository Root"
            generated_output.append(f"### 📂 `{display_dir}/`")
            last_directory = current_directory

        # 2. File Name
        block = []
        block.append(f"`{filename}`")

        # Notes
        note_data = config.get("file_notes", {}).get(rel_path)
        if note_data:
            if note_data.get("position") == "before":
                block.append(note_data.get("note", ""))

        # 3. Content Logic
        is_pattern_omitted = is_ignored(rel_path, omit_content_patterns)

        if args.structure_only:
            # Structure Mode: Skip code block entirely, just keep header/notes
            pass

        elif is_pattern_omitted:
            # Config Omission: Keep code block, but use placeholder
            lang = get_language_id(rel_path)
            block.append(f"```{lang}")
            block.append("(Content omitted to save token count and can be provided upon request)")
            block.append("```")

        else:
            # Normal: Read file and include content
            lang = get_language_id(rel_path)
            abs_path = os.path.join(root_dir, rel_path)
            content = ""
            try:
                with open(abs_path, 'r', encoding='utf-8', errors='replace') as f:
                    content = f.read()

                if rel_path == "CHANGELOG.md":
                    content = parse_changelog(content)

                # Strip imports for Java files
                if rel_path.endswith(".java"):
                    content = remove_java_imports(content)

            except Exception as e:
                print_error(f"Read error: {rel_path} ({e})")
                content = "(Read Error)"

            block.append(f"```{lang}")
            block.append(content)
            block.append("```")

        # Note After
        if note_data and note_data.get("position") == "after":
            block.append(note_data.get("note", ""))

        generated_output.append("\n".join(block))

    # Join blocks
    # If Structure Only mode, use single newline (compact).
    # Otherwise use double newline (standard markdown spacing).
    separator = "\n" if args.structure_only else "\n\n"
    full_content_body = separator.join(generated_output)

    # Backup & Write
    print_info(f"Writing to {techspec_path} (Backup: {backup_filename})...")

    try:
        # Read Original
        with open(techspec_path, 'r', encoding='utf-8') as f:
            original = f.read()

        # Write Backup
        with open(backup_path, 'w', encoding='utf-8') as f:
            f.write(original)

        # Inject
        start_idx = original.find(MARKER_START)
        end_idx = original.find(MARKER_END)

        if start_idx == -1 or end_idx == -1 or end_idx < start_idx:
            raise ValueError("Markers missing or invalid.")

        # Locate exact insertion point (next line after start marker)
        start_cut = original.find('\n', start_idx)
        if start_cut == -1: start_cut = start_idx + len(MARKER_START)

        pre = original[:start_cut]
        post = original[end_idx:]

        final_doc = f"{pre}\n\n{full_content_body}\n\n{post}"

        with open(techspec_path, 'w', encoding='utf-8') as f:
            f.write(final_doc)

        print_info("--- Success ---")
        print(f"  Branch: {raw_branch}")
        print(f"  Files:  {len(included_files)}")
        print(f"  Time:   {round(time.time() - start_time, 2)}s")

    except Exception as e:
        print_error(f"Operation failed: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()