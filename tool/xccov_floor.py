#!/usr/bin/env python3
"""Fails when a target's line coverage in an xcresult bundle is below a floor (the Apple twin of koverVerify).
Needs `xcodebuild test -enableCodeCoverage YES`. See USAGE.
"""
import json
import subprocess
import sys

USAGE = """usage: xccov_floor.py <bundle.xcresult> <Target.app> <min-line-percent>
  after `xcodebuild test … -enableCodeCoverage YES -resultBundlePath <bundle.xcresult>`
exit codes: 0 at or above the floor, 1 below or no such target, 2 bad usage"""


def line_coverage(report, target):
    """The target's covered lines as a percentage (0-100), or None when the report has no such target."""
    for entry in report.get("targets", []):
        if entry.get("name") == target:
            return 100 * entry["lineCoverage"]
    return None


def check(report, target, floor):
    """A problem to print, or None when the target meets the floor."""
    coverage = line_coverage(report, target)
    if coverage is None:
        return f"{target} is not in the coverage report; its targets are {[t.get('name') for t in report.get('targets', [])]}"
    print(f"{target}: {coverage:.1f}% of lines covered (floor {floor:g}%)")
    if coverage < floor:
        return f"{target} line coverage {coverage:.1f}% is below the floor of {floor:g}%"
    return None


def main(argv):
    if len(argv) != 4:
        print(USAGE, file=sys.stderr)
        return 2
    bundle, target, floor = argv[1], argv[2], argv[3]
    try:
        floor = float(floor)
    except ValueError:
        print(USAGE, file=sys.stderr)
        return 2
    xccov = subprocess.run(["xcrun", "xccov", "view", "--report", "--json", bundle], capture_output=True, text=True)
    if xccov.returncode != 0:
        # No coverage data (test run without -enableCodeCoverage YES) or an unfinished bundle
        problem = f"cannot read the coverage of {bundle}: {xccov.stderr.strip()}"
    else:
        problem = check(json.loads(xccov.stdout), target, floor)
    if problem:
        print(problem, file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
