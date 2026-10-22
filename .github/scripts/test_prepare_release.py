import os
import subprocess
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from prepare_release import find_draft, main, release_version, resolve_release_commit


class ReleaseMetadataTest(unittest.TestCase):
    def test_stable_and_prerelease_versions(self):
        self.assertEqual(("1.2.3", False), release_version("v1.2.3"))
        self.assertEqual(("0.3.5-BETA", True), release_version("v0.3.5-BETA"))
        self.assertEqual(("1.0.0-rc.1", True), release_version("v1.0.0-rc.1"))

    def test_rejects_unsupported_or_ambiguous_versions(self):
        for tag in ["1.2.3", "v01.2.3", "v1.2.3-rc.01", "v1.2.3-SNAPSHOT", "v1.2.3+build", "v1.2.3\n"]:
            with self.subTest(tag=tag), self.assertRaises(ValueError):
                release_version(tag)

    def test_finds_matching_draft_without_trusting_its_prerelease_toggle(self):
        self.assertEqual(42, find_draft([
            {"id": 41, "tag_name": "v0.3.4-BETA", "draft": False},
            {"id": 42, "tag_name": "v0.3.5-BETA", "draft": True, "prerelease": False, "body": "Reviewed notes."},
        ], "v0.3.5-BETA"))

    def test_rejects_missing_or_published_release_and_empty_notes(self):
        for releases in [
            [],
            [{"id": 42, "tag_name": "v1.2.3", "draft": False, "body": "Published."}],
            [{"id": 42, "tag_name": "v1.2.3", "draft": True, "body": " \n"}],
        ]:
            with self.subTest(releases=releases), self.assertRaises(ValueError):
                find_draft(releases, "v1.2.3")

    def test_prepares_metadata_from_an_older_paginated_draft(self):
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / "output"
            summary = Path(directory) / "summary"
            environment = {
                "GITHUB_REF": "refs/heads/main", "GITHUB_SHA": "dispatch-commit",
                "GITHUB_REPOSITORY": "example/generator", "RELEASE_TAG": "v0.3.5-BETA",
                "GITHUB_OUTPUT": str(output), "GITHUB_STEP_SUMMARY": str(summary),
            }
            pages = '[[{"tag_name":"v1.0.0","draft":false}],[{"id":42,"tag_name":"v0.3.5-BETA","draft":true,"body":"Reviewed notes."}]]'
            with patch.dict(os.environ, environment), \
                    patch("prepare_release.resolve_release_commit", return_value="release-commit"), \
                    patch("prepare_release.subprocess.check_output", return_value=pages):
                main()
            self.assertEqual(
                "tag=v0.3.5-BETA\nversion=0.3.5-BETA\nprerelease=true\nsha=release-commit\nrelease-id=42\n",
                output.read_text(),
            )

    def test_rejects_dispatch_from_another_branch(self):
        with patch.dict(os.environ, {"GITHUB_REF": "refs/heads/feature"}), \
                self.assertRaisesRegex(ValueError, "from main"):
            main()


class ReleaseTagTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.original_directory = os.getcwd()
        cls.repository = tempfile.TemporaryDirectory()
        os.chdir(cls.repository.name)
        subprocess.run(["git", "init", "--quiet", "--initial-branch=main"], check=True)
        subprocess.run(["git", "config", "user.name", "Release test"], check=True)
        subprocess.run(["git", "config", "user.email", "release-test@example.invalid"], check=True)
        subprocess.run(["git", "-c", "commit.gpgsign=false", "commit", "--quiet", "--allow-empty", "-m", "Initial"], check=True)
        cls.first_commit = subprocess.check_output(["git", "rev-parse", "HEAD"], text=True).strip()
        subprocess.run(["git", "-c", "tag.gpgsign=false", "tag", "v1.0.0"], check=True)
        subprocess.run(["git", "-c", "tag.gpgsign=false", "tag", "-a", "v1.0.1", "-m", "Annotated"], check=True)
        subprocess.run(["git", "-c", "commit.gpgsign=false", "commit", "--quiet", "--allow-empty", "-m", "Next"], check=True)
        cls.main_commit = subprocess.check_output(["git", "rev-parse", "HEAD"], text=True).strip()

    @classmethod
    def tearDownClass(cls):
        os.chdir(cls.original_directory)
        cls.repository.cleanup()

    def test_resolves_lightweight_and_annotated_tags_in_main_history(self):
        for tag in ["v1.0.0", "v1.0.1"]:
            with self.subTest(tag=tag):
                self.assertEqual(self.first_commit, resolve_release_commit(tag, self.main_commit))

    def test_rejects_missing_tag_even_when_a_draft_could_name_it(self):
        with self.assertRaisesRegex(ValueError, "Create and push the tag"):
            resolve_release_commit("v9.9.9", self.main_commit)

    def test_rejects_commit_outside_dispatch_main_history(self):
        subprocess.run(["git", "-c", "tag.gpgsign=false", "tag", "v1.0.2", self.main_commit], check=True)
        with self.assertRaisesRegex(ValueError, "main's history"):
            resolve_release_commit("v1.0.2", self.first_commit)


if __name__ == "__main__":
    unittest.main()
