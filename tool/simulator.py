#!/usr/bin/env python3
"""Prints the UDID of a simulator on the newest installed runtime, for `xcodebuild -destination id=…`.
On watchOS an unpaired watch comes first (see README, Testing). See USAGE for the platforms.
"""
import json
import re
import subprocess
import sys

DEVICES = {"iOS": "iPhone", "watchOS": "Apple Watch"}
USAGE = f"""usage: simulator.py {{{'|'.join(DEVICES)}}}
  iOS       an iPhone
  watchOS   an Apple Watch, unpaired when there is one
exit codes: 0 found, 1 no such simulator, 2 bad usage"""
RUNTIME = re.compile(r"\.SimRuntime\.(\w+)-([\d-]+)$")


def runtime_version(key, platform):
    """(26, 0) for 'com.apple.CoreSimulator.SimRuntime.iOS-26-0' on iOS, None for another platform."""
    match = RUNTIME.search(key)
    if not match or match.group(1) != platform:
        return None
    return tuple(int(part) for part in match.group(2).split("-"))


def pick(devices, platform, paired=frozenset()):
    """The first available device of the platform's kind on its newest runtime, an unpaired one first; None when there is none."""
    candidates = [(version, device) for key, runtime in devices.items()
                  if (version := runtime_version(key, platform)) is not None
                  for device in runtime
                  if device.get("isAvailable") and device["name"].startswith(DEVICES[platform])]
    if not candidates:
        return None
    newest = max(version for version, _ in candidates)
    on_newest = [device for version, device in candidates if version == newest]
    return next((device for device in on_newest if device["udid"] not in paired), on_newest[0])


def paired_watches(pairs):
    """The UDIDs of the watches in `simctl list pairs`."""
    return {pair["watch"]["udid"] for pair in pairs.values()}


def simctl_list(what):
    """The JSON of `xcrun simctl list <what> --json`."""
    listing = subprocess.run(["xcrun", "simctl", "list", *what, "--json"], check=True, capture_output=True, text=True).stdout
    return json.loads(listing)


def main(argv):
    if len(argv) != 2 or argv[1] not in DEVICES:
        print(USAGE, file=sys.stderr)
        return 2
    devices = simctl_list(["devices", "available"])["devices"]
    paired = paired_watches(simctl_list(["pairs"])["pairs"]) if argv[1] == "watchOS" else frozenset()
    device = pick(devices, argv[1], paired)
    if device is None:
        print(f"no {DEVICES[argv[1]]} simulator is installed", file=sys.stderr)
        return 1
    print(device["udid"])
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
