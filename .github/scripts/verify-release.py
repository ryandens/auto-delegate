"""Validate release identity before any publishing credentials are available."""

import os
from pathlib import Path
import re
import subprocess

tag = os.environ["GITHUB_REF_NAME"]
if os.environ.get("GITHUB_REF_TYPE") != "tag" or not re.fullmatch(
    r"v(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)", tag
):
    raise SystemExit("Releases require a stable vMAJOR.MINOR.PATCH tag.")

versions = re.findall(r"^version=(\S+)$", Path("gradle.properties").read_text(), re.M)
if versions != [tag[1:]]:
    raise SystemExit("The tag must match the single version= entry in gradle.properties.")

subprocess.run(["git", "merge-base", "--is-ancestor", "HEAD", "origin/main"], check=True)
print(f"Verified {tag}: version matches and commit belongs to main.")
