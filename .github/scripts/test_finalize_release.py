import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from finalize_release import ensure_asset, finalize, verify_remote_tag
from release_assets import archive_paths, checksum_manifest


class FinalizeReleaseTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name)

    def write_assets(self, version):
        for path in archive_paths(self.root, version):
            path.write_bytes(b"tested archive")
        (self.root / "SHA256SUMS").write_bytes(checksum_manifest(self.root, version))

    def test_resolves_remote_lightweight_and_annotated_tags(self):
        for output in ["commit\trefs/tags/v1.2.3\n", "tag-object\trefs/tags/v1.2.3\ncommit\trefs/tags/v1.2.3^{}\n"]:
            with self.subTest(output=output), patch("finalize_release.subprocess.check_output", return_value=output):
                verify_remote_tag("v1.2.3", "commit")

    def test_rejects_moved_or_deleted_tag(self):
        for output in ["other-commit\trefs/tags/v1.2.3\n", ""]:
            with self.subTest(output=output), patch("finalize_release.subprocess.check_output", return_value=output), \
                    self.assertRaisesRegex(ValueError, "changed or disappeared"):
                verify_remote_tag("v1.2.3", "commit")

    def test_reuses_only_identical_uploaded_assets(self):
        path = self.root / "archive.zip"
        path.write_bytes(b"tested")
        existing = {"id": 7, "state": "uploaded", "size": 6}
        with patch("finalize_release.subprocess.check_output", return_value=b"tested") as request:
            ensure_asset("example/generator", 42, path, existing)
            self.assertEqual("GET", request.call_args.args[0][3])
        with patch("finalize_release.subprocess.check_output", return_value=b"change"), \
                self.assertRaisesRegex(ValueError, "differs from the tested asset"):
            ensure_asset("example/generator", 42, path, existing)

    def test_upload_uses_asset_endpoint_and_verifies_downloaded_bytes(self):
        path = self.root / "archive.zip"
        path.write_bytes(b"tested")
        metadata = json.dumps({"id": 7, "state": "uploaded", "size": 6}).encode()
        with patch("finalize_release.subprocess.check_output", side_effect=[metadata, b"tested"]) as request:
            ensure_asset("example/generator", 42, path, None)
            command = request.call_args_list[0].args[0]
            self.assertIn("https://uploads.github.com/repos/example/generator/releases/42/assets?name=archive.zip", command)
            self.assertEqual(str(path), command[-1])

    def test_publishes_last_and_derives_prerelease_without_overwriting_notes(self):
        for version, prerelease in [("1.2.3", False), ("1.2.3-BETA", True)]:
            with self.subTest(version=version):
                self.write_assets(version)
                draft = {"id": 42, "tag_name": f"v{version}", "draft": True, "body": "Reviewed notes."}
                published = {"draft": False, "prerelease": prerelease, "html_url": "release-url"}
                def response(command, **kwargs):
                    if command[3] == "PATCH":
                        self.assertEqual(3, assets.call_count)
                        return json.dumps(published)
                    if "--paginate" in command:
                        return "[[]]"
                    return json.dumps(draft)

                with patch("finalize_release.verify_remote_tag"), \
                        patch("finalize_release.ensure_asset") as assets, \
                        patch("finalize_release.subprocess.check_output", side_effect=response) as request:
                    self.assertEqual("release-url", finalize("example/generator", f"v{version}", "commit", 42, self.root))
                    self.assertEqual(3, assets.call_count)
                    self.assertEqual("PATCH", request.call_args.args[0][3])
                    self.assertEqual({"draft": False, "prerelease": prerelease}, json.loads(request.call_args.kwargs["input"]))

    def test_asset_failure_prevents_publication(self):
        self.write_assets("1.2.3")
        draft = {"id": 42, "tag_name": "v1.2.3", "draft": True, "body": "Reviewed notes."}
        with patch("finalize_release.verify_remote_tag"), \
                patch("finalize_release.ensure_asset", side_effect=ValueError("conflicting asset")), \
                patch("finalize_release.subprocess.check_output", side_effect=[json.dumps(draft), "[[]]"]) as request, \
                self.assertRaisesRegex(ValueError, "conflicting asset"):
            finalize("example/generator", "v1.2.3", "commit", 42, self.root)
        self.assertTrue(all(call.args[0][3] == "GET" for call in request.call_args_list))
