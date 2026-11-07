"""Attach verified assets to an existing draft and publish it as the final gate."""

import hashlib
import json
import os
import subprocess
from pathlib import Path

from prepare_release import find_draft, release_version
from release_assets import sha256, verify_assets


def verify_remote_tag(tag, expected_commit):
    output = subprocess.check_output(
        ["git", "ls-remote", "--tags", "origin", f"refs/tags/{tag}", f"refs/tags/{tag}^{{}}"], text=True,
    )
    refs = {}
    for line in output.splitlines():
        sha, ref = line.split()
        refs[ref] = sha
    actual = refs.get(f"refs/tags/{tag}^{{}}", refs.get(f"refs/tags/{tag}"))
    if actual != expected_commit:
        raise ValueError("The remote release tag changed or disappeared after verification.")


def ensure_asset(repository, release_id, path, existing):
    if existing is None:
        asset = json.loads(subprocess.check_output([
            "gh", "api", "--method", "POST",
            f"https://uploads.github.com/repos/{repository}/releases/{release_id}/assets?name={path.name}",
            "--header", "Content-Type: application/octet-stream", "--input", str(path),
        ]))
    else:
        asset = existing
    if asset["state"] != "uploaded" or asset["size"] != path.stat().st_size:
        raise ValueError(f"Draft asset {path.name} is incomplete or conflicts with the tested asset.")
    downloaded = subprocess.check_output([
        "gh", "api", "--method", "GET", "--header", "Accept: application/octet-stream",
        f"repos/{repository}/releases/assets/{asset['id']}",
    ])
    if hashlib.sha256(downloaded).hexdigest() != sha256(path):
        raise ValueError(f"Draft asset {path.name} differs from the tested asset; inspect it before retrying.")


def finalize(repository, tag, commit, release_id, directory):
    version, prerelease = release_version(tag)
    paths = verify_assets(directory, version)
    endpoint = f"repos/{repository}/releases/{release_id}"
    verify_remote_tag(tag, commit)
    release = json.loads(subprocess.check_output(["gh", "api", "--method", "GET", endpoint]))
    if find_draft([release], tag) != release_id:
        raise ValueError("The draft release identity changed after preparation.")
    pages = json.loads(subprocess.check_output([
        "gh", "api", "--method", "GET", "--paginate", "--slurp", f"{endpoint}/assets?per_page=100",
    ]))
    assets = {asset["name"]: asset for page in pages for asset in page}
    for path in paths:
        ensure_asset(repository, release_id, path, assets.get(path.name))

    # Recheck identity after uploads; leave reviewed notes untouched.
    verify_remote_tag(tag, commit)
    release = json.loads(subprocess.check_output(["gh", "api", "--method", "GET", endpoint]))
    find_draft([release], tag)
    published = json.loads(subprocess.check_output(
        ["gh", "api", "--method", "PATCH", endpoint, "--input", "-"],
        input=json.dumps({"draft": False, "prerelease": prerelease}).encode(),
    ))
    if published["draft"] or published["prerelease"] != prerelease:
        raise ValueError("GitHub did not confirm the requested release state.")
    return published["html_url"]


if __name__ == "__main__":
    try:
        url = finalize(
            os.environ["GITHUB_REPOSITORY"], os.environ["RELEASE_TAG"], os.environ["RELEASE_SHA"],
            int(os.environ["RELEASE_ID"]), Path(os.environ["ASSET_DIRECTORY"]),
        )
        with Path(os.environ["GITHUB_STEP_SUMMARY"]).open("a") as summary:
            summary.write(f"Published release: {url}\n")
    except (ValueError, subprocess.CalledProcessError, OSError) as error:
        raise SystemExit(str(error)) from error
