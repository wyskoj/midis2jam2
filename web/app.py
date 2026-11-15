#!/usr/bin/env python3
import atexit
import hmac
import os
import secrets
import shlex
import signal
import subprocess
import sys
import tempfile
import threading
from pathlib import Path

from flask import Flask, Response, abort, jsonify, render_template, request, session, stream_with_context
from werkzeug.exceptions import RequestEntityTooLarge
from werkzeug.utils import secure_filename

ROOT = Path(__file__).resolve().parents[1]
UPLOADS = Path(tempfile.mkdtemp(prefix="midis2jam2-online-"))
MAX_UPLOAD_BYTES = 64 * 1024 * 1024
ALLOWED_EXTENSIONS = {".mid", ".midi"}
PULSE_SINK = os.environ.get("MIDIS2JAM2_PULSE_SINK", "midis2jam2")

app = Flask(__name__, template_folder="templates", static_folder="static")
app.config.update(
    SECRET_KEY=os.environ.get("MIDIS2JAM2_SESSION_KEY") or secrets.token_hex(32),
    MAX_CONTENT_LENGTH=MAX_UPLOAD_BYTES,
    SESSION_COOKIE_HTTPONLY=True,
    SESSION_COOKIE_SAMESITE="Strict",
)

process_lock = threading.Lock()
current_process: subprocess.Popen[bytes] | None = None
current_midi: Path | None = None
current_filename: str | None = None


def _stop_performance_locked() -> None:
    global current_process, current_midi, current_filename
    process = current_process
    midi_file = current_midi
    current_process = None
    current_midi = None
    current_filename = None

    if process is not None and process.poll() is None:
        try:
            os.killpg(process.pid, signal.SIGTERM)
        except ProcessLookupError:
            pass
        try:
            process.wait(timeout=10)
        except subprocess.TimeoutExpired:
            try:
                os.killpg(process.pid, signal.SIGKILL)
            except ProcessLookupError:
                pass
            process.wait()

    if midi_file is not None:
        midi_file.unlink(missing_ok=True)


def stop_performance() -> None:
    with process_lock:
        _stop_performance_locked()


def cleanup_uploads() -> None:
    for path in UPLOADS.iterdir():
        path.unlink(missing_ok=True)
    UPLOADS.rmdir()


atexit.register(cleanup_uploads)
atexit.register(stop_performance)


@app.get("/")
def player():
    vnc_token = os.environ.get("MIDIS2JAM2_VNC_TOKEN")
    if not vnc_token:
        abort(503, "Start the browser player with scripts/run-online.sh.")
    csrf_token = session.setdefault("csrf_token", secrets.token_urlsafe(32))
    return render_template("player.html", csrf_token=csrf_token, vnc_token=vnc_token)


@app.get("/api/status")
def status():
    with process_lock:
        running = current_process is not None and current_process.poll() is None
        exit_code = current_process.returncode if current_process is not None and not running else None
        return jsonify(running=running, filename=current_filename, exitCode=exit_code)


def require_csrf_token() -> None:
    token = session.get("csrf_token", "")
    supplied = request.headers.get("X-CSRF-Token", "")
    if not token or not hmac.compare_digest(token, supplied):
        abort(403)


@app.post("/upload")
def upload():
    global current_process, current_midi, current_filename
    require_csrf_token()

    midi = request.files.get("midi")
    if midi is None or not midi.filename:
        return jsonify(error="Choose a MIDI file to play."), 400

    filename = secure_filename(midi.filename)
    extension = Path(filename).suffix.lower()
    if not filename or extension not in ALLOWED_EXTENSIONS:
        return jsonify(error="Choose a .mid or .midi file."), 400

    with tempfile.NamedTemporaryFile(dir=UPLOADS, suffix=extension, delete=False) as upload_file:
        midi.save(upload_file)
        midi_path = Path(upload_file.name)

    environment = os.environ.copy()
    environment["DISPLAY"] = os.environ.get("DISPLAY", ":99")
    environment["LIBGL_ALWAYS_SOFTWARE"] = os.environ.get("LIBGL_ALWAYS_SOFTWARE", "1")
    gradle_args = shlex.quote(str(midi_path))

    with process_lock:
        _stop_performance_locked()
        try:
            process = subprocess.Popen(
                [
                    str(ROOT / "gradlew"),
                    ":app:run",
                    "--no-daemon",
                    f"--args={gradle_args}",
                ],
                cwd=ROOT,
                env=environment,
                start_new_session=True,
            )
        except OSError as error:
            midi_path.unlink(missing_ok=True)
            return jsonify(error=f"Could not start midis2jam2: {error}"), 503

        current_process = process
        current_midi = midi_path
        current_filename = filename

    return jsonify(message=f"Starting {filename}.", filename=filename), 202


@app.post("/stop")
def stop():
    require_csrf_token()
    stop_performance()
    return jsonify(message="Performance stopped.")


@app.errorhandler(RequestEntityTooLarge)
def upload_too_large(_error):
    return jsonify(error="MIDI file is too large (64 MB maximum)."), 413


@app.get("/audio.mp3")
def audio():
    if not session.get("csrf_token"):
        abort(403)

    command = [
        "ffmpeg",
        "-hide_banner",
        "-loglevel",
        "warning",
        "-nostdin",
        "-f",
        "pulse",
        "-i",
        f"{PULSE_SINK}.monitor",
        "-ac",
        "2",
        "-b:a",
        "128k",
        "-f",
        "mp3",
        "pipe:1",
    ]
    process = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=sys.stderr)

    def stream():
        try:
            assert process.stdout is not None
            for chunk in iter(lambda: process.stdout.read(4096), b""):
                yield chunk
            return_code = process.wait()
            if return_code != 0:
                app.logger.error("Audio stream ended with ffmpeg exit code %s", return_code)
        finally:
            if process.poll() is None:
                process.terminate()
                try:
                    process.wait(timeout=5)
                except subprocess.TimeoutExpired:
                    process.kill()
                    process.wait()

    return Response(
        stream_with_context(stream()),
        mimetype="audio/mpeg",
        headers={"Cache-Control": "no-store"},
    )


def shutdown(signum, _frame):
    stop_performance()
    raise SystemExit(128 + signum)


if __name__ == "__main__":
    signal.signal(signal.SIGTERM, shutdown)
    signal.signal(signal.SIGINT, shutdown)
    host = os.environ.get("MIDIS2JAM2_WEB_HOST", "127.0.0.1")
    app.run(host=host, port=5000, threaded=True)
