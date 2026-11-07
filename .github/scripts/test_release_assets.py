import hashlib
import tempfile
import unittest
from pathlib import Path

from release_assets import archive_paths, checksum_manifest, verify_assets


class ReleaseAssetsTest(unittest.TestCase):
    def test_manifest_names_the_exact_archives_with_standard_sha256_format(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            paths = archive_paths(root, "1.2.3")
            for path in paths:
                path.write_bytes(b"archive")
            digest = hashlib.sha256(b"archive").hexdigest()
            expected = (
                f"{digest}  asyncapi-generator-1.2.3-bin.tar.gz\n"
                f"{digest}  asyncapi-generator-1.2.3-bin.zip\n"
            ).encode()
            self.assertEqual(expected, checksum_manifest(root, "1.2.3"))
            manifest = root / "SHA256SUMS"
            manifest.write_bytes(expected)
            self.assertEqual([*paths, manifest], verify_assets(root, "1.2.3"))

    def test_changed_archive_fails_verification(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            for path in archive_paths(root, "1.2.3"):
                path.write_bytes(b"tested")
            (root / "SHA256SUMS").write_bytes(checksum_manifest(root, "1.2.3"))
            archive_paths(root, "1.2.3")[0].write_bytes(b"changed")
            with self.assertRaisesRegex(ValueError, "checksums"):
                verify_assets(root, "1.2.3")
