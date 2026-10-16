import contextlib
import io
import json
import unittest
from unittest import mock

import xccov_floor

REPORT = {"targets": [{"name": "WPPadelTracker.app", "lineCoverage": 0.8765, "coveredLines": 877, "executableLines": 1000},
                      {"name": "WPPadelTrackerTests.xctest", "lineCoverage": 0.99}]}


class CheckTest(unittest.TestCase):

    def test_line_coverage_is_the_targets_share_in_percent(self):
        self.assertAlmostEqual(87.65, xccov_floor.line_coverage(REPORT, "WPPadelTracker.app"))
        self.assertIsNone(xccov_floor.line_coverage(REPORT, "WPPadelBoard.app"))
        self.assertIsNone(xccov_floor.line_coverage({}, "WPPadelBoard.app"))

    def test_meeting_the_floor_prints_the_value_and_passes(self):
        out = io.StringIO()
        with contextlib.redirect_stdout(out):
            self.assertIsNone(xccov_floor.check(REPORT, "WPPadelTracker.app", 87.6))
        self.assertEqual("WPPadelTracker.app: 87.6% of lines covered (floor 87.6%)\n", out.getvalue())

    def test_falling_below_the_floor_is_a_problem(self):
        with contextlib.redirect_stdout(io.StringIO()):
            self.assertEqual("WPPadelTracker.app line coverage 87.6% is below the floor of 90%",
                             xccov_floor.check(REPORT, "WPPadelTracker.app", 90))

    def test_a_missing_target_is_a_problem_that_lists_the_targets(self):
        self.assertEqual("WPPadelBoard.app is not in the coverage report; its targets are ['WPPadelTracker.app', 'WPPadelTrackerTests.xctest']",
                         xccov_floor.check(REPORT, "WPPadelBoard.app", 50))


class MainTest(unittest.TestCase):

    def run_main(self, argv, report=REPORT, returncode=0, stderr=""):
        completed = mock.Mock(stdout=json.dumps(report), returncode=returncode, stderr=stderr)
        out, err = io.StringIO(), io.StringIO()
        with mock.patch("subprocess.run", return_value=completed) as run, \
                contextlib.redirect_stdout(out), contextlib.redirect_stderr(err):
            code = xccov_floor.main(["xccov_floor.py", *argv])
        return code, out.getvalue(), err.getvalue(), run

    def test_reads_the_bundle_with_xccov_and_passes_at_the_floor(self):
        code, out, err, run = self.run_main(["build/WPPadelTracker.xcresult", "WPPadelTracker.app", "87"])
        self.assertEqual(0, code)
        self.assertEqual("", err)
        self.assertIn("87.6%", out)
        self.assertEqual(["xcrun", "xccov", "view", "--report", "--json", "build/WPPadelTracker.xcresult"], run.call_args.args[0])

    def test_fails_below_the_floor(self):
        code, _, err, _ = self.run_main(["build/WPPadelTracker.xcresult", "WPPadelTracker.app", "88"])
        self.assertEqual(1, code)
        self.assertIn("below the floor of 88%", err)

    def test_a_bundle_without_coverage_fails_with_the_xccov_error(self):
        code, out, err, _ = self.run_main(["build/WPPadelTracker.xcresult", "WPPadelTracker.app", "80"],
                                          returncode=1, stderr="Error: No coverage data in result bundle\n")
        self.assertEqual(1, code)
        self.assertEqual("", out)
        self.assertEqual("cannot read the coverage of build/WPPadelTracker.xcresult: Error: No coverage data in result bundle\n", err)

    def test_bad_usage_exits_2(self):
        for argv in [[], ["a.xcresult", "T.app"], ["a.xcresult", "T.app", "high"]]:
            with self.subTest(argv=argv):
                code, _, err, run = self.run_main(argv)
                self.assertEqual(2, code)
                self.assertEqual(xccov_floor.USAGE + "\n", err)
                run.assert_not_called()


if __name__ == "__main__":
    unittest.main()
