import io
import os
import re
import unittest
from unittest.mock import Mock, patch

import app


class PlayerRoutesTest(unittest.TestCase):
    def setUp(self):
        app.stop_performance()
        self.client = app.app.test_client()
        with patch.dict(os.environ, {"MIDIS2JAM2_VNC_TOKEN": "test-vnc-token"}):
            page = self.client.get("/")
        self.assertEqual(page.status_code, 200)
        self.assertIn(b'data-vnc-token="test-vnc-token"', page.data)
        match = re.search(rb'data-csrf-token="([^"]+)"', page.data)
        self.assertIsNotNone(match)
        self.headers = {"X-CSRF-Token": match.group(1).decode()}

    def tearDown(self):
        app.stop_performance()

    def test_player_requires_vnc_token_and_audio_requires_session(self):
        with patch.dict(os.environ, {"MIDIS2JAM2_VNC_TOKEN": ""}):
            self.assertEqual(self.client.get("/").status_code, 503)
        anonymous_client = app.app.test_client()
        self.assertEqual(anonymous_client.get("/audio.mp3").status_code, 403)

    def test_upload_requires_csrf_and_midi_extension(self):
        self.assertEqual(self.client.post("/upload").status_code, 403)
        response = self.client.post(
            "/upload",
            headers=self.headers,
            data={"midi": (io.BytesIO(b"not midi"), "setlist.txt")},
        )
        self.assertEqual(response.status_code, 400)
        self.assertIn("Choose a .mid or .midi file.", response.json["error"])

    def test_midi_upload_starts_and_can_stop_performance(self):
        process = Mock(pid=999999, returncode=0)
        process.poll.return_value = 0

        with patch.object(app.subprocess, "Popen", return_value=process) as launch:
            response = self.client.post(
                "/upload",
                headers=self.headers,
                data={"midi": (io.BytesIO(b"MThd"), "../../setlist.mid")},
            )

        self.assertEqual(response.status_code, 202)
        self.assertEqual(response.json["filename"], "setlist.mid")
        command = launch.call_args.args[0]
        self.assertEqual(command[1:3], [":app:run", "--no-daemon"])
        self.assertTrue(launch.call_args.kwargs["start_new_session"])
        self.assertEqual(self.client.get("/api/status").json["filename"], "setlist.mid")

        stopped = self.client.post("/stop", headers=self.headers)
        self.assertEqual(stopped.status_code, 200)
        self.assertIsNone(self.client.get("/api/status").json["filename"])


if __name__ == "__main__":
    unittest.main()
