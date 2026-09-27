# Tools

Developer tools for working on midis2jam2. They aren't part of the build.

## Voice catalogue editor

`voice_editor/` lets you browse and edit the voice catalogues in `sharedAssets/voices`, which map each
specification's bank and program numbers to the look an instrument is drawn with.

```sh
pip install -r tools/requirements.txt
python tools/voice_editor/app.py
```

- Pick a catalogue on the left. Each voice shows what it appears as on stage: its own look, or, in grey
  with `↳`, the General MIDI instrument it falls back to. `None` means nothing is drawn.
- Double-click a cell to edit it. The look column offers every look defined in `Looks.kt` or `KitLooks.kt`.
- Anything the app's integrity test would reject shows in red, with the reason in the name's tooltip.
- **Ctrl+S** saves the current catalogue, and **Ctrl+Shift+S** saves them all. Comments in the files are kept, and
  an unchanged file saves back byte for byte.

To check the catalogues without opening the editor (no PySide6 needed):

```sh
python tools/voice_editor/catalogue.py --check
```

The editor reads the list of catalogue files and the look names straight from the Kotlin sources, so it
stays in step with the app. After editing, run `./gradlew :app:desktopTest` as usual.
