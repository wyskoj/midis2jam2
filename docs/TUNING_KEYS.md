# Tuning keys and the capo

The fretting engine picks each part's tuning and capo. The guitar, bass and banjo show them on the instrument
itself (`instrument/family/guitar/TuningVisuals.kt`, timed by `TuningMotion.kt`):
- **Keys.** Each tuning key is turned by how far its string is tuned from standard.
- **Slack.** A lowered string vibrates wider and slower; a raised one is tighter. This needs no art.
- **Retune.** Just before the instrument's first note, the retuned strings' keys turn one at a time from the lowest,
  as a player tunes. The strings themselves stay still.
- **Capo.** A capo slides down from the nut and clamps behind its fret.

## Which instruments have key art

| Instrument | Body (no keys) | Key model | Layout |
|---|---|---|---|
| Electric guitars (all types) | `Guitar.obj` | `GuitarKey.obj` | `instrument/tuning/Guitar.json` |
| Acoustic guitar | `GuitarAcoustic.obj` | `GuitarKey.obj` | `instrument/tuning/GuitarAcoustic.json` |
| Bass (standard, synth) | `Bass.obj` | `BassKey.obj` | `instrument/tuning/Bass.json` |
| Banjo | `Banjo.obj` | `BassKey.obj` at 75% | `instrument/tuning/Banjo.json` |
| Fretless bass | `BassFretless.obj` (still has keys) | — | none yet |

An instrument without a layout keeps the old rule instead: it shows its separate drop-D model (`BassFretlessD.obj`)
when its lowest string is lowered. The other drop-D models are gone.

## Models

All go in `sharedAssets/Assets/`, as OBJ (Blender's default export: Y up), sharing the body's texture and UVs.
- **Body.** Exported without its keys, in exactly the same space as before, so the strings, frets and hand still line
  up. Leave the tuner posts or bushings on the body; only the part that turns moves.
- **Key.** One key, exported on its own with its pivot (the centre of the post) at the origin, so its post runs along
  the export's +Y (Blender's +Z). The key turns about that axis. Every key on the headstock is a copy of it.

## Layout

`sharedAssets/instrument/tuning/<Body>.json`, named after the body it belongs to (`Guitar.json` for `Guitar.obj`).
Having this file is what switches the instrument over. Positions and rotations are **Blender's own numbers**, as
the N panel shows them (Z up, XYZ Euler, degrees); the code converts them to the engine's Y-up space exactly as the
OBJ exporter converts the meshes.

```json
{
  "body": "Bass.obj",
  "key": "BassKey.obj",
  "keyRotation": [-90, 0, 0],
  "keys": [
    {"position": [-1.129, 1.5923, 21.839], "rotation": [47.848, -74.919, 41.131], "direction": 1},
    ...
  ]
}
```

| Key | What it does |
|---|---|
| `body` | The body without its keys, used whatever the tuning. |
| `key` | The key model. |
| `keyRotation` | How the key model sits within each key object, for a model exported turned. `BassKey.obj` was exported with its post along Blender Z, while in the scene the bass key's post runs along its object's local Y, so it is `[-90, 0, 0]`. |
| `keyScale` | How large each key is drawn (the banjo reuses `BassKey.obj` at `0.75`). |
| `keyTexture` | The texture the key model and capo are UV-mapped to, when it isn't the body's. The acoustic's keys use `GuitarSkin.bmp`, and the banjo's (`BassKey.obj`) `BassSkin.bmp`. |
| `degreesPerSemitone` | How far a key turns per semitone from standard (default 20, so drop D's low key turns 40°; much more and the key turns edge-on to the camera and looks small). |
| `keys` | One entry per string, lowest string first. |
| `position`, `rotation` | That key object's Location and Rotation in Blender. |
| `direction` | `1` or `-1`: which way the key turns to tighten its string. The mirrored side of a two-sided headstock is `-1`, so both sides turn as mirror images. |

### Keys made with an Array modifier

With **Endpoint** offset, the Array's translation is the distance from the first copy to the **last**, so each key
is one `count − 1`th of it further along, in the key object's local axes. The layouts here were built that way:
key `i` sits at `location + rotation · (translation × i / (count − 1))`.

A two-sided headstock (acoustic, banjo) has the other side mirrored across X. A mirrored key has its X position
negated and its Y and Z rotations negated, and runs in the opposite string order: on a 3+3 headstock, the key
mirroring the low E's is the high E's.

## Capo

`GuitarCapo.obj` is modelled in place across the guitar's neck (its X and Z are the guitar's), centred on Y = 0.
The code only slides it along the neck (the model's Y) onto its fret (covering where the open strings start to vibrate), lifted off the strings (Z) while it
slides down from the nut, then dropped on to clamp. The acoustic shares the guitar's neck; the banjo uses the same
model, so check it there if a banjo part ever infers a capo.

## Tuning by eye

These constants are in `TuningMotion.kt`:
- `RETUNE_LEAD`, `RETUNE_GAP`: when the retune happens, relative to the first note. For a part that plays from
  the start, the retune is squeezed into the intro (`START_MARGIN` after playback begins).
- `SHOW_BEFORE`: how long a retuned or capo'd instrument is on stage before its retune starts. Instruments normally
  appear only a second before they play, which would hide the retune.
- `SPEED_EXPONENT`, `WIDTH_EXPONENT`: how strongly slack shows.
- `OVERSHOOT`: how far the glide passes its tuning before settling.

## Tests

`TuningMotionTest` and `TuningVisualsTest` check:
- the timing and the slack, and that the strings stay still while tuning;
- that the strings are tuned one at a time, lowest first;
- that the drop-D low key turns 40° about its post and the others stay put;
- that every layout has a key per string;
- the capo's place;
- that Blender poses convert to the same matrices as an independent calculation. That calculation was used to check
  the layouts here against where the keys sat in the old models: every key lands within about 0.2 units.
