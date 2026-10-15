#!/usr/bin/env python3
"""Propagates app.json to the files that cannot read it.

  python3 tool/app_config.py apply   writes code/apple/Config/App.xcconfig
  python3 tool/app_config.py check   fails if that file is stale or app.json is invalid
Exit codes: 0 ok, 1 invalid or stale config, 2 bad usage.
Gradle reads app.json itself, so Android needs no generated file.
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CONFIG = ROOT / "app.json"
XCCONFIG = ROOT / "code" / "apple" / "Config" / "App.xcconfig"
PROJECT = ROOT / "code" / "apple" / "WPPadel.xcodeproj" / "project.pbxproj"

USAGE = "usage: app_config.py {apply|check}"
ID = re.compile(r"^[a-z][a-z0-9]*(\.[a-z][a-z0-9]*)+$")
TEAM = re.compile(r"^[A-Z0-9]{10}$")
VERSION_NAME = re.compile(r"^\d+\.\d+\.\d+$")
MAX_VERSION_CODE = 2_100_000_000  # Google Play's limit
IDS = ["apple.phone.bundleId", "apple.watch.bundleId", "android.phone.applicationId", "android.watch.applicationId"]
STRINGS = ["version.name", "apple.team", "apple.phone.name", "apple.watch.name", "android.phone.name", "android.watch.name", *IDS]
REQUIRED = [*STRINGS, "version.build", "android.phone.versionCodeOffset", "android.watch.versionCodeOffset"]


class ConfigError(Exception):
    """app.json cannot be read at all."""


def get(config, path):
    """The value at a dotted path such as 'apple.phone.bundleId', or None when any part is missing."""
    for key in path.split("."):
        if not isinstance(config, dict) or key not in config:
            return None
        config = config[key]
    return config


def is_int(value):
    return isinstance(value, int) and not isinstance(value, bool)


def android_version_code(config, app):
    """The versionCode Gradle gives android.<app>: build * 10 + its versionCodeOffset."""
    return config["version"]["build"] * 10 + config["android"][app]["versionCodeOffset"]


def load():
    """app.json as a dict, or a ConfigError naming what is wrong with the file."""
    try:
        config = json.loads(CONFIG.read_text())
    except FileNotFoundError:
        raise ConfigError(f"{CONFIG} not found") from None
    except json.JSONDecodeError as e:
        raise ConfigError(f"{CONFIG} is not valid JSON: {e}") from None
    if not isinstance(config, dict):
        raise ConfigError(f"{CONFIG} must hold a JSON object")
    return config


def validate(config):
    """Returns a list of problems; empty when the config is usable."""
    problems = [f"{key} is missing" for key in REQUIRED if get(config, key) is None]
    problems += [f"{key} must be a non-empty string" for key in STRINGS
                 if get(config, key) is not None and not (isinstance(get(config, key), str) and get(config, key))]
    if problems:
        return problems
    problems += [f"{key} '{get(config, key)}' is not a lowercase reverse-domain id" for key in IDS if not ID.match(get(config, key))]
    if not TEAM.match(get(config, "apple.team")):
        problems.append(f"apple.team '{get(config, 'apple.team')}' must be 10 uppercase letters or digits (the Apple team ID)")
    if not get(config, "apple.watch.bundleId").startswith(get(config, "apple.phone.bundleId") + "."):
        problems.append("apple.watch.bundleId must start with apple.phone.bundleId + '.' (Xcode companion rule)")
    if get(config, "android.phone.applicationId") != get(config, "android.watch.applicationId"):
        problems.append("android.phone.applicationId must equal android.watch.applicationId (Wear Data Layer rule)")
    if not VERSION_NAME.match(get(config, "version.name")):
        problems.append("version.name must look like 1.2.3")
    build = get(config, "version.build")
    if not is_int(build) or build <= 0:
        problems.append("version.build must be a positive integer")
        return problems
    offsets = [get(config, f"android.{app}.versionCodeOffset") for app in ("phone", "watch")]
    if not all(is_int(o) and 0 <= o <= 9 for o in offsets):
        problems.append("android.<app>.versionCodeOffset must be a digit 0-9")
    elif offsets[0] == offsets[1]:
        problems.append("android.phone and android.watch need different versionCodeOffsets (one Play app, one versionCode per bundle)")
    elif max(android_version_code(config, app) for app in ("phone", "watch")) > MAX_VERSION_CODE:
        problems.append(f"version.build * 10 must stay below Google Play's versionCode limit {MAX_VERSION_CODE}")
    return problems


def xcconfig(config):
    apple = config["apple"]
    lines = [
        "// GENERATED from app.json by tool/app_config.py apply. Do not edit.",
        f"DEVELOPMENT_TEAM = {apple['team']}",
        f"MARKETING_VERSION = {config['version']['name']}",
        f"CURRENT_PROJECT_VERSION = {config['version']['build']}",
        f"PHONE_BUNDLE_ID = {apple['phone']['bundleId']}",
        f"PHONE_DISPLAY_NAME = {apple['phone']['name']}",
        f"WATCH_BUNDLE_ID = {apple['watch']['bundleId']}",
        f"WATCH_DISPLAY_NAME = {apple['watch']['name']}",
    ]
    return "\n".join(lines) + "\n"


def stray_versions(project_text):
    """Xcode writes Version/Build into the project when edited in the General tab; that overrides app.json."""
    return re.findall(r"(?:MARKETING_VERSION|CURRENT_PROJECT_VERSION|DEVELOPMENT_TEAM|PRODUCT_BUNDLE_IDENTIFIER) = [^$\";]+;", project_text)


def run(command):
    """Runs apply or check and returns the problems; an empty list means success."""
    config = load()
    problems = validate(config)
    if problems:
        return problems
    if command == "apply":
        XCCONFIG.write_text(xcconfig(config))
        print(f"wrote {XCCONFIG.relative_to(ROOT)}")
    elif not XCCONFIG.exists() or XCCONFIG.read_text() != xcconfig(config):
        problems.append("code/apple/Config/App.xcconfig is stale: run `python3 tool/app_config.py apply`")
    if PROJECT.exists():
        problems += [f"Xcode project hard-codes {s} (set it in app.json)" for s in stray_versions(PROJECT.read_text())]
    return problems


def main(argv):
    if len(argv) != 2 or argv[1] not in ("apply", "check"):
        print(USAGE, file=sys.stderr)
        return 2
    try:
        problems = run(argv[1])
    except ConfigError as e:
        problems = [str(e)]
    for p in problems:
        print(f"app.json: {p}", file=sys.stderr)
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
