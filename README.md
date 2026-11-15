# midis2jam2

[![Open in GitHub Codespaces](https://github.com/codespaces/badge.svg)](https://codespaces.new/tvoginc/midis2jam2-online)

Turn a MIDI file into a 3D stage performance. midis2jam2 animates a virtual band as it
plays your music, with detailed instruments, configurable cameras, lyrics, and video
recording.

## What makes it special

- **A full 3D band:** instruments move and play in time with the notes in your MIDI.
- **Your music, your way:** open MIDI files, connect supported MIDI devices, and customize
  the performance and stage.
- **Make it cinematic:** choose camera angles or let automatic cameras follow the music.
- **Keep the performance:** record a performance to a video file from the desktop app.
- **More than a piano roll:** lyrics, instrument-specific animation, and visual details make
  each arrangement feel like a show.

## Get the app

See the [latest releases](https://github.com/wysko/midis2jam2/releases) for available
desktop and Android downloads. For feature details and usage instructions, visit the
[documentation](https://docs.midis2jam2.xyz/).

## Run locally

You need **JDK 21**, Android SDK platform 36, build tools 36.0.0, NDK 27.0.12077973,
and CMake 3.22.1. Install the Android components with Android Studio's SDK Manager and
make sure `ANDROID_HOME` or `ANDROID_SDK_ROOT` points to the SDK.

Run the desktop app on your computer:

```sh
./gradlew :app:run
```

On Windows, use `.\gradlew.bat :app:run`. The Gradle wrapper downloads the project's
Gradle version automatically. To open a MIDI file, use the app's file picker.

### Browser player on Linux

The browser player needs Xvfb, Fluxbox, x11vnc, noVNC, websockify, PulseAudio, FFmpeg,
Python 3, and a working Android SDK. These are preinstalled in the dev container. On a
Linux host, install the same packages with your distribution's package manager. On
Ubuntu/Debian, for example:

```sh
sudo apt-get install curl ffmpeg fluxbox libasound2 libgl1 libgl1-mesa-dri libopenal1 \
  libxcursor1 libxinerama1 libxrandr2 libxss1 mesa-utils novnc pulseaudio \
  pulseaudio-utils python3 python3-venv unzip websockify x11-utils x11vnc xvfb
```

Create a Python environment, install the small web-server dependency, then launch:

```sh
python3 -m venv .venv
.venv/bin/pip install -r web/requirements.txt
./scripts/run-online.sh
```

Open <http://localhost:5000>, choose a `.mid` or `.midi` file, and select **Upload & Play**.
The app renders in a private virtual display; the browser receives its video and audio
streams. The local listener binds to loopback; Codespaces forwards private ports, and the
display websocket uses a per-run token. Stop the launcher with **Ctrl+C**.

On macOS or Windows, use **Reopen in Container** in VS Code with Docker installed, then
follow the Codespaces instructions below to use the browser player. You can also run the
desktop app directly on either platform.

## Use GitHub Codespaces

Create a Codespace from the badge above or the repository's **Code → Codespaces** menu.
The dev container installs JDK 21, the Android SDK, and the private browser-player
dependencies during setup. When ready, run:

```sh
./scripts/run-online.sh
```

Codespaces opens the private port 5000 player in your browser. Upload a MIDI file there to
start the performance. Port 6080 carries the private display stream and is not public.

## Run tests

```sh
./gradlew :app:desktopTest :asset-tools:test
```

Run `./gradlew check` for the full check, including Android lint.

## Website

The [project showcase](site/index.html) is separate from the live player. Preview it locally:

```sh
python3 -m http.server 8000 --directory site
```

Then open <http://localhost:8000>. The GitHub Pages workflow deploys `site/` when
the repository's `main` or `master` branch is updated. Enable **Settings → Pages →
Build and deployment → GitHub Actions** in the repository to publish it.

## Contributing

Read [CONDUCT.md](CONDUCT.md) before participating. Project behavior and test coverage are
described in [docs/SPEC.md](docs/SPEC.md).

## License

midis2jam2 is free software under the terms of the
[GNU General Public License, version 3 or later](LICENSE).
