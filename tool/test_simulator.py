import contextlib
import io
import json
import unittest
from unittest import mock

import simulator

IOS = "com.apple.CoreSimulator.SimRuntime.iOS-"
WATCH = "com.apple.CoreSimulator.SimRuntime.watchOS-"


def device(name, udid, available=True):
    return {"name": name, "udid": udid, "isAvailable": available}


class PickTest(unittest.TestCase):

    def test_runtime_version_reads_the_platform_and_version(self):
        self.assertEqual((26, 0), simulator.runtime_version(IOS + "26-0", "iOS"))
        self.assertEqual((12, 1, 2), simulator.runtime_version(WATCH + "12-1-2", "watchOS"))
        self.assertIsNone(simulator.runtime_version(WATCH + "12-0", "iOS"))
        self.assertIsNone(simulator.runtime_version("garbage", "iOS"))

    def test_picks_from_the_newest_runtime(self):
        devices = {IOS + "18-6": [device("iPhone 16", "old")],
                   IOS + "26-10": [device("iPhone 18 Pro", "newest")],
                   IOS + "26-2": [device("iPhone 17", "older")]}
        self.assertEqual("newest", simulator.pick(devices, "iOS")["udid"])

    def test_skips_other_kinds_and_unavailable_devices(self):
        devices = {IOS + "26-0": [device("iPad Pro", "ipad"), device("iPhone 18", "gone", available=False),
                                  device("iPhone 18 Pro", "phone")],
                   WATCH + "12-0": [device("Apple Watch Ultra 4", "watch")]}
        self.assertEqual("phone", simulator.pick(devices, "iOS")["udid"])
        self.assertEqual("watch", simulator.pick(devices, "watchOS")["udid"])

    def test_a_newer_runtime_without_a_usable_device_is_ignored(self):
        devices = {IOS + "27-0": [device("iPad Air", "ipad")], IOS + "26-0": [device("iPhone 18", "phone")]}
        self.assertEqual("phone", simulator.pick(devices, "iOS")["udid"])

    def test_nothing_installed(self):
        self.assertIsNone(simulator.pick({IOS + "26-0": [device("iPhone 18", "phone")]}, "watchOS"))


class MainTest(unittest.TestCase):

    def run_main(self, args, devices=None):
        listing = mock.Mock(stdout=json.dumps({"devices": devices or {}}))
        out, err = io.StringIO(), io.StringIO()
        with mock.patch.object(simulator.subprocess, "run", return_value=listing) as run, \
                contextlib.redirect_stdout(out), contextlib.redirect_stderr(err):
            code = simulator.main(["simulator.py", *args])
        return code, out.getvalue(), err.getvalue(), run

    def test_prints_the_udid(self):
        code, out, err, run = self.run_main(["watchOS"], {WATCH + "12-0": [device("Apple Watch Series 12 (46mm)", "W1")]})
        self.assertEqual((0, "W1\n", ""), (code, out, err))
        self.assertEqual(["xcrun", "simctl", "list", "devices", "available", "--json"], run.call_args.args[0])

    def test_fails_when_no_simulator_matches(self):
        code, out, err, _ = self.run_main(["iOS"])
        self.assertEqual((1, "", "no iPhone simulator is installed\n"), (code, out, err))

    def test_bad_usage_prints_usage_without_listing(self):
        for args in [[], ["tvOS"], ["iOS", "extra"]]:
            with self.subTest(args=args):
                code, out, err, run = self.run_main(args)
                self.assertEqual((2, "", simulator.USAGE + "\n"), (code, out, err))
                run.assert_not_called()


if __name__ == "__main__":
    unittest.main()
