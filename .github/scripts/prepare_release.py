"""Validate a release's existing tag and draft without modifying GitHub state."""

import json
import os
import re
import subprocess
from pathlib import Path


def release_version(tag):
    match = re.fullmatch(
        r"v(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)"
        r"(?:-([0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*))?",
        tag,
    )
    if not match:
        raise ValueError("Expected an existing tag named vX.Y.Z or vX.Y.Z-prerelease.")
    suffix = match.group(4)
    if suffix and (
        "snapshot" in suffix.lower()
        or any(part.isdigit() and len(part) > 1 and part.startswith("0") for part in suffix.split("."))
    ):
        raise ValueError("Release tags cannot contain SNAPSHOT or numeric prerelease identifiers with leading zeros.")
    return tag[1:], suffix is not None


def find_draft(releases, tag):
    matches = [release for release in releases if release["tag_name"] == tag]
    if len(matches) != 1 or not matches[0]["draft"]:
        raise ValueError(f"Expected exactly one draft GitHub release for {tag}.")
    draft = matches[0]
    if not (draft.get("body") or "").strip():
        raise ValueError("The draft must contain reviewed release notes before dispatch.")
    return draft["id"]


def resolve_release_commit(tag, main_commit):
    result = subprocess.run(
        ["git", "rev-parse", "--verify", f"refs/tags/{tag}^{{commit}}"],
        capture_output=True, text=True,
    )
    if result.returncode:
        raise ValueError(f"Git tag {tag} does not resolve to a commit. Create and push the tag before dispatch.")
    commit = result.stdout.strip()
    result = subprocess.run(["git", "merge-base", "--is-ancestor", commit, main_commit])
    if result.returncode:
        raise ValueError(f"Tag {tag} must belong to main's history at the time of dispatch.")
    return commit


def main():
    if os.environ["GITHUB_REF"] != "refs/heads/main":
        raise ValueError("Run the Release workflow from main.")
    tag = os.environ["RELEASE_TAG"]
    version, prerelease = release_version(tag)
    commit = resolve_release_commit(tag, os.environ["GITHUB_SHA"])
    repository = os.environ["GITHUB_REPOSITORY"]
    # The tag endpoint is for published releases. Listing also exposes drafts
    # to authenticated callers with push access, including older paginated drafts.
    pages = json.loads(subprocess.check_output(
        ["gh", "api", "--method", "GET", "--paginate", "--slurp",
         f"repos/{repository}/releases?per_page=100"], text=True,
    ))
    release_id = find_draft([release for page in pages for release in page], tag)
    metadata = {
        "tag": tag,
        "version": version,
        "prerelease": str(prerelease).lower(),
        "sha": commit,
        "release-id": str(release_id),
    }
    with Path(os.environ["GITHUB_OUTPUT"]).open("a") as output:
        for key, value in metadata.items():
            output.write(f"{key}={value}\n")
    with Path(os.environ["GITHUB_STEP_SUMMARY"]).open("a") as summary:
        summary.write(f"Release `{tag}` at `{commit}`; draft release ID `{release_id}`.\n\n")
        summary.write(f"Version-derived prerelease status: `{metadata['prerelease']}`.\n")


if __name__ == "__main__":
    try:
        main()
    except (ValueError, subprocess.CalledProcessError) as error:
        raise SystemExit(str(error)) from error
