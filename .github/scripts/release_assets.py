"""Create or verify checksums for the two portable CLI release archives."""

import argparse
import hashlib
from pathlib import Path


def archive_paths(directory, version):
    return [directory / f"asyncapi-generator-{version}-bin.{suffix}" for suffix in ["tar.gz", "zip"]]


def sha256(path):
    with path.open("rb") as source:
        return hashlib.file_digest(source, "sha256").hexdigest()


def checksum_manifest(directory, version):
    return "".join(f"{sha256(path)}  {path.name}\n" for path in archive_paths(directory, version)).encode("ascii")


def verify_assets(directory, version):
    manifest = directory / "SHA256SUMS"
    if manifest.read_bytes() != checksum_manifest(directory, version):
        raise ValueError("Release archive checksums do not match SHA256SUMS.")
    return [*archive_paths(directory, version), manifest]


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("directory", type=Path)
    parser.add_argument("version")
    parser.add_argument("--write", action="store_true", help="Create SHA256SUMS before retaining build artifacts.")
    args = parser.parse_args()
    if args.write:
        (args.directory / "SHA256SUMS").write_bytes(checksum_manifest(args.directory, args.version))
    verify_assets(args.directory, args.version)
