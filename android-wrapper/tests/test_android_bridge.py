import stat
import tempfile
import unittest
from pathlib import Path

import android_bridge


class AndroidBridgeTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.app_files = Path(self.temp.name)
        android_bridge.initialize(str(self.app_files))

    def tearDown(self):
        try:
            android_bridge.clear_api_key()
        finally:
            self.temp.cleanup()

    def test_status_works_without_network_or_api_key(self):
        self.assertFalse(android_bridge.has_api_key())
        output = android_bridge.send_message("/status")
        self.assertIn("API", output)
        self.assertIn("0", output)

    def test_key_is_stored_only_under_private_app_data_and_can_be_removed(self):
        self.assertEqual(android_bridge.save_api_key("local-test-key"), "saved")
        self.assertTrue(android_bridge.has_api_key())
        key_file = self.app_files / "aira-agent" / "data" / "api_key"
        self.assertEqual(key_file.read_text(encoding="utf-8"), "local-test-key\n")
        self.assertEqual(stat.S_IMODE(key_file.stat().st_mode), 0o600)
        self.assertEqual(android_bridge.clear_api_key(), "removed")
        self.assertFalse(android_bridge.has_api_key())

    def test_multiline_write_command_is_blocked_without_touching_workspace(self):
        message = android_bridge.send_message("/write sample.txt")
        self.assertIn("বন্ধ", message)
        self.assertFalse((self.app_files / "aira-agent" / "workspace" / "sample.txt").exists())


if __name__ == "__main__":
    unittest.main()
