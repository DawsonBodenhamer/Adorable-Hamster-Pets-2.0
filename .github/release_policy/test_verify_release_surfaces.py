import tempfile
import unittest
from pathlib import Path

import verify_release_surfaces


class ReleaseSurfacePolicyTest(unittest.TestCase):
    def setUp(self) -> None:
        self.temp_dir = tempfile.TemporaryDirectory()
        self.publisher = Path(self.temp_dir.name) / "publish.yml"
        self.publisher.write_bytes(Path(".github/workflows/publish.yml").read_bytes())

    def tearDown(self) -> None:
        self.temp_dir.cleanup()

    def test_accepts_reviewed_tombstone_without_feed_changes(self) -> None:
        self.assertEqual([], verify_release_surfaces.validate(self.publisher, [".github/CODEOWNERS"]))

    def test_rejects_modified_publisher(self) -> None:
        self.publisher.write_text("on: push\n", encoding="utf-8")
        errors = verify_release_surfaces.validate(self.publisher, [])
        self.assertTrue(any("not the reviewed inert tombstone" in error for error in errors))

    def test_rejects_live_feed_change(self) -> None:
        errors = verify_release_surfaces.validate(
            self.publisher,
            ["announcements/manifest.json", "common/src/main/java/Example.java"],
        )
        self.assertTrue(any("Feed Publisher identity" in error for error in errors))


if __name__ == "__main__":
    unittest.main()
