#!/usr/bin/env python3
"""Prints the UDID of a simulator on the newest installed runtime, for `xcodebuild -destination id=…`.

  python3 tool/simulator.py iOS       an iPhone
  python3 tool/simulator.py watchOS   an Apple Watch
Exit codes: 0 found, 1 no such simulator, 2 bad usage.
"""
import json
import re
import subprocess
import sys

DEVICES = {"iOS": "iPhone", "watchOS": "Apple Watch"}
USAGE = f"usage: simulator.py {{{'|'.join(DEVICES)}}}"
RUNTIME = re.compile(r"\.SimRuntime\.(\w+)-([\d-]+)$")


def runtime_version(key, platform):
    """(26, 0) for 'com.apple.CoreSimulator.SimRuntime.iOS-26-0' on iOS, None for another platform."""
    match = RUNTIME.search(key)
    if not match or match.group(1) != platform:
        return None
    return tuple(int(part) for part in match.group(2).split("-"))


def pick(devices, platform):
    """The first available device of the platform's kind on its newest runtime, or None."""
    candidates = [(version, device) for key, runtime in devices.items()
                  if (version := runtime_version(key, platform)) is not None
                  for device in runtime
                  if device.get("isAvailable") and device["name"].startswith(DEVICES[platform])]
    if not candidates:
        return None
    newest = max(version for version, _ in candidates)
    return next(device for version, device in candidates if version == newest)


def main(argv):
    if len(argv) != 2 or argv[1] not in DEVICES:
        print(USAGE, file=sys.stderr)
        return 2
    listing = subprocess.run(["xcrun", "simctl", "list", "devices", "available", "--json"],
                             check=True, capture_output=True, text=True).stdout
    device = pick(json.loads(listing)["devices"], argv[1])
    if device is None:
        print(f"no {DEVICES[argv[1]]} simulator is installed", file=sys.stderr)
        return 1
    print(device["udid"])
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
