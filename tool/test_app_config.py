import contextlib
import copy
import io
import json
import tempfile
import unittest
from pathlib import Path
from unittest import mock

import app_config


def edited(config, path, edit):
    """A copy of config with edit(parent, key) applied at a dotted path."""
    c = copy.deepcopy(config)
    *parents, last = path.split(".")
    node = c
    for key in parents:
        node = node[key]
    edit(node, last)
    return c


def without(config, path):
    return edited(config, path, dict.pop)


class ValidateTest(unittest.TestCase):

    def setUp(self):
        self.config = app_config.load()

    def changed(self, path, value):
        return edited(self.config, path, lambda node, key: node.__setitem__(key, value))

    def test_the_repository_config_is_valid_and_applied(self):
        self.assertEqual([], app_config.validate(self.config))
        self.assertEqual(app_config.xcconfig(self.config), app_config.XCCONFIG.read_text())
        self.assertEqual([], app_config.stray_versions(app_config.PROJECT.read_text()))

    def test_every_missing_key_is_named(self):
        for key in app_config.REQUIRED:
            with self.subTest(key=key):
                self.assertEqual([f"{key} is missing"], app_config.validate(without(self.config, key)))

    def test_a_missing_section_names_each_key_under_it(self):
        problems = app_config.validate(without(self.config, "apple"))
        self.assertEqual(sorted(f"{k} is missing" for k in app_config.REQUIRED if k.startswith("apple.")), sorted(problems))

    def test_strings_must_be_non_empty_strings(self):
        for key in app_config.STRINGS:
            for value in ["", 42]:
                with self.subTest(key=key, value=value):
                    self.assertEqual([f"{key} must be a non-empty string"], app_config.validate(self.changed(key, value)))

    def test_ids_must_be_reverse_domain(self):
        c = self.changed("android.phone.applicationId", "Not An Id")
        c["android"]["watch"]["applicationId"] = "Not An Id"
        self.assertEqual(["android.phone.applicationId 'Not An Id' is not a lowercase reverse-domain id",
                          "android.watch.applicationId 'Not An Id' is not a lowercase reverse-domain id"],
                         app_config.validate(c))

    def test_team_must_be_an_apple_team_id(self):
        for team in ["9p2t82qgn8", "9P2T82QGN", "9P2T82QGN8X", "9P2T-2QGN8"]:
            with self.subTest(team=team):
                self.assertEqual([f"apple.team '{team}' must be 10 uppercase letters or digits (the Apple team ID)"],
                                 app_config.validate(self.changed("apple.team", team)))
        self.assertEqual([], app_config.validate(self.changed("apple.team", "ABCDE12345")))

    def test_watch_id_must_extend_the_phone_id(self):
        self.assertEqual(["apple.watch.bundleId must start with apple.phone.bundleId + '.' (Xcode companion rule)"],
                         app_config.validate(self.changed("apple.watch.bundleId", "ch.other.watch")))

    def test_android_apps_must_share_one_id(self):
        self.assertEqual(["android.phone.applicationId must equal android.watch.applicationId (Wear Data Layer rule)"],
                         app_config.validate(self.changed("android.phone.applicationId", "ch.other.phone")))

    def test_version_name_must_have_three_parts(self):
        for name in ["1.3", "1.3.0-beta", "v1.3.0"]:
            with self.subTest(name=name):
                self.assertEqual(["version.name must look like 1.2.3"], app_config.validate(self.changed("version.name", name)))

    def test_version_build_must_be_a_positive_integer(self):
        for build in [0, -1, 1.5, "20261012", True]:
            with self.subTest(build=build):
                self.assertEqual(["version.build must be a positive integer"], app_config.validate(self.changed("version.build", build)))

    def test_version_code_offsets_must_be_digits(self):
        for offset in [10, -1, "1", True]:
            with self.subTest(offset=offset):
                self.assertEqual(["android.<app>.versionCodeOffset must be a digit 0-9"],
                                 app_config.validate(self.changed("android.phone.versionCodeOffset", offset)))

    def test_android_apps_get_different_version_codes(self):
        same = self.changed("android.phone.versionCodeOffset", self.config["android"]["watch"]["versionCodeOffset"])
        self.assertEqual(["android.phone and android.watch need different versionCodeOffsets (one Play app, one versionCode per bundle)"],
                         app_config.validate(same))

    def test_version_code_stays_below_the_play_limit(self):
        self.assertEqual([f"version.build * 10 must stay below Google Play's versionCode limit {app_config.MAX_VERSION_CODE}"],
                         app_config.validate(self.changed("version.build", 210_000_000)))
        self.assertEqual([], app_config.validate(self.changed("version.build", 209_999_999)))

    def test_version_code_is_build_times_ten_plus_offset(self):
        c = self.changed("version.build", 7)
        self.assertEqual(71, app_config.android_version_code(c, "phone"))
        self.assertEqual(72, app_config.android_version_code(c, "watch"))


class XcconfigTest(unittest.TestCase):

    def test_xcconfig_carries_every_value(self):
        config = app_config.load()
        self.assertEqual([
            "// GENERATED from app.json by tool/app_config.py apply. Do not edit.",
            f"DEVELOPMENT_TEAM = {config['apple']['team']}",
            f"MARKETING_VERSION = {config['version']['name']}",
            f"CURRENT_PROJECT_VERSION = {config['version']['build']}",
            f"PHONE_BUNDLE_ID = {config['apple']['phone']['bundleId']}",
            f"PHONE_DISPLAY_NAME = {config['apple']['phone']['name']}",
            f"WATCH_BUNDLE_ID = {config['apple']['watch']['bundleId']}",
            f"WATCH_DISPLAY_NAME = {config['apple']['watch']['name']}",
        ], app_config.xcconfig(config).splitlines())

    def test_stray_versions_spots_hard_coded_values(self):
        self.assertEqual(["MARKETING_VERSION = 1.0;", "DEVELOPMENT_TEAM = ABCDE12345;"],
                         app_config.stray_versions("MARKETING_VERSION = 1.0;\nDEVELOPMENT_TEAM = ABCDE12345;"))

    def test_stray_versions_spots_quoted_values_too(self):
        # Xcode quotes a value with a hyphen or an empty string (a cleared team)
        quoted = 'MARKETING_VERSION = "1.0-beta";\nCURRENT_PROJECT_VERSION = "7";\nDEVELOPMENT_TEAM = "";'
        self.assertEqual(['MARKETING_VERSION = "1.0-beta";', 'CURRENT_PROJECT_VERSION = "7";', 'DEVELOPMENT_TEAM = "";'],
                         app_config.stray_versions(quoted))

    def test_stray_versions_allows_references_to_the_xcconfig(self):
        for line in ['PRODUCT_BUNDLE_IDENTIFIER = "$(PHONE_BUNDLE_ID)";', "PRODUCT_BUNDLE_IDENTIFIER = $(PHONE_BUNDLE_ID);",
                     'PRODUCT_BUNDLE_IDENTIFIER = "$(PHONE_BUNDLE_ID).tests";', "MARKETING_VERSION = $(MARKETING_VERSION);"]:
            with self.subTest(line=line):
                self.assertEqual([], app_config.stray_versions(line))


class MainTest(unittest.TestCase):
    """Runs the command line against a scratch copy of app.json and the generated files."""

    def setUp(self):
        scratch = tempfile.TemporaryDirectory()
        self.addCleanup(scratch.cleanup)
        self.root = Path(scratch.name)
        self.config = app_config.load()
        self.config_path = self.root / "app.json"
        self.xcconfig_path = self.root / "App.xcconfig"
        self.project_path = self.root / "project.pbxproj"
        self.config_path.write_text(json.dumps(self.config))
        self.project_path.write_text('PRODUCT_BUNDLE_IDENTIFIER = "$(PHONE_BUNDLE_ID)";')
        for name, path in [("ROOT", self.root), ("CONFIG", self.config_path),
                           ("XCCONFIG", self.xcconfig_path), ("PROJECT", self.project_path)]:
            patcher = mock.patch.object(app_config, name, path)
            patcher.start()
            self.addCleanup(patcher.stop)

    def run_main(self, *args):
        out, err = io.StringIO(), io.StringIO()
        with contextlib.redirect_stdout(out), contextlib.redirect_stderr(err):
            code = app_config.main(["app_config.py", *args])
        return code, out.getvalue(), err.getvalue()

    def test_apply_writes_the_xcconfig_and_check_then_passes(self):
        self.assertEqual((0, "wrote App.xcconfig\n", ""), self.run_main("apply"))
        self.assertEqual(app_config.xcconfig(self.config), self.xcconfig_path.read_text())
        self.assertEqual((0, "", ""), self.run_main("check"))

    def test_check_fails_when_the_xcconfig_is_missing_or_stale(self):
        stale = "app.json: code/apple/Config/App.xcconfig is stale: run `python3 tool/app_config.py apply`\n"
        self.assertEqual((1, "", stale), self.run_main("check"))
        self.xcconfig_path.write_text("DEVELOPMENT_TEAM = OLD\n")
        self.assertEqual((1, "", stale), self.run_main("check"))
        self.assertEqual("DEVELOPMENT_TEAM = OLD\n", self.xcconfig_path.read_text())

    def test_hard_coded_project_values_fail_both_commands(self):
        self.project_path.write_text("MARKETING_VERSION = 1.0;")
        expected = (1, "app.json: Xcode project hard-codes MARKETING_VERSION = 1.0; (set it in app.json)\n")
        code, _, err = self.run_main("apply")
        self.assertEqual(expected, (code, err))
        code, _, err = self.run_main("check")
        self.assertEqual(expected, (code, err))

    def test_an_invalid_config_is_reported_and_never_applied(self):
        self.config["apple"]["team"] = "nope"
        self.config_path.write_text(json.dumps(self.config))
        code, out, err = self.run_main("apply")
        self.assertEqual((1, ""), (code, out))
        self.assertEqual("app.json: apple.team 'nope' must be 10 uppercase letters or digits (the Apple team ID)\n", err)
        self.assertFalse(self.xcconfig_path.exists())

    def test_a_missing_key_is_reported_without_a_traceback(self):
        self.config_path.write_text(json.dumps(without(self.config, "version.build")))
        self.assertEqual((1, "", "app.json: version.build is missing\n"), self.run_main("check"))

    def test_a_missing_config_file_is_reported(self):
        self.config_path.unlink()
        self.assertEqual((1, "", f"app.json: {self.config_path} not found\n"), self.run_main("check"))

    def test_unparsable_json_is_reported(self):
        for text in ["{", "[]"]:
            with self.subTest(text=text):
                self.config_path.write_text(text)
                code, out, err = self.run_main("check")
                self.assertEqual((1, ""), (code, out))
                self.assertTrue(err.startswith(f"app.json: {self.config_path} "), err)
                self.assertEqual(1, err.count("\n"))

    def test_unknown_or_missing_commands_print_usage(self):
        for args in [(), ("chekc",), ("check", "extra")]:
            with self.subTest(args=args):
                self.assertEqual((2, "", app_config.USAGE + "\n"), self.run_main(*args))
        self.assertFalse(self.xcconfig_path.exists())


if __name__ == "__main__":
    unittest.main()
