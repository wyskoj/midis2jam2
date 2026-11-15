#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

if [[ "$(uname -s)" != "Linux" ]]; then
    echo "The browser player needs Linux. On macOS or Windows, open this repository in its dev container." >&2
    exit 1
fi

for command in java python3 Xvfb fluxbox x11vnc websockify pulseaudio pactl ffmpeg xdpyinfo; do
    if ! command -v "$command" >/dev/null 2>&1; then
        echo "Missing required command: $command. Use the repository dev container or install its documented packages." >&2
        exit 1
    fi
done

if [[ ! -x "$ROOT_DIR/.venv/bin/python" ]]; then
    echo "Python dependencies are missing. Run: python3 -m venv .venv && .venv/bin/pip install -r web/requirements.txt" >&2
    exit 1
fi

if [[ ! -x "$ROOT_DIR/gradlew" ]]; then
    echo "Gradle wrapper not found: $ROOT_DIR/gradlew" >&2
    exit 1
fi

export DISPLAY="${MIDIS2JAM2_X_DISPLAY:-:99}"
export LIBGL_ALWAYS_SOFTWARE="${LIBGL_ALWAYS_SOFTWARE:-1}"
export MIDIS2JAM2_PULSE_SINK="midis2jam2"
if [[ "${CODESPACES:-false}" == "true" ]]; then
    export MIDIS2JAM2_WEB_HOST=0.0.0.0
else
    export MIDIS2JAM2_WEB_HOST=127.0.0.1
fi

runtime_base="${XDG_RUNTIME_DIR:-${TMPDIR:-/tmp}}"
pulse_runtime="$runtime_base/midis2jam2-pulse-$$"
mkdir -p "$pulse_runtime"
chmod 700 "$pulse_runtime"
pulse_config="$pulse_runtime/default.pa"
token_file="$pulse_runtime/websockify.tokens"
vnc_token=$(.venv/bin/python -c 'import secrets; print(secrets.token_urlsafe(32))')
cat >"$pulse_config" <<EOF
load-module module-native-protocol-unix socket=$pulse_runtime/native auth-anonymous=1
load-module module-null-sink sink_name=$MIDIS2JAM2_PULSE_SINK sink_properties=device.description=midis2jam2
EOF
printf '%s: 127.0.0.1:5900\n' "$vnc_token" >"$token_file"
chmod 600 "$token_file"
export MIDIS2JAM2_VNC_TOKEN="$vnc_token"
export PULSE_RUNTIME_PATH="$pulse_runtime"
export PULSE_SERVER="unix:$PULSE_RUNTIME_PATH/native"

pids=()
cleanup() {
    for ((index=${#pids[@]} - 1; index >= 0; index--)); do
        kill "${pids[index]}" 2>/dev/null || true
    done
    for ((index=${#pids[@]} - 1; index >= 0; index--)); do
        wait "${pids[index]}" 2>/dev/null || true
    done
    rm -f "$pulse_config" "$token_file"
    rmdir "$pulse_runtime" 2>/dev/null || true
}
trap cleanup EXIT INT TERM

Xvfb "$DISPLAY" -screen 0 1280x900x24 -nolisten tcp -ac +extension GLX +render -noreset &
pids+=("$!")

display_ready=false
for _ in {1..40}; do
    if xdpyinfo -display "$DISPLAY" >/dev/null 2>&1; then
        display_ready=true
        break
    fi
    sleep 0.25
done
if [[ "$display_ready" != true ]]; then
    echo "The virtual display did not start on $DISPLAY." >&2
    exit 1
fi

fluxbox -display "$DISPLAY" &
pids+=("$!")

pulseaudio --daemonize=no --exit-idle-time=-1 --file="$pulse_config" &
pids+=("$!")

pulse_ready=false
for _ in {1..40}; do
    if pactl info >/dev/null 2>&1; then
        pulse_ready=true
        break
    fi
    sleep 0.25
done
if [[ "$pulse_ready" != true ]]; then
    echo "The private PulseAudio server did not start." >&2
    exit 1
fi
pactl set-default-sink "$MIDIS2JAM2_PULSE_SINK"

x11vnc -display "$DISPLAY" -localhost -nopw -forever -shared -rfbport 5900 &
pids+=("$!")

if [[ -f /usr/share/novnc/vnc.html ]]; then
    novnc_web_root=/usr/share/novnc
elif [[ -f /usr/share/novnc/utils/vnc.html ]]; then
    novnc_web_root=/usr/share/novnc/utils
else
    echo "noVNC web files were not found under /usr/share/novnc." >&2
    exit 1
fi

if [[ "${CODESPACES:-false}" == "true" ]]; then
    web_bind=0.0.0.0
else
    web_bind=127.0.0.1
fi
websockify \
    --token-plugin=TokenFile \
    --token-source="$token_file" \
    --web="$novnc_web_root" \
    --listen="$web_bind" \
    6080 &
pids+=("$!")

echo "midis2jam2 browser player: http://localhost:5000"
.venv/bin/python web/app.py &
app_pid=$!
pids+=("$app_pid")
wait "$app_pid"
