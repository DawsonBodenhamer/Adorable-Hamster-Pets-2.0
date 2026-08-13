import os
import shutil

# ──────────────────────────────────────────────────────────────────────────────
# Configuration
# ──────────────────────────────────────────────────────────────────────────────

# The directory where Crowdin extracts the downloaded translations
SOURCE_DIR = r"C:\Users\tweek\Downloads\Adorable Hamster Pets (translations)"

# The target directory inside the repository, resolved from this script's location.
REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DEST_DIR = os.path.join(
    REPO_ROOT,
    "common", "src", "main", "resources", "assets", "adorablehamsterpets", "lang"
)

# Maps Crowdin's folder names to Minecraft's primary locale codes.
# If adding a new language in the future; make sure it's in this list.
LANGUAGE_MAPPING = {
    "de": "de_de",
    "es-ES": "es_es",
    "fr": "fr_fr",
    "it": "it_it",
    "ja": "ja_jp",
    "ko": "ko_kr",
    "nl": "nl_nl",
    "pl": "pl_pl",
    "ru": "ru_ru",
    "sv-SE": "sv_se",
    "uk": "uk_ua",
    "zh-CN": "zh_cn",
    "zh-TW": "zh_tw"
}

# Maps a primary locale code to a list of dialects.
# The script will automatically copy the primary file to all of these secondary filenames.
DIALECT_COPIES = {
    "es_es": ["es_ar", "es_cl", "es_ec", "es_mx", "es_uy", "es_ve"]
}

# ──────────────────────────────────────────────────────────────────────────────
# Logic
# ──────────────────────────────────────────────────────────────────────────────

def get_minecraft_locale(crowdin_folder):
    """
    Determines the correct Minecraft locale filename based on the Crowdin folder name.
    """
    # Check the explicit mapping first
    if crowdin_folder in LANGUAGE_MAPPING:
        return LANGUAGE_MAPPING[crowdin_folder]

    # Fallback for future languages: converts 'pt-BR' to 'pt_br' automatically
    return crowdin_folder.replace("-", "_").lower()

def main():
    print(f"--- Updating Language Files ---")

    if not os.path.exists(SOURCE_DIR):
        print(f"ERROR: Source directory not found: {SOURCE_DIR}")
        print("Did you forget to extract the zip file?")
        return

    if not os.path.exists(DEST_DIR):
        print(f"ERROR: Destination directory not found: {DEST_DIR}")
        return

    files_copied = 0
    dialects_copied = 0

    # Iterate through all the language folders inside the Crowdin extraction folder
    for folder_name in os.listdir(SOURCE_DIR):
        folder_path = os.path.join(SOURCE_DIR, folder_name)

        if not os.path.isdir(folder_path):
            continue

        source_file = os.path.join(folder_path, "en_us.json")

        if not os.path.exists(source_file):
            print(f"WARNING: No 'en_us.json' found inside '{folder_name}'. Skipping.")
            continue

        # 1. Determine the primary filename
        primary_locale = get_minecraft_locale(folder_name)
        primary_filename = f"{primary_locale}.json"
        dest_file = os.path.join(DEST_DIR, primary_filename)

        # 2. Copy the primary file
        try:
            shutil.copy2(source_file, dest_file)
            print(f"SUCCESS: {folder_name} -> {primary_filename}")
            files_copied += 1

            # 3. Check for and copy to any associated dialects
            if primary_locale in DIALECT_COPIES:
                for dialect in DIALECT_COPIES[primary_locale]:
                    dialect_filename = f"{dialect}.json"
                    dialect_dest = os.path.join(DEST_DIR, dialect_filename)
                    shutil.copy2(source_file, dialect_dest)
                    print(f"  └─ Copied dialect: -> {dialect_filename}")
                    dialects_copied += 1

        except Exception as e:
            print(f"ERROR: Failed to copy {folder_name}. Reason: {e}")

    print(f"\nDone. {files_copied} primary files and {dialects_copied} dialect copies updated.")

if __name__ == "__main__":
    main()
